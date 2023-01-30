package org.opengauss.portalcontroller.check;

import org.opengauss.portalcontroller.*;
import org.opengauss.portalcontroller.constant.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class CheckTaskFullDatacheck implements CheckTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckTaskFullDatacheck.class);
    private String workspaceId = "";

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    /**
     * Install datacheck package.
     */
    @Override
    public void installAllPackages() {
        Hashtable<String, String> hashtable = PortalControl.toolsConfigParametersTable;
        Tools.installPackage(PortalControl.toolsConfigParametersTable.get(Debezium.Kafka.PATH) + "libs/kafka-streams-examples-3.2.3.jar", Debezium.PKG_PATH, Debezium.Kafka.PKG_NAME, hashtable.get(Debezium.PATH));
        Tools.installPackage(PortalControl.toolsConfigParametersTable.get(Check.PATH) + "config/log4j2.xml", Check.PKG_PATH, Check.PKG_NAME, hashtable.get(Check.INSTALL_PATH));
    }

    /**
     * Copy datacheck config files.
     */
    @Override
    public void copyConfigFiles(String workspaceId) {
    }

    @Override
    public void prepareWork(String workspaceId) {

    }

    /**
     * Change datacheck parameters.
     */
    @Override
    public void changeParameters(String workspaceId) {
        Hashtable<String, String> hashtable = PortalControl.toolsConfigParametersTable;
        String kafkaPath = hashtable.get(Debezium.Kafka.PATH);
        Tools.changeSinglePropertiesParameter("dataDir",PortalControl.portalControlPath + "tmp/zookeeper", kafkaPath + "config/zookeeper.properties");
        Tools.changeSinglePropertiesParameter("log.dirs",PortalControl.portalControlPath + "tmp/kafka-logs", kafkaPath + "config/server.properties");
        Tools.changeSingleYmlParameter("spring.extract.debezium-enable",false,PortalControl.portalWorkSpacePath + "config/datacheck/application-source.yml");
        Tools.changeMigrationDatacheckParameters(PortalControl.toolsMigrationParametersTable);
    }

    @Override
    public void start(String workspaceId) {
        PortalControl.status = Status.START_FULL_MIGRATION_CHECK;
        Plan.runningTaskList.add(Command.Start.Mysql.FULL_CHECK);
        Task.startTaskMethod(Method.Run.ZOOKEEPER,8000);
        Task.startTaskMethod(Method.Run.KAFKA,8000);
        changeParameters(workspaceId);
        Task.startTaskMethod(Method.Run.CHECK_SOURCE,5000);
        Task.startTaskMethod(Method.Run.CHECK_SINK,5000);
        Task.startTaskMethod(Method.Run.CHECK,5000);
        LOGGER.info("Mysql datacheck has started.");
        PortalControl.status = Status.RUNNING_FULL_MIGRATION_CHECK;
        checkEnd();
    }


    public boolean checkNecessaryProcessExist() {
        boolean flag = false;
        boolean flag1 = Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.ZOOKEEPER)) != -1;
        boolean flag2 = Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.KAFKA)) != -1;
        flag = flag1 && flag2;
        return flag;
    }

    public void checkEnd() {
        while (true) {
            if(Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.CHECK))== -1){
                LOGGER.info("Full migration datacheck is finished.");
                PortalControl.status = Status.FULL_MIGRATION_CHECK_FINISHED;
                break;
            }
            if (!Plan.stopPlan) {
                LOGGER.info("Full migration datacheck is running...");
            }
            try {
                Thread.sleep(900);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted exception occurred in running full migration datacheck.");
            }
        }
    }
}
