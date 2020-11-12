package kpo.rpis81.alexandrov.parameters;

/**
 * Перечисление, описывающее режимы построения графа:
 * {@link #RANDOM} - c использованием датчика случайных чисел,
 * {@link #FIXED} - для создания детерминированного графа
 * @author Илья Александров
 */
public enum BuildMode {
    RANDOM,
    FIXED
}
