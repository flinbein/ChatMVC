package ru.flinbein.chatmvc;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.flinbein.chatmvc.template.TemplateParser;

import java.util.HashMap;

public class ChatMVCPlugin extends JavaPlugin {

    TemplateParser tplParser = TemplateParser.getForPlugin(this);

    @Override
    public void onEnable() {
        super.onEnable();
        PluginCommand mvvm = this.getCommand("mvvm");
        // create some MVVM Factory and pass there mvvm
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

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
