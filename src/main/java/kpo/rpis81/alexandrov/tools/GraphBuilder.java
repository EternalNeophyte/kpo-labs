package kpo.rpis81.alexandrov.tools;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import kpo.rpis81.alexandrov.elements.Edge;
import kpo.rpis81.alexandrov.elements.Vertex;
import kpo.rpis81.alexandrov.nullhandler.DefaultValue;
import kpo.rpis81.alexandrov.nullhandler.IFillNullFields;
import kpo.rpis81.alexandrov.parameters.BuildMode;
import kpo.rpis81.alexandrov.sheets.DataExporter;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Класс, отвечающий за создание и визуализацию графов (деревьев).
 * Ключевые атрибуты:
 * {@link #trees} - список генерируемых деревьев,
 * {@link #buildMode} - определяет режим построения деревьев,
 * {@link #random} - датчик случайных чисел,
 * {@link #graphsCount} - количество генерируемых деревьев. Соответствует варианту лабораторных
 * и обозначается как R в методическом пособии;
 * {@link #edgesCount} - максимальное число ребер, соединяющихся с вершиной.
 * Соответствует варианту лабораторных и обозначается как m в методическом пособии;
 * {@link #vertexLimit} - максимально число вершин дерева согласно правилу остановки.
 * Соответствует варианту лабораторных и обозначается как N в методическом пособии
 * @author Илья Александров
 */
public class GraphBuilder implements Runnable, IFillNullFields<GraphBuilder> {

    private final static int INIT_ROOT_AMOUNT = 1;
    private final static int MIN_VERTEX_AMOUNT = 10;

    private List<DelegateTree<Vertex, Edge>> trees;
    private BuildMode buildMode;
    private Random random;

    @DefaultValue(integer = 400)
    private int graphsCount;

    @DefaultValue(integer = 4)
    private int edgesCount;

    @DefaultValue(integer = 100)
    private int vertexLimit;

    /**
     * Сортирует список вершин по порядковому номеру
     * @param tree дерево
     * @return список вершин
     */
    public static List<Vertex> getVertices(DelegateTree<Vertex, Edge> tree) {
        return tree.getVertices()
                .stream()
                .sorted(Vertex::compareTo)
                .collect(Collectors.toList());
    }

    /**
     * Выбирает висячие вершины из списка вершин,
     * отсортированных методом {@link #getVertices(DelegateTree)}
     * @param tree дерево
     * @return список висячих вершин
     */
    public static List<Vertex> getLeaves(DelegateTree<Vertex, Edge> tree) {
        return getVertices(tree)
                .stream()
                .filter(tree::isLeaf)
                .collect(Collectors.toList());
    }

    /**
     * Устанавливает количество потомков для каждого узла при построении графа
     * в методе {@link #createSingleTree()} в зависимости от режима {@link #buildMode}.
     * Гарантированно возвращает значение больше 0 для первых {@link #MIN_VERTEX_AMOUNT} вершин,
     * благодаря чему создаваемое дерево точно не сможет оборваться и будет валидным
     * @param vertexNumber порядковый номер вершины
     * @return случайное или фиксированное значение от 0 до m
     */
    private int setChildrenCount(int vertexNumber) {
        return buildMode.equals(BuildMode.FIXED)
                ? (vertexNumber <= edgesCount
                        ? edgesCount - 2
                        : edgesCount - 1)
                : (vertexNumber < MIN_VERTEX_AMOUNT
                        ? random.nextInt(edgesCount - 1) + 1
                        : random.nextInt(edgesCount));
    }

    /**
     * Добавляет потомка при построении графа в методе {@link #createSingleTree()}
     * @param tree дерево
     * @param children вершина-потомок
     * @param parent вершина-родитель
     * @param parentNumber порядковый номер вершины-родителя
     * @param vertexNumber порядковый номер вершины-потомка
     * @return вершина-потомок
     */
    private Vertex addVertexTo(DelegateTree<Vertex, Edge> tree, List<Vertex> children,
                               Vertex parent, int parentNumber, int vertexNumber) {
        Vertex child = new Vertex(parentNumber + 1, vertexNumber);
        tree.addChild(new Edge(), parent, child, EdgeType.DIRECTED);
        children.add(child);
        return child;
     }

    /**
     * Создает дерево, добавляя в него вершины до тех пор, пока не будет достигнут
     * предел {@link #vertexLimit} согласно правилу остановки. В циклах используются
     * буферные коллекции для привязки ссылок вершин-родителей и детей друг к другу
     * @return объект этого же класса для цепочного вызова методов
     */
    private GraphBuilder createSingleTree() {
        DelegateTree<Vertex, Edge> tree = new DelegateTree<>();
        List<Vertex> parents = new ArrayList<>();
        List<Vertex> children = new ArrayList<>();

        int parentNumber = 0;
        int vertexNumber = INIT_ROOT_AMOUNT;

        Vertex root = new Vertex(parentNumber, vertexNumber);
        tree.addVertex(root);
        parents.add(root);
        addVertexTo(tree, children, root, parentNumber, ++vertexNumber);
        while(vertexNumber <= vertexLimit) {
            for (Vertex parent : parents) {
                for (int i = 0; i < setChildrenCount(vertexNumber); i++) {
                    vertexNumber++;
                    addVertexTo(tree, children, parent, parentNumber, vertexNumber);
                }
                parentNumber++;
            }
            parents.clear();
            parents.addAll(children);
            if(parents.size() == 0) {
                parents.add(addVertexTo(tree, children,
                        new ArrayList<>(tree.getVertices()).get(vertexNumber - 1), parentNumber, vertexNumber));
            }
            children.clear();
        }
        trees.add(tree);
        return this;
    }

    /**
     * Создает случайный граф (дерево)
     * @see #createSingleTree()
     * @return объект этого же класса для цепочного вызова методов
     */
    public GraphBuilder createRandomTree() {
        buildMode = BuildMode.RANDOM;
        return createSingleTree();
    }

    /**
     * Создает детерминированный граф (дерево)
     * @see #createSingleTree()
     * @return объект этого же класса для цепочного вызова методов
     */
    public GraphBuilder createDeterminatedTree() {
        buildMode = BuildMode.FIXED;
        return createSingleTree();
    }

    /**
     * Создает множество случайных графов (деревьев)
     * @see #run() - многопоточная реализация данного метода
     * @return объект этого же класса для цепочного вызова методов
     */
    public GraphBuilder createManyTrees() {
        buildMode = BuildMode.RANDOM;
        ExecutorService ex = Executors.newCachedThreadPool();
        ex.execute(this);
        return this;
    }

    /**
     * Обеспечивает безопасное взаимодействие со списком {@link #trees},
     * предотвращая ситуацию с обращением к дереву, которое еще не создано
     * @return последнее дерево из списка {@link #trees}
     */
    private DelegateTree<Vertex, Edge> ensureSafeTreeUse() {
        if(trees.isEmpty()) {
            createSingleTree();
        }
        return trees.get(trees.size() - 1);
    }

    /**
     * Отправляет данные дерева в Google Sheets, а оттуда - напрямую в отчет
     * @param taskTitle уникальная часть заголовка листа таблицы
     * @return объект этого же класса для цепочного вызова методов
     */
    private GraphBuilder exportSingleTreeData(String taskTitle) {
        DelegateTree<Vertex, Edge> tree = ensureSafeTreeUse();
        DataExporter.writeTo( taskTitle + " - Параметры", DataExporter.wrapParametersFor(tree));
        DataExporter.writeTo(taskTitle + " - Все вершины",
                DataExporter.wrapVertexData("Таблица всех вершин", tree));
        DataExporter.writeTo(taskTitle + " - Вис. вершины",
                DataExporter.wrapVertexData("Таблица висячих вершин", tree));
        return this;
    }

    /**
     * Отправляет данные по второй лабораторной в Google Sheets
     * @return объект этого же класса для цепочного вызова методов
     */
    public GraphBuilder exportTask2ToGoogleSheets() {
        return exportSingleTreeData("Лаб.2");
    }

    /**
     * Отправляет данные по третьей лабораторной в Google Sheets
     * @return объект этого же класса для цепочного вызова методов
     */
    public GraphBuilder exportTask3ToGoogleSheets() {
        return exportSingleTreeData("Лаб.3");
    }

    /**
     * Отправляет данные по четвертой лабораторной в Google Sheets
     * @return объект этого же класса для цепочного вызова методов
     */
    public GraphBuilder exportTask4ToGoogleSheets() {
        DataExporter.writeTo("Лаб.4 - Параметры", DataExporter.wrapParametersFor(trees));
        DataExporter.writeTo("Лаб.4 - Графы", DataExporter.wrapTreeData(trees));
        return this;
    }

    /**
     * Визуализирует граф
     * @return объект этого же класса для цепочного вызова методов
     */
    public GraphBuilder visualize() {
        DelegateTree<Vertex, Edge> tree = ensureSafeTreeUse();
        Layout<Vertex, Edge> layout = new TreeLayout<>(tree);

        BasicVisualizationServer<Vertex, Edge> vs = new BasicVisualizationServer<>(layout);
        vs.getRenderContext().setVertexFillPaintTransformer(v -> tree.isLeaf(v) ? Color.ORANGE : Color.GREEN);
        vs.getRenderContext().setVertexShapeTransformer(v -> new Ellipse2D.Double(-10, -10, 35, 35));
        vs.getRenderContext().setEdgeStrokeTransformer(s -> new BasicStroke());
        vs.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        vs.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);

        showInWindow(1920, 1080, vs);
        return this;
    }

    /**
     * Показывает граф в окне
     * @param width ширина окна
     * @param height высота окна
     * @param vs обертка для рендеринга графа
     */
    private void showInWindow(int width, int height, BasicVisualizationServer<Vertex, Edge> vs) {
        vs.setPreferredSize(new Dimension(width, height));
        JFrame frame = new JFrame("Графическое построение дерева");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(vs);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Завершает выполнение программы, когда все окна закрываются
     */
    public void waitForWindowClosing() {
        final long TIMEOUT = 1000;
        while (Arrays.stream(JFrame.getFrames()).anyMatch(Window::isActive)) {
            try {
                Thread.sleep(TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.exit(0);
    }

    /**
     * Многопоточная реализация метода {@link #createManyTrees()}
     */
    @Override
    public void run() {
        int currentSize = trees.size();
        for(int i = 0; i < graphsCount - currentSize; i++) {
            createSingleTree();
        }
    }

    /**
     * Запускает {@link InnerBuilder}
     * @return экземпляр вложенного класса
     */
    public static InnerBuilder deploy() {
        return new GraphBuilder().new InnerBuilder();
    }

    /**
     * Вложенный класс - реализация паттерна "Builder" ("Строитель")
     * Содержит сеттеры для полей внешнего класса
     */
    @SuppressWarnings("InnerClassMayBeStatic")
    public class InnerBuilder {

        private InnerBuilder() { }

        public InnerBuilder setBuildMode(BuildMode buildMode) {
            GraphBuilder.this.buildMode = Objects.requireNonNull(buildMode, nullMessage());
            return this;
        }

        public InnerBuilder setGraphsCount(int graphsCount) {
            GraphBuilder.this.graphsCount = graphsCount;
            return this;
        }

        public InnerBuilder setEdgesCount(int edgesCount) {
            GraphBuilder.this.edgesCount = edgesCount;
            return this;
        }

        public InnerBuilder setVertexLimit(int vertexLimit) {
            GraphBuilder.this.vertexLimit = vertexLimit;
            return this;
        }

        /**
         * Окончательно инициализирует объект класса {@link GraphBuilder}
         * @return экземпляр внешнего класса
         */
        public GraphBuilder init() {
            fillNullFields(GraphBuilder.this);
            if(Objects.isNull(buildMode)) {
                buildMode = BuildMode.RANDOM;
            }
            GraphBuilder.this.trees = new ArrayList<>();
            GraphBuilder.this.random = new Random();
            return GraphBuilder.this;
        }
    }
}
