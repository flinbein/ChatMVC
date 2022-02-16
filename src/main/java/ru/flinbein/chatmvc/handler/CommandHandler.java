package ru.flinbein.chatmvc.handler;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;
import ru.flinbein.chatmvc.controller.MVCController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class CommandHandler implements TabExecutor {

    private int maxControllersPerSender = 5;
    private final Plugin plugin;
    private final String commandPrefix;
    // Player = [controllerId = MVCController]
    private final HashMap<CommandSender, LinkedHashMap<String, MVCController>> senderControllerMap = new HashMap<>();

    public CommandHandler(PluginCommand command) {
        this.plugin = command.getPlugin();
        command.setExecutor(this);
        command.setTabCompleter(this);
        commandPrefix = command.getName();
    }

    // commandPrefix without slash
    public CommandHandler(Plugin plugin, String commandPrefix) {
        this.plugin = plugin;
        this.commandPrefix = commandPrefix;
    }

    public int getMaxControllersPerSender() { return maxControllersPerSender; }
    public void setMaxControllersPerSender(int maxControllersPerSender) {
        this.maxControllersPerSender = maxControllersPerSender;
    }

    public Plugin getPlugin() { return plugin; }

    private int freeIntControllerId = 1;
    private String getNewControllerId() {
        return Integer.toString(freeIntControllerId++, 32);
    }

    public void registerController(CommandSender sender, MVCController controller) {
        var controllerMap = senderControllerMap.getOrDefault(sender, new LinkedHashMap<>());
        senderControllerMap.put(sender, controllerMap);
        if (controllerMap.size() > maxControllersPerSender) {
            String firstKey = controllerMap.keySet().iterator().next();
            controllerMap.remove(firstKey);
            // Controller on destroy?
        }
        var controllerId = getNewControllerId();
        controller.register(sender, plugin, commandPrefix + " " + controllerId);
        controllerMap.put(controllerId, controller);
    }

    private MVCController getControllerForSender(CommandSender commandSender, String arg1) {
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
        String[] splited = strings[0].split(":");
        String actionId = splited[1];
        String[] texts = Arrays.copyOfRange(strings, 1, strings.length);
        return controller.onCommand(actionId, texts);
    }
    // strings[0] must be "controllerId:actionId"
    public List<String> handleTabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length == 0) return null;
        var controller = getControllerForSender(commandSender, strings[0]);
        if (controller == null) return null;
        String[] splited = strings[0].split(":");
        String actionId = splited[1];
        String[] texts = Arrays.copyOfRange(strings, 1, strings.length);
        return controller.onTabComplete(actionId, texts);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        return handleCommand(commandSender, strings);
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return handleTabComplete(commandSender, strings);
    }
}