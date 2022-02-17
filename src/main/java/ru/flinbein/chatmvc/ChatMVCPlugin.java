package ru.flinbein.chatmvc;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.flinbein.chatmvc.example.ExampleInventoryController;
import ru.flinbein.chatmvc.handler.CommandHandler;
import ru.flinbein.chatmvc.template.TemplateParser;

import java.util.HashMap;

public class ChatMVCPlugin extends JavaPlugin {

    TemplateParser tplParser = TemplateParser.getForPlugin(this);
    CommandHandler commandHandler;

    @Override
    public void onEnable() {
        super.onEnable();
        PluginCommand command = this.getCommand("cmvcx");
        commandHandler = new CommandHandler(command);

        // create some MVVM Factory and pass there mvvm
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equals("cmvcx")) return false;
        if (command.getName().equals("cmvcex")) {
            var controller = commandHandler.registerController(sender, new ExampleInventoryController());
            controller.render("templates/controller_example.ftlx");
            return true;
        }

        if (args.length == 2 && args[0].equals("setHeldItemSlot") && sender instanceof Player player) {
            player.getInventory().setHeldItemSlot(Integer.parseInt(args[1]));
            return true;
        }

        var model = new HashMap<>();
        model.put("player", sender);
        BaseComponent msgModel = tplParser.parseTemplateToComponent("templates/chatmvc_example.ftlx", model);
        sender.spigot().sendMessage(msgModel);
        return true;

    }
}
