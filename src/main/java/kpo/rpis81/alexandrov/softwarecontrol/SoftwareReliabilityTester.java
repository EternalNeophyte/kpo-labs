package kpo.rpis81.alexandrov.softwarecontrol;

import kpo.rpis81.alexandrov.sheets.DataExporter;
import kpo.rpis81.alexandrov.tools.GraphBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Класс, отвечающий за тестирование модели надежности ПО.
 * Ключевые атрибуты:
 * {@link #CONDITIONAL_ZERO} - в методическом пособии сформулировано как
 * "очень малое значение, соответствующее нулевому количеству ошибок",
 * {@link #STEP} - шаг изменения коэффициента пропорциональности,
 * {@link #STOPPER} - значение, при котором происходит
 * принудительная остановка моделирования,
 * {@link #LIMITER} - ограничивающее значение для расчетов
 * вероятностей появления/непоявления ошибок,
 * {@link #errors} - список числа ошибок, возникших в каждый из 7 дней
 * (соответствует варианту задания)
 * @author Илья Александров
 */
public class SoftwareReliabilityTester {

    private static final double CONDITIONAL_ZERO = 0.0001d;
    private static final BigDecimal STEP = BigDecimal.valueOf(CONDITIONAL_ZERO);
    private static final BigDecimal STOPPER = BigDecimal.valueOf(1);
    private static final BigDecimal LIMITER = STOPPER.subtract(STEP);

    private List<Integer> errors;

    /**
     * Подсчитывает все параметры в рамках 6-9 лабораторных сразу и
     * добавляет их в коллекцию для отправки в Google Sheets
     * @return результаты всех расчетов
     */
    private List<List<Object>> calculateAndGetResults() {
        List<List<Object>> results = new ArrayList<>();
        BigDecimal coeff = new BigDecimal("0");
        while (coeff.compareTo(STOPPER) <= 0) {
            coeff = coeff.add(STEP);
            List<BigDecimal> sums = SoftwareReliabilityMath.sumEquation(coeff, errors);
            if (SoftwareReliabilityMath.sumsAreLessThan(CONDITIONAL_ZERO, sums)) {
                BigDecimal initErrorCount = SoftwareReliabilityMath.initErrorCount(coeff, sums);
                BigDecimal intermediateValue = SoftwareReliabilityMath.intermediateValue(coeff, initErrorCount);
                results.add(List.of("Начальное число ошибок N0", initErrorCount));
                results.add(List.of("Коэффициент пропорциональности", coeff));
                HashMap<String, BigDecimal> errorCountList = SoftwareReliabilityMath
                        .errorCountList(coeff, intermediateValue, errors);
                results.addAll(DataExporter.wrapParametersFor(errorCountList));
                results.addAll(DataExporter.wrapParametersFor(SoftwareReliabilityMath
                        .errorsLeft(initErrorCount, errorCountList)));
                results.addAll(DataExporter.wrapParametersFor(SoftwareReliabilityMath
                        .errorProbabilities(coeff, initErrorCount, LIMITER)));
                results.addAll(DataExporter.wrapParametersFor(SoftwareReliabilityMath
                        .xAndYSums(coeff, intermediateValue, errors)));
                break;
            }
        }
        return results;
    }

    /**
     * Отправляет данные по лабораторным 6-9 в Google Sheets
     */
    public void exportTasksToGoogleSheets() {
        DataExporter.writeTo("Лаб.6-9", calculateAndGetResults());
    }

    /**
     * Запускает {@link GraphBuilder.InnerBuilder}
     * @return экземпляр вложенного класса
     */
    public static InnerBuilder deploy() {
        return new SoftwareReliabilityTester().new InnerBuilder();
    }

    /**
     * Вложенный класс - реализация паттерна "Builder" ("Строитель")
     * Содержит сеттеры для полей внешнего класса
     */
    public class InnerBuilder {

        private InnerBuilder() { }

        private List<Integer> initErrorsListIfNull() {
            if(Objects.isNull(SoftwareReliabilityTester.this.errors)) {
                SoftwareReliabilityTester.this.errors = new ArrayList<>();
            }
            return SoftwareReliabilityTester.this.errors;
        }

        public InnerBuilder addErrors(int errorCount) {
            initErrorsListIfNull().add(errorCount);
            return this;
        }

        public InnerBuilder setErrors(List<Integer> errorsList) {
            initErrorsListIfNull().addAll(errorsList);
            return this;
        }

        /**
         * Окончательно инициализирует объект класса {@link SoftwareReliabilityTester}
         * @return экземпляр внешнего класса
         */
        public SoftwareReliabilityTester init() {
            initErrorsListIfNull();
            return SoftwareReliabilityTester.this;
        }
    }
}
