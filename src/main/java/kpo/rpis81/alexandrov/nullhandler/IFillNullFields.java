package kpo.rpis81.alexandrov.nullhandler;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Интерфейс, который работает с полями класса,
 * помеченными аннотацией {@link DefaultValue}
 * @param <T> класс для имплементации
 * @author Илья Александров
 */
public interface IFillNullFields<T> {
    
    default String nullMessage() {
        return "Ошибка. Параметр не может иметь значение null";
    }

    /**
     * Заполняет поля, имеющие значение null при инициализации.
     * Работает с использованием механизма рефлексии
     * @param t объект класса, реализующего данный интерфейс
     */
    default void fillNullFields(T t) {
        try {
            for(Field f : t.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                if(Objects.isNull(f.get(t)) && f.isAnnotationPresent(DefaultValue.class)) {
                    if(f.getType().equals(String.class)) {
                        f.set(t, f.getAnnotation(DefaultValue.class).string());
                    }
                    else if(f.getType().equals(boolean.class)) {
                        f.set(t, f.getAnnotation(DefaultValue.class).bool());
                    }
                    else if(f.getType().equals(int.class)) {
                        f.set(t, f.getAnnotation(DefaultValue.class).integer());
                    }
                }
            }
        }
        catch(IllegalAccessException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }   
}
