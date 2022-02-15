package ru.flinbein.chatmvc.controller;

import net.md_5.bungee.api.chat.BaseComponent;
import org.xml.sax.SAXException;
import ru.flinbein.chatmvc.xml.MVCXmlParser;

import java.io.IOException;
import java.io.InputStream;

public interface Controller {

    public String getId();

    private BaseComponent parseXML(InputStream xmlStream) throws IOException, SAXException {
        return MVCXmlParser.parse(xmlStream);
    }

    void render(InputStream xml);
}
