package org.opengauss.portalcontroller.check;

import org.opengauss.portalcontroller.PortalControl;
import org.opengauss.portalcontroller.RuntimeExecTools;
import org.opengauss.portalcontroller.Tools;
import org.opengauss.portalcontroller.constant.Check;
import org.opengauss.portalcontroller.constant.Debezium;

import java.util.Hashtable;

public class CheckToolsDatacheck implements CheckTool{

    /**
     * Install datacheck package.
     */
    @Override
    public void installPackage(){
        Hashtable<String, String> hashtable = PortalControl.toolsConfigParametersTable;
        String debeziumPath = hashtable.get(Debezium.PATH);
        Tools.installPackage(Debezium.PKG_PATH,Debezium.Kafka.PKG_NAME,debeziumPath);
        String datacheckInstallPath = hashtable.get(Check.INSTALL_PATH);
        Tools.installPackage(Check.PKG_PATH,Check.PKG_NAME,datacheckInstallPath);
    }

    /**
     * Copy datacheck config files.
     */
    @Override
    public void copyConfigFiles(){
        Hashtable<String, String> hashtable = PortalControl.toolsConfigParametersTable;
        String datacheckPath = hashtable.get(Check.PATH);
        RuntimeExecTools.copyFile(PortalControl.portalControlPath + "config/datacheck/application-sink.yml",datacheckPath + "config/");
        RuntimeExecTools.copyFile(PortalControl.portalControlPath + "config/datacheck/application-source.yml",datacheckPath + "config/");
        RuntimeExecTools.copyFile(PortalControl.portalControlPath + "config/datacheck/application.yml",datacheckPath + "config/");
    }

    /**
     * Change datacheck parameters.
     */
    @Override
    public void changeParameters(){

    }
}
