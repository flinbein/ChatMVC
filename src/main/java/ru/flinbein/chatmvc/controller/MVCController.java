package ru.flinbein.chatmvc.controller;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.VisibilityBridgeStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import ru.flinbein.chatmvc.LocalClassLoader;
import ru.flinbein.chatmvc.template.TemplateParser;

import javax.tools.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;


// only for chat.
public class MVCController {

    public String commandPrefix;
    protected Plugin plugin;
    protected CommandSender commandSender;
    protected TemplateParser parser;
    private boolean registered = false;
    private final HashMap<String, Binding> bindings = new HashMap<>();
    private Class<?> ctrlInterface;
    private Object proxyValue;

    public MVCController() {}

    public final void register(CommandSender sender, Plugin plugin, String commandPrefixWithId) {
        if (registered) {
            throw new RuntimeException("Controller already registered: "+commandPrefixWithId);
        }
        registered = true;
        bindings.clear();
        this.plugin = plugin;
        this.commandSender = sender;
        this.commandPrefix = commandPrefixWithId;
        this.parser = TemplateParser.getForPlugin(plugin);
        DynamicType.Builder<?> builder = new ByteBuddy(ClassFileVersion.ofThisVm()).with(TypeValidation.DISABLED).makeInterface().name(this.getClass().getSimpleName()+"__EX");
        Method[] methods = this.getClass().getMethods();
        for (Method method : methods) {
            int modifiers = method.getModifiers();
            if (!Modifier.isPublic(modifiers)) continue;
            if (Modifier.isFinal(modifiers)) continue;
            var returnType = method.isAnnotationPresent(Bind.class) ? String.class : method.getReturnType();
            builder = builder.defineMethod(method.getName(), returnType, Visibility.PUBLIC)
                    .withParameters(method.getParameterTypes())
                    .throwing(method.getExceptionTypes())
                    .withoutCode();
        }
        builder = builder.defineMethod("getMe", int.class, Visibility.PUBLIC).withoutCode();
        ctrlInterface = builder.make().load(this.getClass().getClassLoader()).getLoaded();
        proxyValue = Proxy.newProxyInstance(ctrlInterface.getClassLoader(), new Class[]{ctrlInterface}, (proxy, method, args) -> {
            Method localMethod = this.getClass().getMethod(method.getName(), method.getParameterTypes());
            if (!localMethod.isAnnotationPresent(Bind.class)) return localMethod.invoke(this, args);
            return bind((params) -> {
                try {
                    localMethod.invoke(this, args);
                } catch (IllegalAccessException|InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            });
        });

    }

    public BaseComponent parsePattern(String fileName)  {
        try {
            return parser.parseTemplateToComponent(fileName, proxyValue);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private int freeIntActionId = 1;
    private String getNewActionId() {
        return Integer.toString(freeIntActionId++, 32);
    }


    public final String bind(String methodName, Object... params) {
        Binding binding = new Binding(methodName, params);
        var actionId = getNewActionId();
        bindings.put(actionId, binding);
        return "/" + commandPrefix + ":" + actionId;
    }

    public final String bind(Consumer<String[]> consumer) {
        Binding binding = new Binding(null, new Consumer[]{consumer});
        var actionId = getNewActionId();
        bindings.put(actionId, binding);
        return "/" + commandPrefix + ":" + actionId;
    }

    public final boolean onCommand(String actionId, String[] texts) {
        var binding = bindings.get(actionId);
        if (binding == null) {
            // ToDo error?
            return false;
        }
        String methodName = binding.methodName;
        if (methodName == null) {
            Consumer<String[]> param = (Consumer<String[]>) binding.params[0];
            param.accept(texts);
            return true;
        }
        try {
            Method method = this.getClass().getMethod(methodName, Object[].class, String[].class);
            Object result = method.invoke(this, binding.params, texts);
            return result == null || !result.equals(false);
        } catch (Exception e) {
            // ToDo error?
            return false;
        }
    }

    public final List<String> onTabComplete(String actionId, String[] texts){
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

    public final void render(String patternFileName) {
        bindings.clear();
        BaseComponent baseComponent = parsePattern(patternFileName);
        commandSender.spigot().sendMessage(baseComponent);
    }

    record Binding(String methodName, Object[] params) {}
}
