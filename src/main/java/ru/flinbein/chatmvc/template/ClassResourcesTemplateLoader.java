package ru.flinbein.chatmvc.template;

import java.io.*;
import java.net.URL;

public class ClassResourcesTemplateLoader extends BukkitResourcesTemplateLoader {

    private final ClassLoader classLoader;

    public ClassResourcesTemplateLoader(ClassLoader classLoader){
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * @param templateName template name
     * @return null if template not found. Return non-null if template exists
     */
    @Override
    public URL findTemplateSource(String templateName) {
        URL source = super.findTemplateSource(templateName);
        if (source != null) return source;
        return getClassLoader().getResource(templateName);
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
