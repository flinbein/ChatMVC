package ru.flinbein.chatmvc.controller;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.CommandSender;
import ru.flinbein.chatmvc.template.TemplateParser;

import java.lang.reflect.*;
import java.util.*;


// only for chat.
public class MVCController {

    private static final Map<Class<?>, DummyInterfaceHolder> dummyInterfaceHolders = new HashMap<>();
    private static final String[] dummyArguments = new String[100];

    static {
        for (var i=0; i<dummyArguments.length; i++){
            dummyArguments[i] = String.valueOf(i);
        }
    }

    public String commandPrefix;
    protected CommandSender commandSender;
    protected TemplateParser parser;
    private boolean registered = false;
    private final HashMap<String, Binding> bindings = new HashMap<>();
    private Object proxyValue;

    public MVCController() {}

    public final String[] getArgs(){
        return dummyArguments;
    }

    @Hide()
    public final void register(CommandSender sender, ClassLoader classLoader, String commandPrefixWithId) {
        if (registered) {
            throw new RuntimeException("Controller already registered: "+commandPrefixWithId);
        }
        registered = true;
        bindings.clear();
        this.commandSender = sender;
        this.commandPrefix = commandPrefixWithId;
        this.parser = TemplateParser.getForClassLoader(classLoader);
        Class<? extends MVCController> controllerClass = this.getClass();
        DummyInterfaceHolder dummyHolder = dummyInterfaceHolders.get(controllerClass);
        if (dummyHolder == null) {
            dummyHolder = new DummyInterfaceHolder(controllerClass);
            dummyInterfaceHolders.put(controllerClass, dummyHolder);
        }
        ClassLoader dummyClassLoader = dummyHolder.ctrlInterface.getClassLoader();
        Class[] dummyClasses = {dummyHolder.ctrlInterface};
        proxyValue = Proxy.newProxyInstance(dummyClassLoader, dummyClasses, new DummyHandler(dummyHolder, this));
    }

