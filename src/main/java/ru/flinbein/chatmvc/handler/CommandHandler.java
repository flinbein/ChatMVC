package ru.flinbein.chatmvc.handler;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import ru.flinbein.chatmvc.controller.Binding;
import ru.flinbein.chatmvc.controller.DummyInvocationHandler;
import ru.flinbein.chatmvc.controller.MVVMController;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class CommandHandler implements TabExecutor {

    private int maxControllersPerSender = 5;
    private final String commandPrefix;
    // Player = [controllerId = MVCController]
    private final HashMap<CommandSender, LinkedHashMap<String, MVVMController>> senderControllerMap = new HashMap<>();

    public CommandHandler(PluginCommand command) {
        command.setExecutor(this);
        command.setTabCompleter(this);
        commandPrefix = command.getName();
    }

    // commandPrefix without slash
    public CommandHandler(String commandPrefix) {
        this.commandPrefix = commandPrefix;
    }

    public int getMaxControllersPerSender() { return maxControllersPerSender; }
    public void setMaxControllersPerSender(int maxControllersPerSender) {
        this.maxControllersPerSender = maxControllersPerSender;
    }

    private int freeIntControllerId = 1;
    private String getNewControllerId() {
        return Integer.toString(freeIntControllerId++, 32);
    }

    public <T extends MVVMController> T registerController(CommandSender sender, T controller, ClassLoader classLoader) {
        var controllerMap = senderControllerMap.getOrDefault(sender, new LinkedHashMap<>());
        senderControllerMap.put(sender, controllerMap);
        if (controllerMap.size() > maxControllersPerSender) {
            String firstKey = controllerMap.keySet().iterator().next();
            controllerMap.remove(firstKey);
            // Controller on destroy?
        }
        var controllerId = getNewControllerId();
        controller.registerHandlerParameters(sender, classLoader, commandPrefix + " " + controllerId, dummyArguments);
        controllerMap.put(controllerId, controller);
        return controller;
    }

    public <T extends MVVMController> T registerController(CommandSender sender, T controller) {
        return registerController(sender, controller, controller.getClass().getClassLoader());
    }

    public <T extends MVVMController> T registerController(CommandSender sender, T controller, Object plugin) {
        return registerController(sender, controller, plugin.getClass().getClassLoader());
    }

    public <T extends MVVMController> T registerController(CommandSender sender, T controller, Class<?> clazz) {
        return registerController(sender, controller, clazz.getClassLoader());
    }

    private MVVMController getControllerForSender(CommandSender commandSender, String arg1) {
        var controllerMap = senderControllerMap.get(commandSender);
        if (controllerMap == null || controllerMap.isEmpty()) return null;
        String[] splited = arg1.split(":");
        if (splited.length != 2) return null;
        String controllerId = splited[0];
        return controllerMap.get(controllerId);
    }

    // strings[0] must be "controllerId:actionId"
    public boolean handleCommand(CommandSender commandSender, String[] strings) {
        if (strings.length == 0) return false;
        var controller = getControllerForSender(commandSender, strings[0]);
        if (controller == null) return false;
        String[] split = strings[0].split(":", 2);
        String actionId = split[1];
        String[] texts = Arrays.copyOfRange(strings, 1, strings.length);
        Binding binding = controller.getProxyHandler().getNamedBinding(actionId);
        if (binding == null) return false;
        Method method = binding.method();
        if (method == null) return false;
        Object[] params = replaceParams(binding.params(), texts);
        try {
            Object result = method.invoke(controller, params);
            if (result instanceof Boolean && result.equals(false)) return false;
            if (result instanceof BaseComponent component) commandSender.spigot().sendMessage(component);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    // strings[0] must be "controllerId:actionId"
    public List<String> handleTabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length == 0) return null;
        var controller = getControllerForSender(commandSender, strings[0]);
        if (controller == null) return null;
        String[] split = strings[0].split(":");
        String actionId = split[1];
        String[] texts = Arrays.copyOfRange(strings, 1, strings.length);
        Binding binding = controller.getProxyHandler().getNamedBinding(actionId);
        if (binding == null) return null;
        Method tabMethod = binding.tabMethod();
        if (tabMethod == null) return null;
        if (!canTabComplete(binding.params(), texts)) return List.of();
        Object[] params = replaceParams(binding.params(), texts);
        try {
            Object result = tabMethod.invoke(controller, params);
            if (result == null) return List.of();
            return (List<String>) result;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        return handleCommand(commandSender, strings);
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return handleTabComplete(commandSender, strings);
    }

    private static final String[] dummyArguments = new String[100];
    static {
        for (var i=0; i<dummyArguments.length; i++){
            dummyArguments[i] = String.valueOf(i);
        }
    }

    private static Object[] replaceParams(Object[] originParams, String[] texts){
        if (originParams == null) return new Object[0];
        Object[] result = Arrays.copyOf(originParams, originParams.length);
        for (var i=0; i<originParams.length; i++){
            Object originParam = originParams[i];
            if (originParam == dummyArguments) {
                result[i] = texts;
                continue;
            }
            if (originParam instanceof String str) {
                try {
                    int index = Integer.parseInt(str);
                    if (dummyArguments[index] == originParam){
                        if (index >= texts.length) {
                            result[i] = null;
                        } else {
                            result[i] = texts[index];
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return result;
    }

    private static boolean canTabComplete(Object[] originParams, String[] texts){
        if (texts.length == 0) return false;
        int inputIndex = texts.length-1;
        for (Object originParam : originParams) {
            if (originParam == dummyArguments) return true;
            if (!(originParam instanceof String str)) continue;
            try {
                int index = Integer.parseInt(str);
                if (index != inputIndex) continue;
                if (dummyArguments[index] == originParam) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }
}
