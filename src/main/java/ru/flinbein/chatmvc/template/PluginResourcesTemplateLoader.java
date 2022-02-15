package ru.flinbein.chatmvc.template;

import freemarker.cache.TemplateLoader;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.URL;

public record PluginResourcesTemplateLoader(JavaPlugin plugin) implements TemplateLoader {

    /**
     * @param templateName template name
     * @return null if template not found. Return non-null if template exists
     */
    @Override
    public URL findTemplateSource(String templateName) {
        return plugin.getClass().getClassLoader().getResource(templateName);
    }

    @Override
    public long getLastModified(Object o) {
        return 0;
    }

    @Override
    public Reader getReader(Object o, String charset) throws IOException {
        if (!(o instanceof URL url)) return null;
        return new InputStreamReader(url.openStream(), charset);
    }

    @Override
    public void closeTemplateSource(Object o) {} // not needed; Reader already closed
}
