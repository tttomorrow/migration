package org.opengauss.portalcontroller.check;

import org.opengauss.portalcontroller.*;
import org.opengauss.portalcontroller.constant.Check;
import org.opengauss.portalcontroller.constant.Command;
import org.opengauss.portalcontroller.constant.Debezium;
import org.opengauss.portalcontroller.constant.Method;
import org.opengauss.portalcontroller.constant.MigrationParameters;
import org.opengauss.portalcontroller.constant.Status;
import org.opengauss.portalcontroller.software.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import static org.opengauss.portalcontroller.Plan.runningTaskList;

/**
 * The type Check task reverse datacheck.
 */
public class CheckTaskReverseDatacheck implements CheckTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckTaskReverseDatacheck.class);

    @Override
    public boolean installAllPackages(boolean download) {
        ArrayList<Software> softwareArrayList = new ArrayList<>();
        softwareArrayList.add(new Kafka());
        softwareArrayList.add(new Confluent());
        softwareArrayList.add(new Datacheck());
        boolean flag = InstallMigrationTools.installMigrationTools(softwareArrayList,download);
        return flag;
    }

    /**
     * Install datacheck package.
     */
    @Override
    public boolean installAllPackages() {
        CheckTask checkTask = new CheckTaskReverseDatacheck();
        boolean flag = InstallMigrationTools.installSingleMigrationTool(checkTask,MigrationParameters.Install.CHECK);
        return flag;
    }

    /**
     * Copy datacheck config files.
     */
    @Override
    public void copyConfigFiles(String workspaceId) {

    }

    /**
     * Change datacheck parameters.
     */
    @Override
    public void changeParameters(String workspaceId) {
        Hashtable<String, String> hashtable = PortalControl.toolsConfigParametersTable;
        String kafkaPath = hashtable.get(Debezium.Kafka.PATH);
        Tools.changeSinglePropertiesParameter("dataDir", PortalControl.portalControlPath + "tmp/zookeeper", kafkaPath + "config/zookeeper.properties");
        Tools.changeSinglePropertiesParameter("log.dirs", PortalControl.portalControlPath + "tmp/kafka-logs", kafkaPath + "config/server.properties");
        Tools.changeMigrationDatacheckParameters(PortalControl.toolsMigrationParametersTable);
        Tools.changeSingleYmlParameter("spring.extract.debezium-enable", true, PortalControl.portalWorkSpacePath + "config/datacheck/application-source.yml");
        String sourceTopic = Tools.getSinglePropertiesParameter("transforms.route.replacement", PortalControl.portalWorkSpacePath + "config/debezium/opengauss-source.properties");
        Tools.changeSingleYmlParameter("spring.extract.debezium-topic", sourceTopic, PortalControl.portalWorkSpacePath + "config/datacheck/application-source.yml");
        String sinkTopic = Tools.getSinglePropertiesParameter("transforms.route.replacement", PortalControl.portalWorkSpacePath + "config/debezium/opengauss-sink.properties");
        Tools.changeSingleYmlParameter("spring.extract.debezium-topic", sinkTopic, PortalControl.portalWorkSpacePath + "config/datacheck/application-sink.yml");
    }

    @Override
    public void prepareWork(String workspaceId) {
        runningTaskList.add(Command.Start.Mysql.FULL_CHECK);
        Task.startTaskMethod(Method.Run.ZOOKEEPER, 8000);
        Task.startTaskMethod(Method.Run.KAFKA, 8000);
        Task.startTaskMethod(Method.Run.REGISTRY, 8000);
        changeParameters(workspaceId);
        if (!checkNecessaryProcessExist()) {
            LOGGER.error("There is no kafka running.Reverse datacheck failed.");
        }
    }

    @Override
    public void start(String workspaceId) {
        Task.startTaskMethod(Method.Run.CHECK_SOURCE, 5000);
        Task.startTaskMethod(Method.Run.CHECK_SINK, 5000);
        Task.startTaskMethod(Method.Run.CHECK, 5000);
        checkEnd();
    }

    /**
     * Check necessary process exist boolean.
     *
     * @return the boolean
     */
    public boolean checkNecessaryProcessExist() {
        boolean flag = false;
        boolean flag1 = Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.ZOOKEEPER)) != -1;
        boolean flag2 = Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.KAFKA)) != -1;
        flag = flag1 && flag2;
        boolean flag3 = Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.REGISTRY)) != -1;
        flag = flag && flag3;
        return flag;
    }

    public void checkEnd() {
        while (!Plan.stopPlan && !Plan.stopReverseMigration) {
            try {
                LOGGER.info("Reverse migration is running...");
                LOGGER.info("Reverse migration datacheck is running...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted exception occurred in running reverse migraiton datacheck.");
            }
        }
        if (Plan.stopReverseMigration) {
            if (PortalControl.status != Status.ERROR) {
                PortalControl.status = Status.REVERSE_MIGRATION_FINISHED;
            }
            Task.stopTaskMethod(Method.Run.CHECK);
            Task.stopTaskMethod(Method.Run.CHECK_SINK);
            Task.stopTaskMethod(Method.Run.CHECK_SOURCE);
            Task.stopTaskMethod(Method.Run.REVERSE_CONNECT_SINK);
            Task.stopTaskMethod(Method.Run.REVERSE_CONNECT_SOURCE);
            LOGGER.info("Reverse migration stopped.");
        }
    }

    public void uninstall(){
        String errorPath = PortalControl.portalControlPath + "logs/error.log";
        ArrayList<String> filePaths = new ArrayList<>();
        filePaths.add(PortalControl.toolsConfigParametersTable.get(Debezium.PATH));
        filePaths.add(PortalControl.portalControlPath + "tmp/kafka-logs");
        filePaths.add(PortalControl.portalControlPath + "tmp/zookeeper");
        filePaths.add(PortalControl.toolsConfigParametersTable.get(Check.PATH));
        InstallMigrationTools.removeSingleMigrationToolFiles(filePaths,errorPath);
    }
}
