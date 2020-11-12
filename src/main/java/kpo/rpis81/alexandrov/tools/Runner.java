package kpo.rpis81.alexandrov.tools;

/**
 * Точка входа в программу
 * @author Илья Александров
 */
public class Runner {

    public static void main(String... args) {

        GraphBuilder.deploy()
                .setEdgesCount(4)
                .setGraphsCount(400)
                .setVertexLimit(100)
                .init()
                .createRandomTree()
                .exportTask2ToGoogleSheets()
                .visualize()
                .createDeterminatedTree()
                .exportTask3ToGoogleSheets()
                .visualize()
                .createManyTrees()
                .exportTask4ToGoogleSheets()
                .waitForWindowClosing();
    }
}

