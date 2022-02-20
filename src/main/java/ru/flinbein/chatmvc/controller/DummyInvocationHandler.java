package ru.flinbein.chatmvc.controller;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;

public class DummyInvocationHandler implements InvocationHandler {

    private final DummyInterfaceHolder interfaceHolder;
    private final MVVMController controller;
    private final HashMap<String, Binding> bindings;
    private final String commandPrefix;

    public DummyInvocationHandler(DummyInterfaceHolder interfaceHolder, MVVMController controller, String commandPrefix, HashMap<String, Binding> bindings) {
        this.interfaceHolder = interfaceHolder;
        this.controller = controller;
        this.commandPrefix = commandPrefix;
        this.bindings = bindings;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method sourceMethod = interfaceHolder.getCachedMethod(method);
        if (method.isAnnotationPresent(Bind.class)) {
            return createBinding(sourceMethod, args);
        } else {
            return sourceMethod.invoke(controller, args);
        }
    }

    public final String createBinding(Method method, Object... params) {
        Method tabMethod = null;
        try {
            Class<?>[] parameterTypes = method.getParameterTypes();
            String tabMethodName = method.getName() + "TabComplete";
            Bind[] annotationsByType = method.getAnnotationsByType(Bind.class);
            if (annotationsByType.length > 0) {
                String bindTabValue = annotationsByType[0].tab();
                if (!bindTabValue.isEmpty()) tabMethodName = bindTabValue;
            }
            tabMethod = this.getClass().getMethod(tabMethodName, parameterTypes);
        } catch (NoSuchMethodException ignored) {}
        String actionId = registerBinding(method, tabMethod, params);
        return "/" + commandPrefix + ":" + actionId;
    }


    private String registerBinding(Method method, Method tabMethod, Object[] params){
        Binding newBinding = new Binding(method, tabMethod, params, currentBindingVersion);
        String foundBinding = null;
        for (var entry : bindings.entrySet()) {
            Binding binding = entry.getValue();
            if (binding.method() != method) continue;
            if (binding.tabMethod() != tabMethod) continue;
            if (!Arrays.equals(binding.params(), params)) continue;
            foundBinding = entry.getKey();
            break;
        }
        if (foundBinding != null) {
            bindings.put(foundBinding, newBinding);
            return foundBinding;
        }
        String bindingKey = getNewActionId();
        bindings.put(bindingKey, newBinding);
        return bindingKey;
    }

    private long currentBindingVersion = 0;
    public void upgradeBindingsVersion(){
        currentBindingVersion += 1;
        Set<String> keysToRemove = new HashSet<>();
        for (Map.Entry<String, Binding> e : bindings.entrySet()) {
            Binding binding = e.getValue();
            if (binding.version() + 10 < currentBindingVersion) keysToRemove.add(e.getKey());
        }
        for (String key : keysToRemove) bindings.remove(key);
    }

    public final Binding getNamedBinding(String bindingKey){
        return bindings.get(bindingKey);
    }

    private long freeIntActionId = 1;
    private String getNewActionId() {
        return Long.toString(freeIntActionId++, 32);
    }
}
