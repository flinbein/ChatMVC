package ru.flinbein.chatmvc.controller;

import java.lang.reflect.Method;

public record Binding(
        Method method,
        Method tabMethod,
        Object[] params,
        long version
) {}
