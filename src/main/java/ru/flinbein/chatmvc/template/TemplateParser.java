package ru.flinbein.chatmvc.template;

import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.plugin.java.JavaPlugin;
import org.xml.sax.SAXException;
import ru.flinbein.chatmvc.xml.MVCXmlParser;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class TemplateParser {

    private static final MVCXmlParser xmlParser = MVCXmlParser.getInstance();
    private static final Map<JavaPlugin, TemplateParser> parserMap = new HashMap<>();

    private final Configuration cfg = new Configuration(Configuration.VERSION_2_3_29);

    public static TemplateParser getForPlugin(JavaPlugin plugin){
        if (parserMap.containsKey(plugin)) return parserMap.get(plugin);
        var instance = new TemplateParser(plugin);
        parserMap.put(plugin, instance);
        return instance;
    }

    private TemplateParser(JavaPlugin plugin) {
        TemplateLoader loader = new PluginResourcesTemplateLoader(plugin);
        cfg.setTemplateLoader(loader);
    }


    public InputStream parseTemplateToXml(String templateName, Object model) throws IOException {
        Template temp = cfg.getTemplate(templateName);
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
