package kpo.rpis81.alexandrov.tools;

import edu.uci.ics.jung.graph.DelegateTree;
import kpo.rpis81.alexandrov.elements.Edge;
import kpo.rpis81.alexandrov.elements.Vertex;
import org.apache.commons.math3.stat.descriptive.AbstractStorelessUnivariateStatistic;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;

/**
 * Класс-калькулятор, который выполняет все математические операции для графов.
 * {@link #DEFAULT_RESULT} - результат, возвращаемый калькулятором по умолчанию при
 * неожиданном порождении внутреннего исключения,
 * {@link #VARIANCE} - константа функции математической дисперсии
 * {@link #MEAN} - константа функции математического ожидания
 * @author Илья Александров
 */
public class GraphMath {

    private static final double DEFAULT_RESULT = 0D;
    private static final Variance VARIANCE = new Variance();
    private static final Mean MEAN = new Mean();

    /**
     * Вычисляет количество вершин
     * @param tree дерево
     * @return количество вершин
     */
    private static int vertexCount(DelegateTree<Vertex, Edge> tree) {
        return GraphBuilder.getVertices(tree).size();
    }

    /**
     * Вычисляет количество висячих вершин
     * @param tree дерево
     * @return количество висячих вершин
     */
    public static int leavesCount(DelegateTree<Vertex, Edge> tree) {
        return GraphBuilder.getLeaves(tree).size();
    }

    /**
     * Вычисляет параметр альфа для дерева
     * @param tree дерево
     * @return параметр альфа
     */
    public static double alpha(DelegateTree<Vertex, Edge> tree) {
        return (double) tree.getVertexCount() / leavesCount(tree);
    }

    /**
     * Абстрактная реализация функции, подсчитывающей среднее значение для совокупности деревьев.
     * Используется в ряде методов ниже
     * @param trees список деревьев
     * @param mapper функция, заменяющая дерево результатом вычисления указанного параметра
     * для этого дерева
     * @return вычисляемое среднее значение
     */
    private static double countAverage(List<DelegateTree<Vertex, Edge>> trees,
                                       ToDoubleFunction<? super DelegateTree<Vertex, Edge>> mapper) {
        return trees.stream()
                .mapToDouble(mapper)
                .average()
                .orElse(DEFAULT_RESULT);
    }

    /**
     * Подсчитывает среднее количество вершин
     * @param trees список деревьев
     * @return среднее число вершин
     * @see #countAverage(List, ToDoubleFunction) - реализация метода
     */
    public static double averageVertexCount(List<DelegateTree<Vertex, Edge>> trees) {
        return countAverage(new ArrayList<>(trees), DelegateTree::getVertexCount);
    }

    /**
     * Подсчитывает среднее количество висячих вершин
     * @param trees список деревьев
     * @return среднее число висячих вершин
     * @see #countAverage(List, ToDoubleFunction) - реализация метода
     */
    public static double averageLeavesCount(List<DelegateTree<Vertex, Edge>> trees) {
        return countAverage(new ArrayList<>(trees), GraphMath::leavesCount);
    }

    /**
     * Подсчитывает среднее значение альфа
     * @param trees список деревьев
     * @return среднее значение альфа
     * @see #countAverage(List, ToDoubleFunction) - реализация метода
     */
    public static double averageAlpha(List<DelegateTree<Vertex, Edge>> trees) {
        return countAverage(new ArrayList<>(trees), GraphMath::alpha);
    }

    /**
     * Подсчитывает среднюю высоту (число уровней иерархии)
     * @param trees список деревьев
     * @return средняя высота
     * @see #countAverage(List, ToDoubleFunction) - реализация метода
     */
    public static double averageHeight(List<DelegateTree<Vertex, Edge>> trees) {
        return countAverage(new ArrayList<>(trees), DelegateTree::getHeight);
    }

