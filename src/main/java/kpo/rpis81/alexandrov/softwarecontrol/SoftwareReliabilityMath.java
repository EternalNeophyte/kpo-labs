package kpo.rpis81.alexandrov.softwarecontrol;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Класс-калькулятор, содержащий инструменты для проведения расчетов параметров надежности ПО
 * в рамках модели Джелинского-Моранды.
 * {@link #MC} - константа, определяющая точность вычислений для переменных типа {@link BigDecimal}
 * @author Илья Александров
 */
public class SoftwareReliabilityMath {

    private static final MathContext MC = new MathContext(8);

    /**
     * Вычисляет части уравнения сумм
     * @param coeff коэффициент пропорциональности
     * @param errorsList список количества ошибок в соответствии с вариантом задания
     * @return части уравнения сумм
     */
    public static List<BigDecimal> sumEquation(BigDecimal coeff, List<Integer> errorsList) {
        double firstPart = 0D, secondPart = 0D, thirdPart = 0D, forthPart = 0D, kt, exp, doubleExp;
        int t = 0;
        for (Integer e : errorsList) {
            kt = -coeff.doubleValue() * ++t;
            exp = e * Math.exp(kt);
            doubleExp = Math.exp(2 * kt);
            firstPart += doubleExp;
            secondPart += exp * t;
            thirdPart += doubleExp * t;
            forthPart += exp;
        }
        return new ArrayList<>(List.of(BigDecimal.valueOf(firstPart),
                BigDecimal.valueOf(secondPart),
                BigDecimal.valueOf(thirdPart),
                BigDecimal.valueOf(forthPart)));
    }

    /**
     * Проверяет условие остановки
     * @param precision точность в соответствии с вариантом задания
     * @param sums части уравнения сумм
     * @return логическое значение
     */
    public static boolean sumsAreLessThan(final double precision, List<BigDecimal> sums) {
        return sums.get(3)
                .subtract(sums.get(0)
                        .multiply(sums.get(1).divide(sums.get(2), MC)))
                .abs()
                .doubleValue() < precision;
    }

    /**
     * Вычисляет начальное количество ошибок N0
     * @param coeff коэффициент пропорциональности
     * @param sums части уравнения сумм
     * @return начальное количество ошибок N0
     */
    public static BigDecimal initErrorCount(BigDecimal coeff, List<BigDecimal> sums) {
        return sums.get(1).divide(sums.get(2).multiply(coeff), MC);
    }

    /**
     * Вычисляет промежуточное значение
     * @param coeff коэффициент пропорциональности
     * @param initErrorCount начальное количество ошибок N0
     * @return промежуточное значение
     */
    public static BigDecimal intermediateValue(BigDecimal coeff, BigDecimal initErrorCount) {
        return initErrorCount.multiply(coeff);
    }

    /**
     * Вычисляет суммы x, y, параметры a, b
     * @param coeff коэффициент пропорциональности
     * @param intermediateValue промежуточное значение
     * @param errorsList список количества ошибок в соответствии с вариантом задания
     * @return суммы x, y, параметры a, b
     */
    public static HashMap<String, BigDecimal> xAndYSums(BigDecimal coeff, BigDecimal intermediateValue,
                                                        List<Integer> errorsList) {
        double xSum = 0D, ySum = 0D, xSquaredSum = 0D, xByYSum = 0D, exp, a, b;
        int counter = 0;
        for(Integer e : errorsList) {
            exp = Math.exp(coeff.negate().doubleValue() * ++counter);
            xSum += counter;
            ySum += intermediateValue.doubleValue() * exp;
            xSquaredSum += counter * counter;
            xByYSum += counter * intermediateValue.doubleValue() * exp;
        }
        a = (errorsList.size() * xByYSum - xSum * ySum) / (errorsList.size() * xSquaredSum - xSum * xSum);
        b = (ySum - a * xSum) / errorsList.size();
        return new HashMap<>(Map.of(
                "∑xi", BigDecimal.valueOf(xSum),
                "∑yi", BigDecimal.valueOf(ySum),
                "∑xi^2", BigDecimal.valueOf(xSquaredSum),
                "∑xi*yi", BigDecimal.valueOf(xByYSum),
                "a", BigDecimal.valueOf(a),
                "b", BigDecimal.valueOf(b)));
    }

    /**
     * Определяет прогнозируемое количество ошибок для каждого дня разработки
     * @param coeff коэффициент пропорциональности
     * @param intermediateValue промежуточное значение
     * @param errorsList список количества ошибок в соответствии с вариантом задания
     * @return прогнозируемое количество ошибок
     */
    public static HashMap<String, BigDecimal> errorCountList(BigDecimal coeff, BigDecimal intermediateValue,
                                                   List<Integer> errorsList) {
        HashMap<String, BigDecimal> errors = new HashMap<>();
        AtomicInteger index = new AtomicInteger(1);
        errorsList.forEach(e -> errors.put("n" + index.get(), intermediateValue
                .multiply(BigDecimal.valueOf(Math.exp(coeff.negate().doubleValue() * index.getAndIncrement())))
        ));
        return errors;
    }

    /**
     * Определяет количество оставшихся ошибок
     * @param initErrorCount начальное количество ошибок N0
     * @param errorsCountList список прогнозируемых ошибок
     * @return количество оставшихся ошибок
     */
    public static HashMap<String, BigDecimal> errorsLeft(BigDecimal initErrorCount, HashMap<String, BigDecimal> errorsCountList) {
        return new HashMap<>(Map.of(
                "Количество ошибок N", initErrorCount.subtract(BigDecimal.valueOf(errorsCountList
                        .values()
                        .stream()
                        .mapToDouble(BigDecimal::doubleValue)
                        .sum()))
        ));
    }

    /**
     * Вычисляет вероятности появления/непоявления ошибок
     * @param coeff коэффициент пропорциональности
     * @param initErrorCount начальное количество ошибок N0
     * @param limiter ограничивающее значение
     * @return вероятности появления/непоявления ошибок
     */
    public static HashMap<String, BigDecimal> errorProbabilities(BigDecimal coeff, BigDecimal initErrorCount,
                                                                 BigDecimal limiter) {
        int days = 0;
        double prob = 0D, notProb = 0D, exp;
        while (notProb < limiter.doubleValue()) {
            exp = Math.exp(-coeff.doubleValue() * ++days);
            notProb = Math.pow((1 - exp), initErrorCount.doubleValue());
            prob = Math.pow(exp, initErrorCount.doubleValue());
        }
        return new HashMap<>(Map.of(
                "Количество дней t", BigDecimal.valueOf(days),
                "Вероятность появления ошибки", BigDecimal.valueOf(prob),
                "Вероятность непоявления ошибки", BigDecimal.valueOf(notProb)
        ));
    }
}
