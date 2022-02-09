package ru.flinbein.chatmvc.template;

import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;

public class TemplateParser {

    private Configuration cfg = new Configuration(Configuration.VERSION_2_3_29);

    public TemplateParser(JavaPlugin plugin) {
        TemplateLoader loader = new PluginResourcesTemplateLoader(plugin);
        cfg.setTemplateLoader(loader);
    }


    InputStream parseFile(String fileName, Object model) throws Exception {
        Template temp = cfg.getTemplate(fileName);
        PipedOutputStream outputStream = new PipedOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
        temp.process(model, writer);
        return new PipedInputStream(outputStream);
    }
}