    /**
     * Абстрактная реализация функции, вычисляющей математическое ожидание и дисперсию для ряда параметров
     * @param statistic объект математической функции из пакета Apache Commons Math
     * @param valueSeries ряд значений, соответствующих определенному параметру
     * @param trees список деревьев
     * @param calculation вычислительная операция, применяемая к каждому дереву из списка
     * @return значение математического ожидания/дисперсии
     */
    private static double evaluate(AbstractStorelessUnivariateStatistic statistic,
                                   List<Double> valueSeries,
                                   List<DelegateTree<Vertex, Edge>> trees,
                                   Consumer<DelegateTree<Vertex, Edge>> calculation) {
        trees.forEach(calculation);
        return statistic.evaluate(valueSeries.stream()
                .mapToDouble(Double::doubleValue)
                .toArray());
    }

    /**
     * Вычисляет дисперсию числа вершин для совокупности деревьев
     * @param trees список деревьев
     * @return дисперсия числа вершин для совокупности деревьев
     * @see #evaluate(AbstractStorelessUnivariateStatistic, List, List, Consumer) - реализация метода
     */
    public static double varianceForVertices(List<DelegateTree<Vertex, Edge>> trees) {
        List<Double> vertexSeries = new ArrayList<>();
        return evaluate(VARIANCE, vertexSeries, trees,
                t -> vertexSeries.add((double) t.getVertexCount()));
    }

    /**
     * Вычисляет дисперсию числа висячих вершин для совокупности деревьев
     * @param trees список деревьев
     * @return дисперсия числа висячих вершин для совокупности деревьев
     * @see #evaluate(AbstractStorelessUnivariateStatistic, List, List, Consumer) - реализация метода
     */
    public static double varianceForLeaves(List<DelegateTree<Vertex, Edge>> trees) {
        List<Double> leafSeries = new ArrayList<>();
        return evaluate(VARIANCE, leafSeries, trees, t -> leafSeries.add((double) leavesCount(t)));
    }

    /**
     * Вычисляет дисперсию альфа для совокупности деревьев
     * @param trees список деревьев
     * @return дисперсия альфа для совокупности деревьев
     * @see #evaluate(AbstractStorelessUnivariateStatistic, List, List, Consumer) - реализация метода
     */
    public static double varianceForAlpha(List<DelegateTree<Vertex, Edge>> trees) {
        List<Double> alphaSeries = new ArrayList<>();
        return evaluate(VARIANCE, alphaSeries, trees, t -> alphaSeries.add(alpha(t)));
    }

    /**
     * Вычисляет дисперсию высоты для совокупности деревьев
     * @param trees список деревьев
     * @return дисперсия высоты для совокупности деревьев
     * @see #evaluate(AbstractStorelessUnivariateStatistic, List, List, Consumer) - реализация метода
     */
    public static double varianceForHeight(List<DelegateTree<Vertex, Edge>> trees) {
        List<Double> heightSeries = new ArrayList<>();
        return evaluate(VARIANCE, heightSeries, trees, t -> heightSeries.add((double) t.getHeight()));
    }

    /**
     * Вычисляет математическое ожидание альфа для совокупности деревьев
     * @param trees список деревьев
     * @return математическое ожидание альфа для совокупности деревьев
     * @see #evaluate(AbstractStorelessUnivariateStatistic, List, List, Consumer) - реализация метода
     */
    public static double meanForAlpha(List<DelegateTree<Vertex, Edge>> trees) {
        List<Double> alphaSeries = new ArrayList<>();
        return evaluate(MEAN, alphaSeries, trees, t -> alphaSeries.add(alpha(t)));
    }

    /**
     * Вычисляет математическое ожидание для ребер, исходящих из каждого узла дерева
     * @param tree дерево
     * @return математическое ожидание для ребер графа
     */
    public static double meanForEdges(DelegateTree<Vertex, Edge> tree) {
        return MEAN.evaluate(tree.getVertices().stream()
                .mapToDouble(tree::getChildCount)
                .toArray());
    }
}
