package org.opengauss.portalcontroller.check;

import org.opengauss.portalcontroller.*;
import org.opengauss.portalcontroller.constant.Debezium;
import org.opengauss.portalcontroller.constant.Method;
import org.opengauss.portalcontroller.constant.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Hashtable;

import static org.opengauss.portalcontroller.PortalControl.portalWorkSpacePath;

public class CheckTaskIncrementalMigration implements CheckTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckTaskIncrementalMigration.class);

    /**
     * Install incremental migration tools package.
     */
    @Override
    public void installAllPackages() {
        Hashtable<String, String> hashtable = PortalControl.toolsConfigParametersTable;
        String debeziumPath = hashtable.get(Debezium.PATH);
        String connectorPath = hashtable.get(Debezium.Connector.PATH);
        Tools.installPackage(PortalControl.toolsConfigParametersTable.get(Debezium.Kafka.PATH) + "libs/kafka-streams-examples-3.2.3.jar", Debezium.PKG_PATH, Debezium.Kafka.PKG_NAME, debeziumPath);
        Tools.installPackage(PortalControl.toolsConfigParametersTable.get(Debezium.Confluent.PATH) + "etc/kafka/consumer.properties", Debezium.PKG_PATH, Debezium.Confluent.PKG_NAME, debeziumPath);
        Tools.installPackage(PortalControl.toolsConfigParametersTable.get(Debezium.Connector.PATH) + "debezium-connector-mysql/debezium-connector-mysql-1.8.1.Final.jar", Debezium.PKG_PATH, Debezium.Connector.MYSQL_PKG_NAME, connectorPath);
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
        String sourceConfigPath = PortalControl.portalWorkSpacePath + "config/debezium/mysql-source.properties";
        String sinkConfigPath = PortalControl.portalWorkSpacePath + "config/debezium/mysql-sink.properties";
        Hashtable<String, String> hashtable1 = new Hashtable<>();
        hashtable1.put("name", "mysql-source-" + workspaceId);
        hashtable1.put("database.server.name", "mysql_server_" + workspaceId);
        hashtable1.put("database.history.kafka.topic", "mysql_server_" + workspaceId + "_history");
        hashtable1.put("transforms.route.regex", "^" + "mysql_server_" + workspaceId + "(.*)");
        hashtable1.put("transforms.route.replacement", "mysql_server_" + workspaceId + "_topic");
        hashtable1.put("file.path", portalWorkSpacePath + "status/incremental");
        Tools.changePropertiesParameters(hashtable1, sourceConfigPath);
        Hashtable<String, String> hashtable2 = new Hashtable<>();
        hashtable2.put("name", "mysql-sink-" + workspaceId);
        hashtable2.put("topics", "mysql_server_" + workspaceId + "_topic");
        hashtable2.put("file.path", portalWorkSpacePath + "status/incremental");
        Tools.changePropertiesParameters(hashtable2, sinkConfigPath);
    }

    @Override
    public void prepareWork(String workspaceId) {
        Tools.changeIncrementalMigrationParameters(PortalControl.toolsMigrationParametersTable, workspaceId);
        changeParameters(workspaceId);
        if (!checkNecessaryProcessExist()) {
            Task.startTaskMethod(Method.Run.ZOOKEEPER, 8000);
            Task.startTaskMethod(Method.Run.KAFKA, 8000);
            Task.startTaskMethod(Method.Run.REGISTRY, 8000);
        }
        if (checkAnotherConnectExists()) {
            LOGGER.error("Another connector is running.Cannot run incremental migration whose workspace id is " + workspaceId + " .");
            return;
        }
        Tools.findOffset(workspaceId);
        String confluentPath = PortalControl.toolsConfigParametersTable.get(Debezium.Confluent.PATH);
        Tools.changeConnectXmlFile(workspaceId, confluentPath + "etc/kafka/connect-log4j.properties");
        String standaloneSourcePath = PortalControl.portalWorkSpacePath + "config/debezium/connect-avro-standalone-source.properties";
        int port = Tools.getAvailablePorts(1, 1000).get(0);
        Tools.changeSinglePropertiesParameter("rest.port", String.valueOf(port), standaloneSourcePath);
        Task.startTaskMethod(Method.Run.CONNECT_SOURCE, 8000);
    }

    @Override
    public void start(String workspaceId) {
        PortalControl.status = Status.START_INCREMENTAL_MIGRATION;
        String standaloneSinkFilePath = PortalControl.portalWorkSpacePath + "config/debezium/connect-avro-standalone-sink.properties";
        String confluentPath = PortalControl.toolsConfigParametersTable.get(Debezium.Confluent.PATH);
        Tools.changeConnectXmlFile(workspaceId, confluentPath + "etc/kafka/connect-log4j.properties");
        int port = Tools.getAvailablePorts(1, 1000).get(0);
        Tools.changeSinglePropertiesParameter("rest.port", String.valueOf(port), standaloneSinkFilePath);
        Task.startTaskMethod(Method.Run.CONNECT_SINK, 8000);
        PortalControl.status = Status.RUNNING_INCREMENTAL_MIGRATION;
        checkEnd();
    }

    @Override
    public void checkEnd() {
        while (!Plan.stopPlan && !Plan.stopIncrementalMigration && !PortalControl.taskList.contains("start mysql incremental migration datacheck")) {
            try {
                LOGGER.info("Incremental migration is running...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted exception occurred in running incremental migraiton.");
            }
        }
        if (Plan.stopIncrementalMigration) {
            Task task = new Task();
            PortalControl.status = Status.INCREMENTAL_MIGRATION_FINISHED;
            task.stopTaskMethod(Method.Run.CONNECT_SINK);
            task.stopTaskMethod(Method.Run.CONNECT_SOURCE);
            LOGGER.info("Incremental migration stopped.");
        }
    }

    public boolean checkAnotherConnectExists() {
        boolean flag = false;
        boolean flag1 = Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.REVERSE_CONNECT_SOURCE)) != -1;
        boolean flag2 = Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.REVERSE_CONNECT_SINK)) != -1;
        boolean flag3 = Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.CONNECT_SOURCE)) != -1;
        boolean flag4 = Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.CONNECT_SINK)) != -1;
        flag = flag1 || flag2 || flag3 || flag4;
        return flag;
    }


    public boolean checkNecessaryProcessExist() {
        boolean flag = false;
        boolean flag1 = Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.ZOOKEEPER)) != -1;
        boolean flag2 = Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.KAFKA)) != -1;
        boolean flag3 = Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.REGISTRY)) != -1;
        flag = flag1 && flag2 && flag3;
        return flag;
    }
}
