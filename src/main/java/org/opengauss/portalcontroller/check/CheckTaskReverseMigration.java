package org.opengauss.portalcontroller.check;

import org.opengauss.portalcontroller.*;
import org.opengauss.portalcontroller.constant.Debezium;
import org.opengauss.portalcontroller.constant.Method;
import org.opengauss.portalcontroller.constant.MigrationParameters;
import org.opengauss.portalcontroller.constant.Parameter;
import org.opengauss.portalcontroller.constant.StartPort;
import org.opengauss.portalcontroller.constant.Status;
import org.opengauss.portalcontroller.software.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Hashtable;

import static org.opengauss.portalcontroller.PortalControl.portalWorkSpacePath;

/**
 * The type Check task reverse migration.
 */
public class CheckTaskReverseMigration implements CheckTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckTaskReverseMigration.class);

    @Override
    public boolean installAllPackages(boolean download) {
        ArrayList<Software> softwareArrayList = new ArrayList<>();
        softwareArrayList.add(new Kafka());
        softwareArrayList.add(new Confluent());
        softwareArrayList.add(new ConnectorOpengauss());
        boolean flag = InstallMigrationTools.installMigrationTools(softwareArrayList, download);
        return flag;
    }

    /**
     * Install incremental migration tools package.
     */
    @Override
    public boolean installAllPackages() {
        CheckTask checkTask = new CheckTaskReverseMigration();
        boolean flag = InstallMigrationTools.installSingleMigrationTool(checkTask, MigrationParameters.Install.REVERSE_MIGRATION);
        return flag;
    }

    /**
     * Copy incremental migration tools files.
     */
    public void copyConfigFiles(String workspaceId) {

    }

    /**
     * Change incremental migration tools parameters.
     */
    @Override
    public void changeParameters(String workspaceId) {
        Hashtable<String, String> hashtable = PortalControl.toolsConfigParametersTable;
        String kafkaPath = hashtable.get(Debezium.Kafka.PATH);
        Tools.changeSinglePropertiesParameter("dataDir", PortalControl.portalControlPath + "tmp/zookeeper", kafkaPath + "config/zookeeper.properties");
        Tools.changeSinglePropertiesParameter("log.dirs", PortalControl.portalControlPath + "tmp/kafka-logs", kafkaPath + "config/server.properties");
        Tools.changeReverseMigrationParameters(PortalControl.toolsMigrationParametersTable);
        String sourceConfigPath = PortalControl.portalWorkSpacePath + "config/debezium/opengauss-source.properties";
        String sinkConfigPath = PortalControl.portalWorkSpacePath + "config/debezium/opengauss-sink.properties";
        Hashtable<String, String> hashtable1 = new Hashtable<>();
        hashtable1.put("database.server.name", "opengauss_server_" + workspaceId);
        hashtable1.put("database.history.kafka.topic", "opengauss_server_" + workspaceId + "_history");
        hashtable1.put("transforms.route.regex", "^" + "opengauss_server_" + workspaceId + "(.*)");
        hashtable1.put("transforms.route.replacement", "opengauss_server_" + workspaceId + "_topic");
        hashtable1.put("file.path", portalWorkSpacePath + "status/reverse");
        hashtable1.put("slot.name", "slot_" + workspaceId);
        Tools.changePropertiesParameters(hashtable1, sourceConfigPath);
        Hashtable<String, String> hashtable2 = new Hashtable<>();
        hashtable2.put("topics", "opengauss_server_" + workspaceId + "_topic");
        hashtable2.put("file.path", portalWorkSpacePath + "status/reverse");
        Tools.changePropertiesParameters(hashtable2, sinkConfigPath);
    }

    @Override
    public void prepareWork(String workspaceId) {
        if (PortalControl.status != Status.ERROR) {
            PortalControl.status = Status.START_REVERSE_MIGRATION;
        }
        Tools.changeIncrementalMigrationParameters(PortalControl.toolsMigrationParametersTable);
        changeParameters(workspaceId);
        if (!checkNecessaryProcessExist()) {
            Task.startTaskMethod(Method.Run.ZOOKEEPER, 8000);
            Task.startTaskMethod(Method.Run.KAFKA, 8000);
            Task.startTaskMethod(Method.Run.REGISTRY, 8000);
        }
    }

    @Override
    public void start(String workspaceId) {
        if (checkAnotherConnectExists()) {
            LOGGER.error("Another connector is running.Cannot run reverse migration with workspaceId is " + workspaceId + " .");
            return;
        }
        int sourcePort = StartPort.REST_OPENGAUSS_SOURCE + PortalControl.portId * 10;
        int port = Tools.getAvailablePorts(sourcePort, 1, 1000).get(0);
        Tools.changeSinglePropertiesParameter("rest.port", String.valueOf(port), PortalControl.portalWorkSpacePath + "config/debezium/connect-avro-standalone-reverse-source.properties");
        String confluentPath = PortalControl.toolsConfigParametersTable.get(Debezium.Confluent.PATH);
        Tools.changeConnectXmlFile(workspaceId + "_reverse", confluentPath + "etc/kafka/connect-log4j.properties");
        Task.startTaskMethod(Method.Run.REVERSE_CONNECT_SOURCE, 8000);
        int sinkPort = StartPort.REST_OPENGAUSS_SINK + PortalControl.portId * 10;
        int port2 = Tools.getAvailablePorts(sinkPort, 1, 1000).get(0);
        Tools.changeSinglePropertiesParameter("rest.port", String.valueOf(port2), PortalControl.portalWorkSpacePath + "config/debezium/connect-avro-standalone-reverse-sink.properties");
        Tools.changeConnectXmlFile(workspaceId + "_reverse", confluentPath + "etc/kafka/connect-log4j.properties");
        Task.startTaskMethod(Method.Run.REVERSE_CONNECT_SINK, 8000);
        if (PortalControl.status != Status.ERROR) {
            PortalControl.status = Status.RUNNING_REVERSE_MIGRATION;
        }
        checkEnd();
    }

    /**
     * Check another connect exists boolean.
     *
     * @return the boolean
     */
    public boolean checkAnotherConnectExists() {
        boolean flag = false;
        boolean flag1 = Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.REVERSE_CONNECT_SOURCE)) != -1;
        boolean flag2 = Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.REVERSE_CONNECT_SINK)) != -1;
        boolean flag3 = Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.CONNECT_SOURCE)) != -1;
        boolean flag4 = Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.CONNECT_SINK)) != -1;
        flag = flag1 || flag2 || flag3 || flag4;
        return flag;
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
        boolean flag3 = Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.REGISTRY)) != -1;
        flag = flag1 && flag2 && flag3;
        return flag;
    }

    @Override
    public void checkEnd() {
        while (!Plan.stopPlan && !Plan.stopReverseMigration && !PortalControl.taskList.contains("start mysql reverse migration datacheck")) {
            LOGGER.info("Reverse migration is running...");
            Tools.sleepThread(1000, "running reverse migraiton");
        }
        if (Plan.stopReverseMigration) {
            if (PortalControl.status != Status.ERROR) {
                PortalControl.status = Status.REVERSE_MIGRATION_FINISHED;
                Plan.pause = true;
                Tools.sleepThread(50, "pausing the plan");
            }
            Task.stopTaskMethod(Method.Run.REVERSE_CONNECT_SINK);
            Task.stopTaskMethod(Method.Run.REVERSE_CONNECT_SOURCE);
            LOGGER.info("Reverse migration stopped.");
        }
    }

    public void uninstall() {
        String errorPath = PortalControl.portalControlPath + "logs/error.log";
        ArrayList<String> filePaths = new ArrayList<>();
        filePaths.add(PortalControl.toolsConfigParametersTable.get(Debezium.PATH));
        filePaths.add(PortalControl.portalControlPath + "tmp/kafka-logs");
        filePaths.add(PortalControl.portalControlPath + "tmp/zookeeper");
        InstallMigrationTools.removeSingleMigrationToolFiles(filePaths, errorPath);
    }
}
