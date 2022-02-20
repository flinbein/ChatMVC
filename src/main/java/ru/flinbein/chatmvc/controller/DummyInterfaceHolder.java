package ru.flinbein.chatmvc.controller;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class DummyInterfaceHolder {

    private static final Map<Class<?>, DummyInterfaceHolder> holders = new HashMap<>();
    private static ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.ofThisVm()).with(TypeValidation.DISABLED);

    private Class<?> dummyInterface;
    private Class<? extends MVVMController> controllerClass;
    private Map<Method, Method> cacheMethods = new HashMap<>();

    public static DummyInterfaceHolder getForClass(Class<? extends MVVMController> controllerClass){
        return holders.computeIfAbsent(controllerClass, (ignored) -> new DummyInterfaceHolder(controllerClass));
    }

    public Class<?> getDummyInterface() {
        return dummyInterface;
    }

    public Method getCachedMethod(Method proxyMethod) throws NoSuchMethodException {
        Method result = cacheMethods.get(proxyMethod);
        if (result != null) return result;
        result = controllerClass.getMethod(proxyMethod.getName(), proxyMethod.getParameterTypes());
        cacheMethods.put(proxyMethod, result);
        return result;
    }

    private DummyInterfaceHolder(Class<? extends MVVMController> controllerClass){
        this.controllerClass = controllerClass;
        DynamicType.Builder<?> builder = byteBuddy.makeInterface().name(this.getClass().getSimpleName() + "__EX");
        Method[] methods = controllerClass.getMethods();
        for (Method method : methods) {
            int modifiers = method.getModifiers();
            if (method.getDeclaringClass().equals(Object.class) && Modifier.isFinal(modifiers)) continue;
            if (!Modifier.isPublic(modifiers)) continue;
            if (method.isAnnotationPresent(Hide.class)) continue;
            boolean bindRequired = bindRequired(method);
            Class<?>[] parameterTypes = method.getParameterTypes();
            var returnType = bindRequired ? String.class : method.getReturnType();
            DynamicType.Builder.MethodDefinition<?> methodBuilder = builder
                    .defineMethod(method.getName(), returnType, Visibility.PUBLIC)
                    .withParameters(parameterTypes)
                    .throwing(method.getExceptionTypes())
                    .withoutCode();
            if (bindRequired) {
                AnnotationDescription bindDesc = AnnotationDescription.Builder.ofType(Bind.class).define("value", "").define("tab", "").build();
                methodBuilder = methodBuilder.annotateMethod(bindDesc);
            }
            builder = methodBuilder;
        }
        dummyInterface = builder.make().load(this.getClass().getClassLoader()).getLoaded();
    }

    private boolean bindRequired(Method method){
        if (method.isAnnotationPresent(Bind.class)) return true;
        return method.getReturnType().equals(void.class);
    }
}