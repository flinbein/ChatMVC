package ru.flinbein.chatmvc.controller;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.flinbein.chatmvc.template.TemplateParser;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


// only for chat.
public class MVCController {

    public String commandPrefix;
    protected Plugin plugin;
    protected CommandSender commandSender;
    protected TemplateParser parser;
    private boolean registered = false;
    private final HashMap<String, Binding> bindings = new HashMap<>();

    public MVCController() {}

    public void register(CommandSender sender, Plugin plugin, String commandPrefixWithId) {
        if (registered) {
            throw new RuntimeException("Controller already registered: "+commandPrefixWithId);
        }
        registered = true;
        bindings.clear();
        this.plugin = plugin;
        this.commandSender = sender;
        this.commandPrefix = commandPrefixWithId;
        this.parser = TemplateParser.getForPlugin(plugin);
    }

    public BaseComponent parsePattern(String fileName)  {
        try {
            return parser.parseTemplateToComponent(fileName, this);
        } catch (Exception e) {
            // ToDo error?
        }
        return null;
    }

    private int freeIntActionId = 1;
    private String getNewActionId() {
        return Integer.toString(freeIntActionId++, 32);
    }

    // /cmvc frameId:actionId

    // clear bindings after render
    public String bind(String methodName, Object... params) {
        Binding binding = new Binding(methodName, params);
        var actionId = getNewActionId();
        bindings.put(actionId, binding);
        return commandPrefix + ":" + actionId;
    }


    public boolean onCommand(String actionId, String[] texts) {
        var binding = bindings.get(actionId);
        if (binding == null) {
            // ToDo error?
            return false;
        }
        String methodName = binding.methodName;
        try {
            Method method = this.getClass().getMethod(methodName, Object[].class, String[].class);
            Object result = method.invoke(this, binding.params, texts);
            return result == null || !result.equals(false);
        } catch (Exception e) {
            // ToDo error?
            return false;
        }
    }

    public List<String> onTabComplete(String actionId, String[] texts){
        var binding = bindings.get(actionId);
        if (binding == null) {
            // ToDo error?
            return null;
        }
        String methodName = binding.methodName+"_Tab";
        try {
            Method method = this.getClass().getMethod(methodName, Object[].class, String[].class);
            Object result = method.invoke(this, binding.params, texts);
            if (result instanceof List list) return list;
            return null;
        } catch (Exception e) {
            // ToDo error?
            return null;
        }
    }

    public void render(String patternFileName) {
        bindings.clear();
        BaseComponent baseComponent = parsePattern(patternFileName);
        commandSender.spigot().sendMessage(baseComponent);
    }

    record Binding(String methodName, Object[] params) {}
}
