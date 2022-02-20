package ru.flinbein.chatmvc;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import ru.flinbein.chatmvc.handler.CommandHandler;


public class ChatMVCPlugin extends JavaPlugin {

    public CommandHandler commandHandler = new CommandHandler("chatmvc");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return commandHandler.handleCommand(sender, args);
    }
}