    @Hide()
    public BaseComponent parsePattern(String fileName)  {
        try {
            return parser.parseTemplateToComponent(fileName, proxyValue);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private int freeIntActionId = 1;
    private String getNewActionId() {
        return Integer.toString(freeIntActionId++, 32);
    }

    private long currentBindingVersion = 0;

    @Hide()
    public final String bind(Method method, Object... params) {
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
        Binding binding = new Binding(method, tabMethod, params, currentBindingVersion);
        var actionId = getNewActionId();
        bindings.put(actionId, binding);
        return "/" + commandPrefix + ":" + actionId;
    }

    private Object[] replaceParams(Object[] originParams, String[] texts){
        if (originParams == null) return new Object[0];
        Object[] result = Arrays.copyOf(originParams, originParams.length);
        for (var i=0; i<originParams.length; i++){
            Object originParam = originParams[i];
            if (originParam == dummyArguments) {
                result[i] = texts;
                continue;
            }
            if (originParam instanceof String str) {
                try {
                    int index = Integer.parseInt(str);
                    if (dummyArguments[index] == originParam){
                        if (index >= texts.length) {
                            result[i] = null;
                        } else {
                            result[i] = texts[index];
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return result;
    }

    private boolean canTabComplete(Object[] originParams, String[] texts){
        if (texts.length == 0) return false;
        int inputIndex = texts.length-1;
        for (Object originParam : originParams) {
            if (originParam == dummyArguments) return true;
            if (!(originParam instanceof String str)) continue;
            try {
                int index = Integer.parseInt(str);
                if (index != inputIndex) continue;
                if (dummyArguments[index] == originParam) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    @Hide()
    public final boolean onCommand(String actionId, String[] texts) {
        var binding = bindings.get(actionId);
        if (binding == null) return false;
        Method method = binding.method;
        Object[] params = replaceParams(binding.params, texts);
        try {
            Object result = method.invoke(this, params);
            if (result instanceof Boolean && result.equals(false)) return false;
            if (result instanceof BaseComponent component) commandSender.spigot().sendMessage(component);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Hide()
    public final List<String> onTabComplete(String actionId, String[] texts){
        var binding = bindings.get(actionId);
        if (binding == null) return List.of();
        if (!canTabComplete(binding.params, texts)) return List.of();
        Method tabMethod = binding.tabMethod;
        if (tabMethod == null) return null; // suggest players
        Object[] params = replaceParams(binding.params, texts);
        try {
            Object result = tabMethod.invoke(this, params);
            if (result == null) return List.of();
            return (List<String>) result;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private void upgradeBindingsVersion(){
        currentBindingVersion += 1;
        Set<String> keysToRemove = new HashSet<>();
        for (Map.Entry<String, Binding> e : bindings.entrySet()) {
            Binding binding = e.getValue();
            if (binding.version + 10 < currentBindingVersion) keysToRemove.add(e.getKey());
        }
        for (String key : keysToRemove) bindings.remove(key);
    }

    @Bind()
    public final void render(String patternFileName) {
        upgradeBindingsVersion();
        BaseComponent baseComponent = parsePattern(patternFileName);
        commandSender.spigot().sendMessage(baseComponent);
    }

    record Binding(Method method, Method tabMethod, Object[] params, long version) {}

    private static class DummyInterfaceHolder {

        static ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.ofThisVm()).with(TypeValidation.DISABLED);

        private Class<?> ctrlInterface;

        private Map<Method, Method> cacheMethods = new HashMap<>();

        public Object callMethod(Method method, MVCController source, Object[] args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
            Method sourceMethod = cacheMethods.get(method);
            if (sourceMethod == null) {
                sourceMethod = source.getClass().getMethod(method.getName(), method.getParameterTypes());
                cacheMethods.put(method, sourceMethod);
            }
            if (method.isAnnotationPresent(Bind.class)) {
                return source.bind(sourceMethod, args);
            } else {
                return sourceMethod.invoke(source, args);
            }
        }

        private DummyInterfaceHolder(Class<? extends MVCController> controllerClass){
            DynamicType.Builder<?> builder = byteBuddy.makeInterface().name(this.getClass().getSimpleName() + "__EX");
            Bind[] bindAnnotations = controllerClass.getAnnotationsByType(Bind.class);
            boolean autoBindMethods = true;
            if (bindAnnotations.length > 1) throw new RuntimeException("More than 1 @Bind(): "+controllerClass.getName());
            if (bindAnnotations.length == 1 && !bindAnnotations[0].value()) autoBindMethods = false;
            Method[] methods = controllerClass.getMethods();
            for (Method method : methods) {
                int modifiers = method.getModifiers();
                if (method.getDeclaringClass().equals(Object.class) && Modifier.isFinal(modifiers)) continue;
                if (!Modifier.isPublic(modifiers)) continue;
                if (method.isAnnotationPresent(Hide.class)) continue;
                Class<?>[] parameterTypes = method.getParameterTypes();
                boolean bindRequired = bindRequired(method, autoBindMethods);
                var returnType = bindRequired ? String.class : method.getReturnType();
                DynamicType.Builder.MethodDefinition<?> methodBuilder = builder
                        .defineMethod(method.getName(), returnType, Visibility.PUBLIC)
                        .withParameters(parameterTypes)
                        .throwing(method.getExceptionTypes())
                        .withoutCode();
                if (bindRequired) {
                    AnnotationDescription bindDesc = AnnotationDescription.Builder.ofType(Bind.class).define("value", true).define("tab", "").build();
                    methodBuilder = methodBuilder.annotateMethod(bindDesc);
                }
                builder = methodBuilder;
            }
            ctrlInterface = builder.make().load(this.getClass().getClassLoader()).getLoaded();
        }

        private boolean bindRequired(Method method, boolean autoBindMethods){
            Bind[] bindAnnotations = method.getAnnotationsByType(Bind.class);
            if (bindAnnotations.length > 1) throw new RuntimeException("More than 1 @Bind(): "+method.getDeclaringClass()+ " "+method.getName());
            if (bindAnnotations.length == 1) return bindAnnotations[0].value();
            if (!autoBindMethods) return false;
            return method.getReturnType().equals(void.class);
        }
    }

    private static class DummyHandler implements InvocationHandler {

        private final DummyInterfaceHolder interfaceHolder;
        private final MVCController controller;

        public DummyHandler(DummyInterfaceHolder interfaceHolder, MVCController controller) {
            this.interfaceHolder = interfaceHolder;
            this.controller = controller;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return interfaceHolder.callMethod(method, controller, args);
        }
    }
}
