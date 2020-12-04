package kpo.rpis81.alexandrov.tools;

import kpo.rpis81.alexandrov.softwarecontrol.SoftwareReliabilityTester;

/**
 * Точка входа в программу
 * @author Илья Александров
 */
public class Runner {

    public static void main(String... args) {
        GraphBuilder.deploy()
                .setEdgesCount(4)
                .setGraphsCount(200)
                .setVertexLimit(50)
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
        SoftwareReliabilityTester.deploy()
                .addErrors(4)
                .addErrors(3)
                .addErrors(3)
                .addErrors(1)
                .addErrors(0)
                .addErrors(1)
                .addErrors(0)
                .init()
                .exportTasksToGoogleSheets();
    }
}

