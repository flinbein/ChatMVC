package ru.flinbein.chatmvc.controller;

import java.io.InputStream;

public interface BindableController extends ParserController {

    void onCommand(String actionId, String text);
}
