package ru.flinbein.chatmvc.controller;

import org.bukkit.command.CommandSender;

import java.io.InputStream;

public class ChatMVCController extends  MVCController {

    public final CommandSender sender;

    public ChatMVCController(String id, CommandSender sender) {
        super(id);
        this.sender = sender;
    }

    @Override
    public void render(InputStream pattern) {
        super.renderPattern(pattern);
    }
}
