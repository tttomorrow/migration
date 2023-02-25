package org.opengauss.portalcontroller.check;

import org.opengauss.portalcontroller.Plan;
import org.opengauss.portalcontroller.PortalControl;
import org.opengauss.portalcontroller.RuntimeExecTools;
import org.opengauss.portalcontroller.Task;
import org.opengauss.portalcontroller.Tools;
import org.opengauss.portalcontroller.constant.Chameleon;
import org.opengauss.portalcontroller.constant.Command;
import org.opengauss.portalcontroller.constant.MigrationParameters;
import org.opengauss.portalcontroller.constant.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Hashtable;

import static org.opengauss.portalcontroller.Plan.runningTaskList;

public class CheckTaskMysqlFullMigration implements CheckTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckTaskMysqlFullMigration.class);

    /**
     * Install chameleon package.
     */
    @Override
    public void installAllPackages(){
        Hashtable<String, String> hashtable = PortalControl.toolsConfigParametersTable;
        String chameleonVenvPath = hashtable.get(Chameleon.VENV_PATH);
        String chameleonPkgPath = Tools.getPackagePath(Chameleon.PKG_PATH,Chameleon.PKG_NAME);
        Tools.createFile(chameleonVenvPath,false);
        RuntimeExecTools.executeOrder("python3 -m venv " + chameleonVenvPath + "venv", 3000);
        String[] cmdParts = (chameleonVenvPath + "venv/bin/pip3 install " + chameleonPkgPath).split(" ");
        RuntimeExecTools.executeOrder(cmdParts,3000,PortalControl.portalControlPath,PortalControl.portalControlPath + "logs/install_chameleon.log");
        try {
            File chameleonFile = new File(chameleonVenvPath + "venv/bin/chameleon");
            while (true) {
                Thread.sleep(1000);
                LOGGER.info("Installing chameleon...");
                if (chameleonFile.exists()) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted exception occurred in waiting for process running.");
        }
    }

    /**
     * Copy chameleon files.
     */
    @Override
    public void copyConfigFiles(String workspaceId){
        Hashtable<String, String> hashtable = PortalControl.toolsConfigParametersTable;
        String chameleonVenvPath = hashtable.get(Chameleon.VENV_PATH);
        RuntimeExecTools.executeOrder(chameleonVenvPath + "venv/bin/chameleon set_configuration_files", 3000);
        String chameleonPath = hashtable.get(Chameleon.PATH).replaceFirst("~",System.getProperty("user.home"));
        String fileDirectory = chameleonPath + "configuration/";
        String newFileName = PortalControl.portalWorkSpacePath + "config/chameleon/default_"+ workspaceId +".yml";
        Tools.createFile(fileDirectory,false);
        RuntimeExecTools.copyFile(newFileName,fileDirectory);
    }

    /**
     * Change chameleon parameters.
     */
    @Override
    public void changeParameters(String workspaceId){
        String chameleonConfigOldName = PortalControl.portalWorkSpacePath + "config/chameleon/config-example.yml";
        String chameleonConfigPath = PortalControl.portalWorkSpacePath + "config/chameleon/default_"+ workspaceId +".yml";
        RuntimeExecTools.rename(chameleonConfigOldName,chameleonConfigPath);
        Tools.createFile(PortalControl.portalWorkSpacePath + "pid/",false);
        Tools.changeSingleYmlParameter("pid_dir",PortalControl.portalWorkSpacePath + "pid/",chameleonConfigPath);
        Tools.changeSingleYmlParameter("sources.mysql.out_dir",PortalControl.portalWorkSpacePath + "tmp",chameleonConfigPath);
        Tools.changeFullMigrationParameters(PortalControl.toolsMigrationParametersTable,workspaceId);

    }

    @Override
    public void prepareWork(String workspaceId){
        runningTaskList.add(Command.Start.Mysql.FULL);
        PortalControl.status = Status.START_FULL_MIGRATION;
        changeParameters(workspaceId);
        copyConfigFiles(workspaceId);
        Task task = new Task();
        String chameleonVenv = Tools.getSinglePropertiesParameter(Chameleon.VENV_PATH, PortalControl.toolsConfigPath);
        Hashtable<String, String> chameleonParameterTable = new Hashtable<>();
        chameleonParameterTable.put("--config", "default_"+workspaceId);
        task.useChameleonReplicaOrder(chameleonVenv, "drop_replica_schema", chameleonParameterTable,workspaceId);
        task.useChameleonReplicaOrder(chameleonVenv, "create_replica_schema", chameleonParameterTable,workspaceId);
        chameleonParameterTable.put("--source", "mysql");
        task.useChameleonReplicaOrder(chameleonVenv, "add_source", chameleonParameterTable,workspaceId);
        task.startChameleonReplicaOrder(chameleonVenv, "init_replica", chameleonParameterTable);
        PortalControl.status = Status.RUNNING_FULL_MIGRATION;
        LOGGER.info("Mysql full migration is running.");
    }

    @Override
    public void start(String workspaceId){
        Task task = new Task();
        String chameleonVenv = Tools.getSinglePropertiesParameter(Chameleon.VENV_PATH, PortalControl.toolsConfigPath);
        Hashtable<String, String> chameleonParameterTable = new Hashtable<>();
        chameleonParameterTable.put("--config", "default_"+workspaceId);
        chameleonParameterTable.put("--source", "mysql");
        task.checkChameleonReplicaOrder("init_replica", chameleonParameterTable,workspaceId);
        if (PortalControl.toolsMigrationParametersTable.get(MigrationParameters.SNAPSHOT_OBJECT).equals("yes")) {
            task.useChameleonReplicaOrder(chameleonVenv, "start_trigger_replica", chameleonParameterTable,workspaceId);
            task.useChameleonReplicaOrder(chameleonVenv, "start_view_replica", chameleonParameterTable,workspaceId);
            task.useChameleonReplicaOrder(chameleonVenv, "start_func_replica", chameleonParameterTable,workspaceId);
            task.useChameleonReplicaOrder(chameleonVenv, "start_proc_replica", chameleonParameterTable,workspaceId);
        }
        chameleonParameterTable.clear();
        LOGGER.info("Mysql full migration finished.");
        PortalControl.status = Status.FULL_MIGRATION_FINISHED;
    }

    public void cleanData(String workspaceId) {
        Task task = new Task();
        String chameleonVenv = Tools.getSinglePropertiesParameter(Chameleon.VENV_PATH, PortalControl.toolsConfigPath);
        Hashtable<String, String> chameleonParameterTable = new Hashtable<>();
        chameleonParameterTable.put("--config", "default_"+workspaceId);
        task.useChameleonReplicaOrder(chameleonVenv,"drop_replica_schema", chameleonParameterTable,workspaceId);
        String chameleonVenvPath = PortalControl.toolsConfigParametersTable.get(Chameleon.VENV_PATH);
        RuntimeExecTools.removeFile(chameleonVenvPath + "data_default_" + Plan.workspaceId + "_init_replica.json");
        RuntimeExecTools.removeFile(chameleonVenvPath + "data_default_" + Plan.workspaceId + "_start_view_replica.json");
        RuntimeExecTools.removeFile(chameleonVenvPath + "data_default_" + Plan.workspaceId + "_start_trigger_replica.json");
        RuntimeExecTools.removeFile(chameleonVenvPath + "data_default_" + Plan.workspaceId + "_start_proc_replica.json");
        RuntimeExecTools.removeFile(chameleonVenvPath + "data_default_" + Plan.workspaceId + "_start_func_replica.json");
    }

    public void checkEnd(){

    }

}
