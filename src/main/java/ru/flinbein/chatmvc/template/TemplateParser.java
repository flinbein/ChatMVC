package ru.flinbein.chatmvc.template;

import freemarker.cache.TemplateLoader;
import freemarker.core.XMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.plugin.Plugin;
import org.xml.sax.SAXException;
import ru.flinbein.chatmvc.xml.MVCXmlParser;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class TemplateParser {

    private static final MVCXmlParser xmlParser = MVCXmlParser.getInstance();
    private static final Map<ClassLoader, TemplateParser> parserMap = new HashMap<>();

    private final Configuration templateConfig;

    public static TemplateParser getForPlugin(Plugin plugin){
        return getForClassLoader(plugin.getClass().getClassLoader());
    }

    public static TemplateParser getForClassLoader(ClassLoader classLoader){
        if (parserMap.containsKey(classLoader)) return parserMap.get(classLoader);
        var instance = new TemplateParser(classLoader);
        parserMap.put(classLoader, instance);
        return instance;
    }

    private TemplateParser(ClassLoader classLoader) {
        TemplateLoader loader = new ClassResourcesTemplateLoader(classLoader);
        templateConfig = new Configuration(Configuration.VERSION_2_3_29);
        templateConfig.setTemplateLoader(loader);
        templateConfig.setLocalizedLookup(false);
        templateConfig.setDefaultEncoding("UTF8");
        templateConfig.setOutputFormat(XMLOutputFormat.INSTANCE);
    }

    public TemplateParser(Configuration config){
        templateConfig = config;
    }


    public InputStream parseTemplateToXml(String templateName, Object model) throws IOException {
        Template temp = templateConfig.getTemplate(templateName);
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        new Thread(() -> {
            try (var writer = new OutputStreamWriter(new BufferedOutputStream(outputStream))) {
                temp.process(model, writer);
            } catch (IOException|TemplateException e) {
                throw new RuntimeException(e);
            }
        }).start();
        return pipedInputStream;
    }

    public BaseComponent parseTemplateToComponent(String fileName, Object model){
        try (
            var input = parseTemplateToXml(fileName, model);
        ) {
            return xmlParser.parse(input);
        } catch (IOException|SAXException exception) {
           throw new RuntimeException("ChatMVC template parse error: "+fileName, exception);
        }
    }


}
