package ru.flinbein.chatmvc.xml;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class MVCXmlParser {

    private static MVCXmlParser self;
    private final DocumentBuilder documentBuilder;

    public static MVCXmlParser getInstance(){
        if (self != null) return self;
        return self = new MVCXmlParser();
    }

    private MVCXmlParser(){
        SchemaFactory schemaFactory = SchemaFactory.newDefaultInstance();
        URL schemaUrl = MVCXmlParser.class.getClassLoader().getResource("chatMVCSchema.xsd");
        if (schemaUrl == null) throw new RuntimeException("can not initialize MVCXmlParser: no schema file");
        try (var schemaStream = schemaUrl.openStream()) {
            var schemaSource = new StreamSource(schemaStream);
            Schema schema = schemaFactory.newSchema(schemaSource);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setSchema(schema);
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException|SAXException|IOException ex) {
            throw new RuntimeException("can not initialize MVCXmlParser", ex);
        }
    }

    public BaseComponent parse(InputStream xml) throws IOException, SAXException {
        // Создается построитель документа

        // Создается дерево DOM документа из файла
        Document document = documentBuilder.parse(xml);
        Element rootComponent = document.getDocumentElement();
        return parseBaseComponent(rootComponent);
    }

    private BaseComponent parseBaseComponent(Element element){
        return parseBaseComponent(element, element.getTagName());
    }

    private BaseComponent parseBaseComponent(Element element, String type){
        return parseBaseComponent(element, type, true, true,null);
    }

    private BaseComponent parseBaseComponent(Element element, String type, boolean trimFirstSpace, boolean trimLastSpace, boolean[] spaceRef) {
        BaseComponent component = switch (type) {
            case "keybind" -> parseKeybind(element);
            case "text" -> parseText(element);
            case "score" -> parseScore(element);
            case "selector" -> parseSelector(element);
            case "translatable" -> parseTranslatable(element);
            default -> throw new RuntimeException("Unknown component - "+type);
        };

        if (element.hasAttribute("bold")) {
            component.setBold(element.getAttribute("bold").equals("true"));
        }
        if (element.hasAttribute("italic")) {
            component.setItalic(element.getAttribute("italic").equals("true"));
        }
        if (element.hasAttribute("underlined")) {
            component.setUnderlined(element.getAttribute("underlined").equals("true"));
        }
        if (element.hasAttribute("strikethrough")) {
            component.setStrikethrough(element.getAttribute("strikethrough").equals("true"));
        }
        if (element.hasAttribute("obfuscated")) {
            component.setObfuscated(element.getAttribute("obfuscated").equals("true"));
        }
        if (element.hasAttribute("color")) {
            var colorStr = element.getAttribute("color");
            component.setColor( ChatColor.of(colorStr) );
        }
        if (element.hasAttribute("font")) {
            component.setFont( element.getAttribute("font") );
        }
        if (element.hasAttribute("insertion")) {
            component.setInsertion( element.getAttribute("insertion") );
        }
        Element hoverElement = null;
        NodeList childNodes = element.getChildNodes();
        if (childNodes.getLength() == 0) trimFirstSpace = false;
        for (var i=0; i<childNodes.getLength(); i++){
            boolean currentTrimFirstSpace = true;
            Node child = childNodes.item(i);
            if (child instanceof Text text){
                String textValue = text.getNodeValue().replaceAll("[\\s\\n\\t]+", " ");
                if (textValue.equals(" ") || textValue.isEmpty()) continue;
                if (trimFirstSpace && textValue.startsWith(" ")) textValue = textValue.substring(1);
                if (trimLastSpace && i == childNodes.getLength()-1 && textValue.endsWith(" ")) textValue = textValue.substring(0, textValue.length()-1);
                component.addExtra(textValue);
                currentTrimFirstSpace = textValue.endsWith(" ");
            } else if (child instanceof Element e){
                switch (e.getTagName()) {
                    case "hover-entity", "hover-item", "hover-text" -> {
                        currentTrimFirstSpace = trimFirstSpace;
                        if (hoverElement != null) throw new RuntimeException("Only one hover item allowed");
                        hoverElement = e;
                    }
                    case "pre" -> {
                        String content = e.getTextContent();
                        component.addExtra(content);
                        currentTrimFirstSpace = content.matches("[\\s\\n\\t]$");
                    }
                    case "br" -> component.addExtra("\n");
                    case "space" -> component.addExtra(" ");
                    default -> {
                        var spaceRefOut = new boolean[]{false};
                        component.addExtra(parseBaseComponent(e, e.getTagName(), trimFirstSpace, false, spaceRefOut));
                        currentTrimFirstSpace = spaceRefOut[0];
                    }
                }
            } else if (child instanceof Comment) {
                continue;
            } else {
                throw new RuntimeException("only text & component-tags allowed in tag <"+type+">");
            }
            trimFirstSpace = currentTrimFirstSpace;
        }
        if (spaceRef != null) spaceRef[0] = trimFirstSpace;
        if (element.hasAttribute("endl") && element.getAttribute("endl").equals("true")) {
            component.addExtra("\n");
        }

        if (element.hasAttribute("onclick") && !element.getAttribute("onclick").isEmpty()) {
            String onclickAttr = element.getAttribute("onclick");
            String[] split = onclickAttr.split(":", 2);
            if (split.length < 1) throw new RuntimeException("Wrong onclick value: "+onclickAttr);
            String commandType = split[0];
            String commandValue = split.length > 1 ? split[1] : "";
            ClickEvent.Action action = switch (commandType) {
                case "copy" -> ClickEvent.Action.COPY_TO_CLIPBOARD;
                case "file" -> ClickEvent.Action.OPEN_FILE;
                case "page" -> ClickEvent.Action.CHANGE_PAGE;
                case "run" -> ClickEvent.Action.RUN_COMMAND;
                case "suggest" -> ClickEvent.Action.SUGGEST_COMMAND;
                case "url" -> ClickEvent.Action.OPEN_URL;
                case "nothing" -> null;
                default -> throw new RuntimeException("Wrong onclick type: "+commandType);
            };
            if (action != null) component.setClickEvent(new ClickEvent(action, commandValue));
        }

        if (hoverElement != null) {
            component.setHoverEvent(parseHoverEvent(hoverElement));
        }

        return component;
    }

    private HoverEvent parseHoverEvent(Element element){
        return switch (element.getTagName()) {
            case "hover-item" -> parseHoverEventItem(element);
            case "hover-entity" -> parseHoverEventEntity(element);
            case "hover-text" -> parseHoverEventText(element);
            default -> throw new RuntimeException("Unknown hover element: "+element.getTagName());
        };
    }

    private HoverEvent parseHoverEventItem(Element element){
        String itemId = element.getAttribute("id");
        int itemCount = element.hasAttribute("count") ? Integer.parseInt(element.getAttribute("count")) : 1;
        ItemTag itemTag = null;
        if (element.hasAttribute("tag")) {
            itemTag = ItemTag.ofNbt(element.getAttribute("tag"));
        }
        var item = new net.md_5.bungee.api.chat.hover.content.Item(itemId, itemCount, itemTag);
        return new HoverEvent(HoverEvent.Action.SHOW_ITEM, item);
    }

    private HoverEvent parseHoverEventEntity(Element element){
        String id = element.getAttribute("id");
        String type = element.getAttribute("type");
        BaseComponent name = null;
        if (element.hasAttribute("text") || element.getChildNodes().getLength() > 0) {
            name = parseBaseComponent(element, "text");
        }
        var entity = new net.md_5.bungee.api.chat.hover.content.Entity(type, id, name);
        return new HoverEvent(HoverEvent.Action.SHOW_ENTITY, entity);
    }

    private HoverEvent parseHoverEventText(Element element){
        net.md_5.bungee.api.chat.hover.content.Text text;
        if (element.hasAttribute("text") && element.getAttributes().getLength() == 1) {
            text = new net.md_5.bungee.api.chat.hover.content.Text(element.getAttribute("text"));
        } else {
            BaseComponent base = parseBaseComponent(element, "text");
            text = new net.md_5.bungee.api.chat.hover.content.Text(new BaseComponent[]{base});
        }
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, text);
    }

    private KeybindComponent parseKeybind(Element element) {
        KeybindComponent cmp = new KeybindComponent();
        String keybind = element.getAttribute("keybind");
        cmp.setKeybind(keybind);
        return cmp;
    }

    private TextComponent parseText(Element element) {
        TextComponent cmp = new TextComponent();
        if (element.hasAttribute("text")) {
            cmp.setText(element.getAttribute("text"));
        }
        return cmp;
    }

    private ScoreComponent parseScore(Element element) {
        String name = element.getAttribute("name");
        String objective = element.getAttribute("objective");

        if (element.hasAttribute("value") && !element.getAttribute("value").isEmpty()) {
            return new ScoreComponent(name, objective, element.getAttribute("value"));
        }

        return new ScoreComponent(name, objective);
    }

    private SelectorComponent parseSelector(Element element) {
        String selector = element.getAttribute("selector");
        return new SelectorComponent(selector);
    }

    private TranslatableComponent parseTranslatable(Element element) {
        String translate = element.getAttribute("translate");
        TranslatableComponent cmp = new TranslatableComponent(translate);
        NodeList childNodes = element.getChildNodes();
        Element withElement = null;
        for (var i = 0; i < childNodes.getLength(); i++) {
            var node = childNodes.item(i);
            if (! (node instanceof Element childComponent)) continue;
            if (childComponent.getTagName().equals("with")) {
                if (withElement != null) throw new RuntimeException("Only 1 <With> allowed");
                withElement = childComponent;
            };
        }
        if (withElement != null) {
            NodeList withElementChildren = withElement.getChildNodes();
            for (var i = 0; i < withElementChildren.getLength(); i++) {
                var child = withElementChildren.item(i);
                if (child instanceof Text text) {
                    cmp.addWith(text.getNodeValue().trim());
                } else if (child instanceof Element) {
                    var childComponent = parseBaseComponent((Element) child);
                    cmp.addWith(childComponent);
                } if (!(child instanceof Comment)) {
                    throw new RuntimeException("only text & component-tags allowed in tag <With>");
                }
            }
        }
        return cmp;
    }
}
