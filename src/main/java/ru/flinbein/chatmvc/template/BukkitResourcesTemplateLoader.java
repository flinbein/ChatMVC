package ru.flinbein.chatmvc.template;

import freemarker.cache.TemplateLoader;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

public class BukkitResourcesTemplateLoader implements TemplateLoader {

    /**
     * @param templateName template name
     * @return null if template not found. Return non-null if template exists
     */
    @Override
    public URL findTemplateSource(String templateName) {
        if (templateName.startsWith("plugin:")) {
            String pluginPath = templateName.substring(7);
            String[] pathArgs = pluginPath.split(":", 2);
            if (pathArgs.length != 2) return null;
            return findPluginTemplateSource(pathArgs[0], pathArgs[1]);
        }
        try {
            return new URL(templateName);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    protected URL findPluginTemplateSource(String pluginName, String path){
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) return null;
        return plugin.getClass().getClassLoader().getResource(path);
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
