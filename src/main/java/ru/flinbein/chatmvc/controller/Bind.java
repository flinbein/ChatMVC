package ru.flinbein.chatmvc.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)

@Target({ElementType.METHOD})
public @interface Bind {
    String value() default "";
    String tab() default "";
}
