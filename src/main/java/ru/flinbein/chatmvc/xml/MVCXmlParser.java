package ru.flinbein.chatmvc.xml;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

public class MVCXmlParser {

    static DocumentBuilder documentBuilder;

    static {
        try {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Throwable ignored) {}
    }

    public BaseComponent parse(InputStream xml) throws Exception {
        // Создается построитель документа

        // Создается дерево DOM документа из файла
        Document document = documentBuilder.parse(xml);
        Element rootComponent = document.getDocumentElement();
        return parseBaseComponent(rootComponent);
    }

    private BaseComponent parseBaseComponent(Element element){
        return parseBaseComponent(element, element.getTagName());
    }

    private BaseComponent parseBaseComponent(Element element, String type) {
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
        for (var i=0; i<childNodes.getLength(); i++){
            Node child = childNodes.item(i);
            if (child instanceof Text text){
                component.addExtra(text.getNodeValue().trim());
            } else if (child instanceof Element e){
                switch (e.getTagName()) {
                    case "hover-entity", "hover-item", "hover-text" -> {
                        if (hoverElement != null) throw new RuntimeException("Only one hover item allowed");
                        hoverElement = e;
                    }
                    case "br" -> component.addExtra("\n");
                    case "pre" -> component.addExtra(e.getNodeValue());
                    case "space" -> component.addExtra(" ");
                    default -> component.addExtra(parseBaseComponent(e));
                }
            } else {
                throw new RuntimeException("ToDo: only text & component-tags allowed in tag <"+type+">");
            }
        }
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
                default -> throw new RuntimeException("Wrong onclick type: "+commandType);
            };
            component.setClickEvent(new ClickEvent(action, commandValue));
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
                if (withElement != null) throw new RuntimeException("ToDo: Only 1 <With> allowed");
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
                } else {
                    throw new RuntimeException("ToDo: only text & component-tags allowed in tag <With>");
                }
            }
        }
        return cmp;
    }
}
