package kpo.rpis81.alexandrov.nullhandler;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Аннотация для полей класса.
 * Указывает, какое значение будет присвоено полю вместо null,
 * если оно не проинициализировано при создании объекта.
 * Поддерживает поля трех типов:
 * {@link String}, {@link Boolean}, {@link Integer}
 * @author Илья Александров
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultValue {
    String string() default "";
    boolean bool() default false;
    int integer() default 0;
}
