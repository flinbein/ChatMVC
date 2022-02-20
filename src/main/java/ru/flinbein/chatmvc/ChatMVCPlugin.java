package ru.flinbein.chatmvc;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.flinbein.chatmvc.handler.CommandHandler;

import java.util.List;


public class ChatMVCPlugin extends JavaPlugin {

    public CommandHandler commandHandler = new CommandHandler("chatmvc");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return commandHandler.handleCommand(sender, args);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return commandHandler.handleTabComplete(sender, args);
    }
}
