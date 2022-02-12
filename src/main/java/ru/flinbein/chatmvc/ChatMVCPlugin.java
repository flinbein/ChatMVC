package ru.flinbein.chatmvc;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import ru.flinbein.chatmvc.xml.MVCXmlParser;

public class ChatMVCPlugin extends JavaPlugin {

    MVCXmlParser parser = new MVCXmlParser();

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ClassLoader classLoader = this.getClass().getClassLoader();
        try (var xmlStream = classLoader.getResourceAsStream("chatmvc_text.xml")){
            BaseComponent msg = parser.parse(xmlStream);
            sender.spigot().sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return true;
    }
}
