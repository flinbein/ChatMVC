package ru.flinbein.chatmvc.template;

import freemarker.cache.TemplateLoader;
import freemarker.core.XMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.xml.sax.SAXException;
import ru.flinbein.chatmvc.xml.MVCXmlParser;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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


    public InputStream parseTemplateToXml(String templatePath, Object model) throws IOException {
        Template temp = templateConfig.getTemplate(templatePath);
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

    public BaseComponent parseTemplateToComponent(String templatePath, Object model){
        try (var input = parseTemplateToXml(templatePath, model)) {
            return xmlParser.parse(input);
        } catch (IOException|SAXException exception) {
            throw new RuntimeException("ChatMVC template parse error: "+templatePath, exception);
        }
    }

    public static void parseTemplateToComponent(String templatePath, Object model, Plugin plugin, BiConsumer<BaseComponent, Throwable> task){
        TemplateParser parser = TemplateParser.getForPlugin(plugin);
        new Thread(() -> {
            try {
                var component = parser.parseTemplateToComponent(templatePath, model);
                Bukkit.getScheduler().runTask(plugin, () -> task.accept(component, null));
            } catch (Throwable error) {
                task.accept(null, error);
            }
        }).start();
    }

    public static void parseTemplateToComponent(String templatePath, Object model, Plugin plugin, Consumer<BaseComponent> task){
        parseTemplateToComponent(templatePath, model, plugin, ((component, throwable) -> {
            if (component != null) task.accept(component);
        }));
    }


}
