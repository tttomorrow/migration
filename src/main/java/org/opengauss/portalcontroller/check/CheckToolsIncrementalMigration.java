package org.opengauss.portalcontroller.check;

import org.opengauss.portalcontroller.PortalControl;
import org.opengauss.portalcontroller.RuntimeExecTools;
import org.opengauss.portalcontroller.Tools;
import org.opengauss.portalcontroller.constant.Debezium;

import java.util.Hashtable;

public class CheckToolsIncrementalMigration implements CheckTool{

    /**
     * Install incremental migration tools package.
     */
    @Override
    public void installPackage(){
        Hashtable<String, String> hashtable = PortalControl.toolsConfigParametersTable;
        String debeziumPath = hashtable.get(Debezium.PATH);
        String connectorPath = hashtable.get(Debezium.Connector.PATH);
        Tools.installPackage(Debezium.PKG_PATH,Debezium.Kafka.PKG_NAME,debeziumPath);
        Tools.installPackage(Debezium.PKG_PATH,Debezium.Confluent.PKG_NAME,debeziumPath);
        Tools.installPackage(Debezium.PKG_PATH,Debezium.Connector.MYSQL_PKG_NAME,connectorPath);
        Tools.installPackage(Debezium.PKG_PATH,Debezium.Connector.OPENGAUSS_PKG_NAME,connectorPath);
    }

    /**
     * Copy incremental migration tools files.
     */
    public void copyConfigFiles(){
        String confluentConfigDirectory = PortalControl.toolsConfigParametersTable.get(Debezium.Confluent.PATH) + "etc/kafka/";
        Hashtable<String,String> configFilesTable = new Hashtable<>();
        configFilesTable.put(PortalControl.portalControlPath + "config/debezium/mysql-source.properties", confluentConfigDirectory);
        configFilesTable.put(PortalControl.portalControlPath + "config/debezium/mysql-sink.properties", confluentConfigDirectory);
        configFilesTable.put(PortalControl.portalControlPath + "config/debezium/opengauss-source.properties", confluentConfigDirectory);
        configFilesTable.put(PortalControl.portalControlPath + "config/debezium/opengauss-sink.properties", confluentConfigDirectory);
        for(String key:configFilesTable.keySet()){
            Tools.createFile(configFilesTable.get(key),false);
            RuntimeExecTools.copyFile(key,configFilesTable.get(key));
        }
    }

    /**
     * Change incremental migration tools parameters.
     */
    @Override
    public void changeParameters(){
        Hashtable<String, String> hashtable = PortalControl.toolsConfigParametersTable;
        String connectorPath = hashtable.get(Debezium.Connector.PATH);
        String confluentPath = hashtable.get(Debezium.Confluent.PATH);
        String kafkaPath = hashtable.get(Debezium.Kafka.PATH);
        Hashtable<String,String> table = new Hashtable<>();
        table.put("dataDir",kafkaPath + "config/zookeeper.properties");
        table.put("log.dirs",kafkaPath + "config/server.properties");
        table.put("offset.storage.file.filename",confluentPath + "etc/schema-registry/connect-avro-standalone.properties");
        Tools.changeParameters(table);
        Tools.changeSinglePropertiesParameter("plugin.path", "share/java, " + connectorPath, confluentPath + "etc/schema-registry/connect-avro-standalone.properties");
    }
}
