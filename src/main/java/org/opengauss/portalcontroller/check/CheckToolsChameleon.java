package org.opengauss.portalcontroller.check;

import org.opengauss.portalcontroller.InstallMigrationTools;
import org.opengauss.portalcontroller.PortalControl;
import org.opengauss.portalcontroller.RuntimeExecTools;
import org.opengauss.portalcontroller.Tools;
import org.opengauss.portalcontroller.constant.Chameleon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Hashtable;

public class CheckToolsChameleon implements CheckTool{
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckToolsChameleon.class);

    /**
     * Install chameleon package.
     */
    @Override
    public void installPackage(){
        Hashtable<String, String> hashtable = PortalControl.toolsConfigParametersTable;
        String chameleonVenvPath = hashtable.get(Chameleon.VENV_PATH);
        String chameleonPkgPath = Tools.getPackagePath(Chameleon.PKG_PATH,Chameleon.PKG_NAME);
        Tools.createFile(chameleonVenvPath,false);
        RuntimeExecTools.executeOrder("python3 -m venv " + chameleonVenvPath + "venv", 3000);
        RuntimeExecTools.executeOrder(chameleonVenvPath + "venv/bin/pip3 install " + chameleonPkgPath, 3000);
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
    public void copyConfigFiles(){
        Hashtable<String, String> hashtable = PortalControl.toolsConfigParametersTable;
        String chameleonVenvPath = hashtable.get(Chameleon.VENV_PATH);
        RuntimeExecTools.executeOrder(chameleonVenvPath + "venv/bin/chameleon set_configuration_files", 3000);
        String filePath =  PortalControl.portalControlPath + "config/chameleon/config-example.yml";
        String chameleonPath = hashtable.get(Chameleon.PATH).replaceFirst("~",System.getProperty("user.home"));
        String fileDirectory = chameleonPath + "configuration/";
        String newFileName = chameleonPath + "configuration/default.yml";
        Tools.createFile(fileDirectory,false);
        RuntimeExecTools.copyFile(filePath,newFileName);
    }

    /**
     * Change chameleon parameters.
     */
    @Override
    public void changeParameters(){
        Hashtable<String, String> hashtable = PortalControl.toolsConfigParametersTable;
        String userHome = System.getProperty("user.home");
        String chameleonConfigPath = hashtable.get(Chameleon.PATH).replaceFirst("~", userHome) + "configuration/default.yml";
        Hashtable<String,String> table = new Hashtable<>();
        table.put("sources.mysql.out_dir",chameleonConfigPath);
        boolean flag = Tools.changeParameters(table);
        if(flag){
            LOGGER.warn("Change parameters failed.");
        }
    }
}
