package ru.flinbein.chatmvc.controller;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.CommandSender;
import ru.flinbein.chatmvc.template.TemplateParser;


// only for chat.
public class MVVMController {

    protected CommandSender commandSender;
    protected TemplateParser parser;
    private boolean registered = false;
    private Object proxyValue;
    private DummyInvocationHandler proxyHandler;
    private String[] dummyArguments = new String[100];

    public MVVMController() {}

    public final String[] getArgs(){
        return dummyArguments;
    }

    @Hide()
    public final void registerHandlerParameters(CommandSender sender, ClassLoader classLoader, String commandPrefixWithId, String[] dummyArguments) {
        if (registered) throw new RuntimeException("Controller already registered: "+commandPrefixWithId);
        registered = true;
        this.commandSender = sender;
        this.parser = TemplateParser.getForClassLoader(classLoader);
        this.dummyArguments = dummyArguments;
        proxyHandler = new DummyInvocationHandler( this, commandPrefixWithId);
        proxyValue = proxyHandler.createProxy();
    }

    @Hide()
    public final DummyInvocationHandler getProxyHandler(){
        return proxyHandler;
    }

    @Bind()
    public final void render(String templatePath) {
        new Thread(() -> {
            synchronized (this) {
                proxyHandler.upgradeBindingsVersion();
                try {
                    BaseComponent baseComponent = parser.parseTemplateToComponent(templatePath, proxyValue);
                    commandSender.spigot().sendMessage(baseComponent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
