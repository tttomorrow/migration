package org.opengauss.portalcontroller.check;

import org.opengauss.portalcontroller.*;
import org.opengauss.portalcontroller.constant.Debezium;
import org.opengauss.portalcontroller.constant.Method;
import org.opengauss.portalcontroller.constant.MigrationParameters;
import org.opengauss.portalcontroller.constant.StartPort;
import org.opengauss.portalcontroller.constant.Status;
import org.opengauss.portalcontroller.software.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Hashtable;

import static org.opengauss.portalcontroller.PortalControl.portalWorkSpacePath;

/**
 * The type Check task incremental migration.
 */
public class CheckTaskIncrementalMigration implements CheckTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckTaskIncrementalMigration.class);

    @Override
    public boolean installAllPackages(boolean download) {
        ArrayList<Software> softwareArrayList = new ArrayList<>();
        softwareArrayList.add(new Kafka());
        softwareArrayList.add(new Confluent());
        softwareArrayList.add(new ConnectorMysql());
        boolean flag = InstallMigrationTools.installMigrationTools(softwareArrayList, download);
        return flag;
    }

    /**
     * Install incremental migration tools package.
     */
    @Override
    public boolean installAllPackages() {
        CheckTask checkTask = new CheckTaskIncrementalMigration();
        boolean flag = InstallMigrationTools.installSingleMigrationTool(checkTask, MigrationParameters.Install.INCREMENTAL_MIGRATION);
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
        Tools.changeSinglePropertiesParameter("zookeeper.connection.timeout.ms", "30000", kafkaPath + "config/server.properties");
        Tools.changeSinglePropertiesParameter("zookeeper.session.timeout.ms", "30000", kafkaPath + "config/server.properties");
        String sourceConfigPath = PortalControl.portalWorkSpacePath + "config/debezium/mysql-source.properties";
        String sinkConfigPath = PortalControl.portalWorkSpacePath + "config/debezium/mysql-sink.properties";
        Hashtable<String, String> hashtable1 = new Hashtable<>();
        hashtable1.put("name", "mysql-source-" + workspaceId);
        hashtable1.put("database.server.name", "mysql_server_" + workspaceId);
        hashtable1.put("database.history.kafka.topic", "mysql_server_" + workspaceId + "_history");
        hashtable1.put("transforms.route.regex", "^" + "mysql_server_" + workspaceId + "(.*)");
        hashtable1.put("transforms.route.replacement", "mysql_server_" + workspaceId + "_topic");
        hashtable1.put("source.process.file.path", portalWorkSpacePath + "status/incremental");
        Tools.changePropertiesParameters(hashtable1, sourceConfigPath);
        Hashtable<String, String> hashtable2 = new Hashtable<>();
        hashtable2.put("name", "mysql-sink-" + workspaceId);
        hashtable2.put("topics", "mysql_server_" + workspaceId + "_topic");
        hashtable2.put("sink.process.file.path", portalWorkSpacePath + "status/incremental");
        hashtable2.put("xlog.location", portalWorkSpacePath + "status/incremental/xlog.txt");
        Tools.changePropertiesParameters(hashtable2, sinkConfigPath);
    }

    @Override
    public void prepareWork(String workspaceId) {
        Tools.changeIncrementalMigrationParameters(PortalControl.toolsMigrationParametersTable);
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
        Tools.findOffset();
        String confluentPath = PortalControl.toolsConfigParametersTable.get(Debezium.Confluent.PATH);
        Tools.changeConnectXmlFile(workspaceId + "_source", confluentPath + "etc/kafka/connect-log4j.properties");
        String standaloneSourcePath = PortalControl.portalWorkSpacePath + "config/debezium/connect-avro-standalone-source.properties";
        int sourcePort = StartPort.REST_MYSQL_SOURCE + PortalControl.portId * 10;
        int port = Tools.getAvailablePorts(sourcePort, 1, 1000).get(0);
        Tools.changeSinglePropertiesParameter("rest.port", String.valueOf(port), standaloneSourcePath);
        Task.startTaskMethod(Method.Run.CONNECT_SOURCE, 8000);
    }

    @Override
    public void start(String workspaceId) {
        if (PortalControl.status != Status.ERROR) {
            PortalControl.status = Status.START_INCREMENTAL_MIGRATION;
        }
        String standaloneSinkFilePath = PortalControl.portalWorkSpacePath + "config/debezium/connect-avro-standalone-sink.properties";
        String confluentPath = PortalControl.toolsConfigParametersTable.get(Debezium.Confluent.PATH);
        Tools.changeConnectXmlFile(workspaceId + "_sink", confluentPath + "etc/kafka/connect-log4j.properties");
        int sinkPort = StartPort.REST_MYSQL_SINK + PortalControl.portId * 10;
        int port = Tools.getAvailablePorts(sinkPort, 1, 1000).get(0);
        Tools.changeSinglePropertiesParameter("rest.port", String.valueOf(port), standaloneSinkFilePath);
        Task.startTaskMethod(Method.Run.CONNECT_SINK, 8000);
        if (PortalControl.status != Status.ERROR) {
            PortalControl.status = Status.RUNNING_INCREMENTAL_MIGRATION;
        }
        checkEnd();
    }

    @Override
    public void checkEnd() {
        while (!Plan.stopPlan && !Plan.stopIncrementalMigration && !PortalControl.taskList.contains("start mysql incremental migration datacheck")) {
            LOGGER.info("Incremental migration is running...");
            Tools.sleepThread(1000,"running incremental migraiton");
        }
        if (Plan.stopIncrementalMigration) {
            Task task = new Task();
            if (PortalControl.status != Status.ERROR) {
                PortalControl.status = Status.INCREMENTAL_MIGRATION_FINISHED;
                Plan.pause = true;
                Tools.sleepThread(50,"pausing the plan");
            }
            task.stopTaskMethod(Method.Run.CONNECT_SINK);
            task.stopTaskMethod(Method.Run.CONNECT_SOURCE);
            LOGGER.info("Incremental migration stopped.");
        }
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

    public void uninstall() {
        String errorPath = PortalControl.portalControlPath + "logs/error.log";
        ArrayList<String> filePaths = new ArrayList<>();
        filePaths.add(PortalControl.toolsConfigParametersTable.get(Debezium.PATH));
        filePaths.add(PortalControl.portalControlPath + "tmp/kafka-logs");
        filePaths.add(PortalControl.portalControlPath + "tmp/zookeeper");
        InstallMigrationTools.removeSingleMigrationToolFiles(filePaths, errorPath);
    }
}
