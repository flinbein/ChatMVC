package ru.flinbein.chatmvc.template;

import freemarker.cache.TemplateLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

public class PluginResourcesTemplateLoader implements TemplateLoader {

    private JavaPlugin plugin;

    public PluginResourcesTemplateLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Object findTemplateSource(String s) {
        return plugin.getClass().getClassLoader().getResourceAsStream(s);
    }

    @Override
    public long getLastModified(Object o) {
        return 0;
    }

    @Override
    public Reader getReader(Object o, String s) throws IOException {
        InputStream stream = (InputStream) o;
        return new InputStreamReader(stream);
    }

    @Override
    public void closeTemplateSource(Object o) throws IOException {
        InputStream stream = (InputStream) o;
        stream.close();
    }
}
