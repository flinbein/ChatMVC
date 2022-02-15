package ru.flinbein.chatmvc.controller;

import java.io.InputStream;

public interface ParserController extends Controller {

    public InputStream parsePattern(String fileName);

}
