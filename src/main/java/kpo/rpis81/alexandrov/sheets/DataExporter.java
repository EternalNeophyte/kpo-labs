package kpo.rpis81.alexandrov.sheets;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import edu.uci.ics.jung.graph.DelegateTree;
import kpo.rpis81.alexandrov.elements.Edge;
import kpo.rpis81.alexandrov.elements.Vertex;
import kpo.rpis81.alexandrov.tools.GraphBuilder;
import kpo.rpis81.alexandrov.tools.GraphMath;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * Класс для интеграции с Google Sheets API. Содержит служебные константы,
 * необходимые для прохождения авторизации OAuth
 * @author Илья Александров
 */
public class DataExporter {

    private static final String APPLICATION_NAME = "kpo-labs";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String SPREADSHEET_ID = "1_4PAyDYbGfb7524Nv46vueORUV8i57kEYLSB1xjXQ6Q";
    private static final String TOKENS_DIRECTORY_PATH = "/google-sheet-client.json";
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String DEFAULT_CELLS = "!A1:G";

    /**
     * Создает объект для авторизации в Google Sheets API
     * @param HTTP_TRANSPORT сетевой HTTP-транспорт
     * @return объект с данными авторизации
     * @throws IOException если не найден файл credentials.json
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream in = DataExporter.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Создает сервис Google Sheets
     * @return объект сервиса
     * @throws GeneralSecurityException если появляется ошибка авторизации
     * @throws IOException если возникает ошибка при чтении файлов .json или при соединении с сервером
     */
    private static Sheets getSheetsService() throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Записывает данные в Google-таблицу
     * @param sheetTitle заголовок листа в документе
     * @param cells диапазон записи. Обозначается с помощью ячеек
     * @param data данные, из которых формируется таблица
     */
    public static void writeTo(String sheetTitle, String cells, List<List<Object>> data) {
        try {
            String range = sheetTitle + cells;
            ValueRange body = new ValueRange().setValues(data);
            Sheets service = getSheetsService();
            service.spreadsheets()
                    .values()
                    .clear(SPREADSHEET_ID, range, new ClearValuesRequest())
                    .execute();
            service.spreadsheets()
                    .values()
                    .update(SPREADSHEET_ID, range, body)
                    .setValueInputOption("RAW")
                    .execute();
        }
        catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Записывает данные в диапазон по умолчанию
     * @see #writeTo(String, String, List)
     */
    public static void writeTo(String sheetTitle, List<List<Object>> data) {
        writeTo(sheetTitle, DEFAULT_CELLS, data);
    }

    /**
     * Извлекает набор данных из дерева для представления в виде таблицы
     * @param header заголовок создаваемой таблицы
     * @param tree дерево
     * @return набор данных
     */
    public static List<List<Object>> wrapVertexData(String header, DelegateTree<Vertex, Edge> tree) {
        List<Vertex> vertices = header.contains("висячих")
                ? GraphBuilder.getLeaves(tree) : GraphBuilder.getVertices(tree);
        List<List<Object>> data = new ArrayList<>(List.of(
                List.of(header),
                List.of("№ вершины", "№ родителя")
        ));
        vertices.forEach(v -> data.add(List.of(v.getNumber(), v.getParent())));
        return data;
    }

    /**
     * Извлекает набор данных из списка деревьев для представления в виде таблицы
     * @param trees список деревьев
     * @return набор данных
     */
    public static List<List<Object>> wrapTreeData(List<DelegateTree<Vertex, Edge>> trees) {
        List<List<Object>> data = new ArrayList<>(List.of(
                List.of("Таблица сгенерированных случайных графов"),
                List.of("№ графа", "Кол-во вершин", "Кол-во вис. вершин", "Высота", "Альфа")
        ));
        trees.forEach(t -> data.add(List.of(trees.indexOf(t) + 1, t.getVertexCount(), GraphMath.leavesCount(t),
                t.getHeight(), GraphMath.alpha(t))));
        return data;
    }

    /**
     * Выделяет общие параметры дерева для представления в виде таблицы
     * @param tree дерево
     * @return набор параметров
     */
    public static List<List<Object>> wrapParametersFor(DelegateTree<Vertex, Edge> tree) {
        return new ArrayList<>(List.of(
                List.of("Параметры сгенерированного графа"),
                List.of("Высота", tree.getHeight()),
                List.of("Общее кол-во вершин", tree.getVertexCount()),
                List.of("Кол-во висячих вершин", GraphMath.leavesCount(tree)),
                List.of("Альфа", GraphMath.alpha(tree)),
                List.of("Мат. ожидание исходящих ребер", GraphMath.meanForEdges(tree))
        ));
    }

    /**
     * Выделяет общие параметры совокупности деревьев для представления в виде таблицы
     * @param trees список деревьев
     * @return набор параметров
     */
    public static List<List<Object>> wrapParametersFor(List<DelegateTree<Vertex, Edge>> trees) {
        return new ArrayList<>(List.of(
                List.of("Параметры сгенерированных случайных графов"),
                List.of("Среднее кол-во вершин", GraphMath.averageVertexCount(trees)),
                List.of("Среднее кол-во висячих вершин", GraphMath.averageLeavesCount(trees)),
                List.of("Средняя высота", GraphMath.averageHeight(trees)),
                List.of("Среднее значение альфа", GraphMath.averageAlpha(trees)),
                List.of("Дисперсия вершин", GraphMath.varianceForVertices(trees)),
                List.of("Дисперсия висячих вершин", GraphMath.varianceForLeaves(trees)),
                List.of("Дисперсия высоты", GraphMath.varianceForHeight(trees)),
                List.of("Дисперсия альфа", GraphMath.varianceForAlpha(trees)),
                List.of("Мат. ожидание альфа", GraphMath.meanForAlpha(trees))
        ));
    }

    /**
     * Оборачивает данные из
     * @param map в
     * @return список типа List<List<Object>>
     */
    public static List<List<Object>> wrapParametersFor(HashMap<String, BigDecimal> map) {
        List<List<Object>> data = new ArrayList<>();
        map.forEach((k, v) -> data.add(List.of(k, v)));
        return data;
    }
}