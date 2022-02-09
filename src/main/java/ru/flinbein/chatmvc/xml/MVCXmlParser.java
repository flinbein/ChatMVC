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

    static void parse(InputStream xml) throws Exception {
        // Создается построитель документа

        // Создается дерево DOM документа из файла
        Document document = documentBuilder.parse("BookCatalog.xml");
        Element rootComponent = document.getDocumentElement();
    }

    private BaseComponent parseBaseComponent(Element element) {
        String type = element.getTagName();
        BaseComponent component = switch (type) {
            case "KeybindComponent" -> parseKeybindComponent(element);
            case "TextComponent" -> parseTextComponent(element);
            case "ScoreComponent" -> parseScoreComponent(element);
            case "SelectorComponent" -> parseSelectorComponent(element);
            case "TranslatableComponent" -> parseTranslatableComponent(element);
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

        return component;
    }

    private KeybindComponent parseKeybindComponent(Element element) {
        KeybindComponent cmp = new KeybindComponent();
        String keybind = element.getAttribute("keybind");
        cmp.setKeybind(keybind);
        return cmp;
    }

    private TextComponent parseTextComponent(Element element) {
        TextComponent cmp = new TextComponent();
        if (element.hasAttribute("text")) {
            cmp.setText(element.getAttribute("text"));
        }
        return cmp;
    }

    private ScoreComponent parseScoreComponent(Element element) {
        String name = element.getAttribute("name");
        String objective = element.getAttribute("objective");

        if (element.hasAttribute("value")) {
            return new ScoreComponent(name, objective, element.getAttribute("value"));
        }

        return new ScoreComponent(name, objective);
    }

    private SelectorComponent parseSelectorComponent(Element element) {
        String selector = element.getAttribute("selector");
        return new SelectorComponent(selector);
    }

    private TranslatableComponent parseTranslatableComponent(Element element) {
        String translate = element.getAttribute("translate");
        TranslatableComponent cmp = new TranslatableComponent(translate);
        NodeList childNodes = element.getChildNodes();
        Element withElement = null;
        for (var i = 0; i < childNodes.getLength(); i++) {
            var node = childNodes.item(i);
            if (! (node instanceof Element)) continue;
            Element child = (Element) node;
            if (child.getTagName().equals("With")) {
                if (withElement != null) throw new RuntimeException("ToDo: Only 1 <With> allowed");
                withElement = child;
            };
        }
        if (withElement != null) {
            NodeList withElementChildren = withElement.getChildNodes();
            for (var i = 0; i < withElementChildren.getLength(); i++) {
                var child = withElementChildren.item(i);
                if (child instanceof Text) {
                    var txt = child.getNodeValue().trim();
                    cmp.addWith(txt);
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
