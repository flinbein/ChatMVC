package ru.flinbein.chatmvc;

public class LocalClassLoader extends ClassLoader {

    private static LocalClassLoader self = new LocalClassLoader();

    public static LocalClassLoader getInstance(){
        return self;
    }

    private LocalClassLoader(){
        super();
    }
}
