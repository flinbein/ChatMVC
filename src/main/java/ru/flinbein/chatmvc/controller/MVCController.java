package ru.flinbein.chatmvc.controller;

import com.google.common.collect.Maps;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.flinbein.chatmvc.template.TemplateParser;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class MVCController implements BindableController {

    private final String id;
    private final JavaPlugin plugin;
    private final TemplateParser parser;
    private final BindMethod bindMethod = new BindMethod();
    public Boolean global = false;
    public final HashMap<String, Object> variables = new HashMap<>();
    private final HashMap<String, Binding> bindings = new HashMap<>();


    public MVCController(String id, JavaPlugin plugin) {
        this.id = id;
        this.plugin = plugin;
        parser = new TemplateParser(plugin);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public InputStream parsePattern(String fileName)  {
        Map<String, Object> model = new HashMap<>(variables);
        model.put("bind", bindMethod);
        try {
            return parser.parseFile(fileName, model);
        } catch (Exception e) {
            // ToDo error?
        }
        return null;
    }

    private int freeIntActionId = 1;
    private String getNewActionId() {
        return Integer.toString(freeIntActionId++, 32);
    }

    private String getControllerCommandPrefix() {
        return (global ? "g:" : "p:") + getId();
    }


    @Override
    public void onCommand(String actionId, String text) {
        var binding = bindings.get(actionId);
        if (binding == null) {
            // ToDo error?
            return;
        }
        String methodName = binding.methodName;
        try {
            Method method = this.getClass().getMethod(methodName, List.class, String.class);
            method.invoke(this, binding.params, text);
        } catch (Exception e) {
            // ToDo error?
            return;
        }
    }

    record Binding(String methodName, List params) {}

    class BindMethod implements TemplateMethodModelEx {

        @Override
        public Object exec(List list) throws TemplateModelException {
            if (list.isEmpty() || !(list.get(0) instanceof String methodName)) {
                throw new RuntimeException("First argument of controller bind function must be methodName");
            }
            var params = list.subList(1, list.size());
            Binding binding = new Binding(methodName, params);
            bindings.put(id, binding);
            return getControllerCommandPrefix() + ":" + getNewActionId();
        }

    }
}
