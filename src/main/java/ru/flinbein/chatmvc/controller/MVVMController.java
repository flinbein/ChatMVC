package ru.flinbein.chatmvc.controller;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.CommandSender;
import ru.flinbein.chatmvc.template.TemplateParser;

import java.lang.reflect.*;
import java.util.*;


// only for chat.
public class MVVMController {

    public String commandPrefix;
    protected CommandSender commandSender;
    protected TemplateParser parser;
    private boolean registered = false;
    private final HashMap<String, Binding> bindings = new HashMap<>();
    private Object proxyValue;
    private String[] dummyArguments = new String[100];
    private DummyInvocationHandler proxyHandler;

    public MVVMController() {}

    public final String[] getArgs(){
        return dummyArguments;
    }

    @Hide()
    public final void registerHandlerParameters(CommandSender sender, ClassLoader classLoader, String commandPrefixWithId, String[] dummyArguments) {
        if (registered) {
            throw new RuntimeException("Controller already registered: "+commandPrefixWithId);
        }
        registered = true;
        bindings.clear();
        this.commandSender = sender;
        this.commandPrefix = commandPrefixWithId;
        this.parser = TemplateParser.getForClassLoader(classLoader);
        this.dummyArguments = dummyArguments;
        Class<? extends MVVMController> controllerClass = this.getClass();
        DummyInterfaceHolder dummyHolder = DummyInterfaceHolder.getForClass(controllerClass);
        proxyHandler = new DummyInvocationHandler(dummyHolder, this, commandPrefix, bindings);
        proxyValue = dummyHolder.createProxy(proxyHandler);
    }

    @Hide()
    public final DummyInvocationHandler getProxyHandler(){
        return proxyHandler;
    }

    @Bind()
    public final void render(String templatePath) {
        proxyHandler.upgradeBindingsVersion();
        try {
            BaseComponent baseComponent = parser.parseTemplateToComponent(templatePath, proxyValue);
            commandSender.spigot().sendMessage(baseComponent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
