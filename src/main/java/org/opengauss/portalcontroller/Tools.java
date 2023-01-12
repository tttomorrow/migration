/*
 * Copyright (c) 2022-2022 Huawei Technologies Co.,Ltd.
 *
 * openGauss is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *
 *           http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.opengauss.portalcontroller;

import org.opengauss.jdbc.PgConnection;
import org.opengauss.portalcontroller.constant.Chameleon;
import org.opengauss.portalcontroller.constant.Check;
import org.opengauss.portalcontroller.constant.Debezium;
import org.opengauss.portalcontroller.constant.Mysql;
import org.opengauss.portalcontroller.constant.Offset;
import org.opengauss.portalcontroller.constant.Opengauss;
import org.opengauss.portalcontroller.constant.Regex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

/**
 * Tools
 *
 * @author ：liutong
 * @date ：Created in 2022/12/24
 * @since ：1
 */
public class Tools {
    private static final Logger LOGGER = LoggerFactory.getLogger(Tools.class);

    /**
     * Change single parameter in yml file.If key is not in yml file,add key and value.
     *
     * @param key   The key of parameter.
     * @param value The value of parameter you want to change.
     * @param path  The path of configuration file.
     */
    public static void changeSingleYmlParameter(String key, Object value, String path) {
        try {
            File file = new File(path);
            FileInputStream fis = new FileInputStream(file);
            DumperOptions dumperOptions = new DumperOptions();
            dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(dumperOptions);
            LinkedHashMap<String, Object> bigMap = yaml.load(fis);
            fis.close();
            String[] keys = key.split("\\.");
            String lastKey = keys[keys.length - 1];
            Map map = bigMap;
            for (int i = 0; i < keys.length - 1; ++i) {
                String s = keys[i];
                if (map.get(s) == null || !(map.get(s) instanceof Map)) {
                    map.put(s, new HashMap(4));
                }
                map = (HashMap) map.get(s);
            }
            map.put(lastKey, value);
            yaml.dump(bigMap, new FileWriter(file));
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in changing single yml parameter.");
        }
    }

    /**
     * Change parameters in yml file.If keys in paramaeter map are not in yml file,add keys and values.
     *
     * @param changeParametersMap The hashmap of parameters you want to change.
     * @param path                The path of configuration file.
     */
    public static void changeYmlParameters(HashMap<String, Object> changeParametersMap, String path) {
        try {
            File file = new File(path);
            FileInputStream fis = new FileInputStream(file);
            DumperOptions dumperOptions = new DumperOptions();
            dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(dumperOptions);
            LinkedHashMap<String, Object> bigMap = yaml.load(fis);
            fis.close();
            for (String key : changeParametersMap.keySet()) {
                String[] keys = key.split("\\.");
                String lastKey = keys[keys.length - 1];
                Map map = bigMap;
                for (int i = 0; i < keys.length - 1; ++i) {
                    String s = keys[i];
                    if (map.get(s) == null || !(map.get(s) instanceof Map)) {
                        map.put(s, new HashMap<>(4));
                    }
                    map = (HashMap) map.get(s);
                }
                map.put(lastKey, changeParametersMap.get(key));
            }
            yaml.dump(bigMap, new FileWriter(file));
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in changing yml parameters.");
        }
    }

    /**
     * Change single parameter in properties file.If key is not in properties file,add key and value.
     *
     * @param key   The key of parameter.
     * @param value The value of parameter you want to change.
     * @param path  The path of configuration file.
     */
    public static void changeSinglePropertiesParameter(String key, String value, String path) {
        File file = new File(path);
        try {
            ArrayList<String> stringList = new ArrayList<>();
            if (!file.exists()) {
                LOGGER.error("No such file whose path is " + path);
                return;
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            boolean isKeyExist = false;
            while (true) {
                String temp = bufferedReader.readLine();
                if (temp == null) {
                    break;
                }
                if (temp.length() > key.length()) {
                    String tempKey = temp.substring(0, key.length() + 1);
                    if (tempKey.equals(key + "=")) {
                        temp = key + "=" + value;
                        isKeyExist = true;
                    }
                }
                stringList.add(temp);
            }
            bufferedReader.close();
            if (!isKeyExist) {
                String temp = key + "=" + value;
                stringList.add(temp);
            }
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            for (String str : stringList) {
                bufferedWriter.write(str + System.lineSeparator());
            }
            bufferedWriter.close();
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in changing single properties parameter.");
        }
    }

    /**
     * Change parameters in properties file.If keys in paramaeter map are not in properties file,add keys and values.
     *
     * @param originalTable The hashtable of parameters you want to change.
     * @param path          The path of configuration file.
     */
    public static void changePropertiesParameters(Hashtable<String, String> originalTable, String path) {
        File file = new File(path);
        ArrayList<String> stringList = new ArrayList<>();
        if (!file.exists()) {
            LOGGER.error("No such file whose path is " + path);
            return;
        }
        try {
            Hashtable<String, String> table = new Hashtable<>();
            for (String str : originalTable.keySet()) {
                table.put(str, originalTable.get(str));
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            while (true) {
                String temp = bufferedReader.readLine();
                if (temp == null) {
                    break;
                }
                String existKey = "";
                for (String key : table.keySet()) {
                    if (temp.length() > key.length()) {
                        String tempKey = temp.substring(0, key.length() + 1);
                        if (tempKey.equals(key + "=")) {
                            temp = key + "=" + table.get(key);
                            existKey = key;
                        }
                    }
                }
                table.remove(existKey);
                stringList.add(temp);
            }
            bufferedReader.close();
            for (String key : table.keySet()) {
                String temp = key + "=" + table.get(key);
                stringList.add(temp);
            }
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            for (String s : stringList) {
                bufferedWriter.write(s + System.lineSeparator());
            }
            bufferedWriter.close();
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in changing parameters in properties files.");
        }
    }

    /**
     * Get pid of process which contains the command.
     *
     * @param command Command.
     */
    public static int getCommandPid(String command) {
        int pid = -1;
        try {
            Process pro = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ps ux"});
            BufferedInputStream in = new BufferedInputStream(pro.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String s;
            while ((s = br.readLine()) != null) {
                if (s.contains(command)) {
                    String[] strs = s.split("\\s+");
                    pid = Integer.parseInt(strs[1]);
                }
            }
            br.close();
            in.close();
            pro.waitFor();
            pro.destroy();
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in executing the command.Execute command failed.");
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted exception occurred in waiting for process running.");
        }
        return pid;
    }

    /**
     * Get value in properties file with the key.If key is not in properties file,return "".
     *
     * @param key  The key of the parameter you want to get.
     * @param path The path of the configuration file.
     */
    public static String getSinglePropertiesParameter(String key, String path) {
        String value = "";
        Properties pps = new Properties();
        try {
            pps.load(new FileInputStream(path));
            value = pps.getProperty(key);
        } catch (FileNotFoundException e) {
            LOGGER.error("File not found exception occurred in getting single properties parameter.");
            Thread.interrupted();
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in reading parameters in properties files.");
            Thread.interrupted();
        }
        return value;
    }

    /**
     * Get parameters in properties file.
     *
     * @param path The path of the configuration file.
     */
    public static Hashtable<String, String> getPropertiesParameters(String path) {
        Hashtable<String, String> table = new Hashtable<>();
        try {
            Properties pps = new Properties();
            pps.load(new FileInputStream(path));
            for (Object o : pps.keySet()) {
                if (o instanceof String) {
                    table.put(o.toString(), pps.getProperty(o.toString()));
                }
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("File not found exception occurred in getting single properties parameter.");
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in reading parameters in properties files.");
        }
        return table;
    }

    /**
     * Get value in yml file with the key.If key is not in yml file,return "".
     *
     * @param key  The key of the parameter you want to get.
     * @param path The path of the configuration file.
     */
    public static String getSingleYmlParameter(String key, String path) {
        String value = "";
        try {
            File file = new File(path);
            FileInputStream fis = new FileInputStream(file);
            DumperOptions dumperOptions = new DumperOptions();
            dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(dumperOptions);
            LinkedHashMap<String, Object> bigMap = yaml.load(fis);
            fis.close();
            String[] keys = key.split("\\.");
            String lastKey = keys[keys.length - 1];
            Map map = bigMap;
            for (int i = 0; i < keys.length - 1; ++i) {
                String s = keys[i];
                if (map.get(s) == null || !(map.get(s) instanceof Map)) {
                    map.put(s, new HashMap(4));
                }
                map = (HashMap) map.get(s);
            }
            if (map.get(lastKey) instanceof String) {
                value = (String) map.get(lastKey);
            }
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in changing yml parameters.");
        }
        return value;
    }

    /**
     * Get last line in file with the path.
     *
     * @param path The path of the file.
     */
    public static String lastLine(String path) {
        String last = "";
        File file = new File(path);
        StringBuilder builder = new StringBuilder();
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            long fileLastPointer = randomAccessFile.length() - 1;
            for (long filePointer = fileLastPointer; filePointer != -1; filePointer--) {
                randomAccessFile.seek(filePointer);
                int readByte = randomAccessFile.readByte();
                if (0xA == readByte) {
                    if (filePointer == fileLastPointer) {
                        continue;
                    }
                    break;
                }
                if (0xD == readByte) {
                    if (filePointer == fileLastPointer - 1) {
                        continue;
                    }
                    break;
                }
                builder.append((char) readByte);
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("File not found in finding last line in files.");
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in finding last line in files.");
        }
        last = builder.reverse().toString();
        return last;
    }

    /**
     * Change migration parameters include  user name,password,host,port,database name,schema in mysql and openGauss database in migration tools' config files.
     *
     * @param migrationparametersTable The hashtable of migration parameters.
     */
    public static void changeMigrationParameters(Hashtable<String, String> migrationparametersTable) {
        changeFullMigrationParameters(migrationparametersTable);
        changeMigrationDatacheckParameters(migrationparametersTable);
        changeIncrementalMigrationParameters(migrationparametersTable);
        changeReverseMigrationParameters(migrationparametersTable);
    }

    /**
     * Change full migration parameters.
     *
     * @param migrationparametersTable migrationparametersTable
     */
    public static void changeFullMigrationParameters(Hashtable<String, String> migrationparametersTable){
        String chameleonPath = PortalControl.toolsConfigParametersTable.get(Chameleon.PATH).replaceFirst("~", System.getProperty("user.home"));
        String chameleonConfigPath = chameleonPath + "configuration/default.yml";
        HashMap<String, Object> chameleonMap = new HashMap<>();
        String mysqlDatabaseHost = migrationparametersTable.get(Mysql.DATABASE_HOST);
        String mysqlDatabasePort = migrationparametersTable.get(Mysql.DATABASE_PORT);
        String opengaussDatabaseHost = migrationparametersTable.get(Opengauss.DATABASE_HOST);
        String opengaussDatabasePort = migrationparametersTable.get(Opengauss.DATABASE_PORT);
        if (mysqlDatabaseHost.matches(Regex.IP) && mysqlDatabasePort.matches(Regex.PORT) && opengaussDatabaseHost.matches(Regex.IP) && opengaussDatabasePort.matches(Regex.PORT)) {
            chameleonMap.put(Chameleon.Parameters.Mysql.HOST, mysqlDatabaseHost);
            chameleonMap.put(Chameleon.Parameters.Mysql.PORT, mysqlDatabasePort);
            chameleonMap.put(Chameleon.Parameters.Opengauss.HOST, opengaussDatabaseHost);
            chameleonMap.put(Chameleon.Parameters.Opengauss.PORT, opengaussDatabasePort);
            chameleonMap.put(Chameleon.Parameters.Mysql.USER, migrationparametersTable.get(Mysql.USER));
            chameleonMap.put(Chameleon.Parameters.Mysql.PASSWORD, migrationparametersTable.get(Mysql.PASSWORD));
            String mysqlDatabaseName = migrationparametersTable.get(Mysql.DATABASE_NAME);
            chameleonMap.put(Chameleon.Parameters.Mysql.NAME, mysqlDatabaseName);
            chameleonMap.put(Chameleon.Parameters.Opengauss.USER, migrationparametersTable.get(Opengauss.USER));
            chameleonMap.put(Chameleon.Parameters.Opengauss.PASSWORD, migrationparametersTable.get(Opengauss.PASSWORD));
            String opengaussDatabaseName = migrationparametersTable.get(Opengauss.DATABASE_NAME);
            chameleonMap.put(Chameleon.Parameters.Opengauss.NAME, opengaussDatabaseName);
            Tools.changeSingleYmlParameter(Chameleon.Parameters.Mysql.MAPPING, null, chameleonConfigPath);
            chameleonMap.put(Chameleon.Parameters.Mysql.MAPPING + "." + mysqlDatabaseName, migrationparametersTable.get(Opengauss.DATABASE_SCHEMA));
            Tools.changeYmlParameters(chameleonMap, chameleonConfigPath);
        } else {
            LOGGER.error("Invalid parameters.");
        }
    }

    /**
     * Change datacheck parameters.
     *
     * @param migrationparametersTable migrationparametersTable
     */
    public static void changeMigrationDatacheckParameters(Hashtable<String, String> migrationparametersTable){
        String datacheckPath = PortalControl.toolsConfigParametersTable.get(Check.PATH);
        String datacheckSourcePath = datacheckPath + "config/application-source.yml";
        String datacheckSinkPath = datacheckPath + "config/application-sink.yml";
        HashMap<String, Object> datacheckSourceMap = new HashMap<>();
        String mysqlDatabaseName = migrationparametersTable.get(Mysql.DATABASE_NAME);
        String mysqlDatabaseHost = migrationparametersTable.get(Mysql.DATABASE_HOST);
        String mysqlDatabasePort = migrationparametersTable.get(Mysql.DATABASE_PORT);
        String opengaussDatabaseHost = migrationparametersTable.get(Opengauss.DATABASE_HOST);
        String opengaussDatabasePort = migrationparametersTable.get(Opengauss.DATABASE_PORT);
        String mysqlUserName = migrationparametersTable.get(Mysql.USER);
        String mysqlUserPassword = migrationparametersTable.get(Mysql.PASSWORD);
        String opengaussUserName = migrationparametersTable.get(Opengauss.USER);
        String opengaussUserPassword = migrationparametersTable.get(Opengauss.PASSWORD);
        String opengaussDatabaseName = migrationparametersTable.get(Opengauss.DATABASE_NAME);
        String opengaussDatabaseSchema = migrationparametersTable.get(Opengauss.DATABASE_SCHEMA);
        datacheckSourceMap.put(Check.Parameters.SCHEMA, mysqlDatabaseName);
        String mysqlDatacheckUrl = "jdbc:mysql://" + mysqlDatabaseHost + ":" + mysqlDatabasePort + "/" + mysqlDatabaseName + "?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        datacheckSourceMap.put(Check.Parameters.URL, mysqlDatacheckUrl);
        datacheckSourceMap.put(Check.Parameters.USER_NAME, mysqlUserName);
        datacheckSourceMap.put(Check.Parameters.PASSWORD, mysqlUserPassword);
        Tools.changeYmlParameters(datacheckSourceMap, datacheckSourcePath);
        HashMap<String, Object> datacheckSinkMap = new HashMap<>();
        datacheckSinkMap.put(Check.Parameters.SCHEMA, opengaussDatabaseSchema);
        String opengaussDatacheckUrl = "jdbc:opengauss://" + opengaussDatabaseHost + ":" + opengaussDatabasePort + "/" + opengaussDatabaseName + "?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC";
        datacheckSinkMap.put(Check.Parameters.URL, opengaussDatacheckUrl);
        datacheckSinkMap.put(Check.Parameters.USER_NAME, opengaussUserName);
        datacheckSinkMap.put(Check.Parameters.PASSWORD, opengaussUserPassword);
        Tools.changeYmlParameters(datacheckSinkMap, datacheckSinkPath);
    }

    /**
     * Change incremental migration parameters.
     *
     * @param migrationparametersTable migrationparametersTable
     */
    public static void changeIncrementalMigrationParameters(Hashtable<String, String> migrationparametersTable){
        String mysqlDatabaseName = migrationparametersTable.get(Mysql.DATABASE_NAME);
        String mysqlDatabaseHost = migrationparametersTable.get(Mysql.DATABASE_HOST);
        String mysqlDatabasePort = migrationparametersTable.get(Mysql.DATABASE_PORT);
        String opengaussDatabaseHost = migrationparametersTable.get(Opengauss.DATABASE_HOST);
        String opengaussDatabasePort = migrationparametersTable.get(Opengauss.DATABASE_PORT);
        String mysqlUserName = migrationparametersTable.get(Mysql.USER);
        String mysqlUserPassword = migrationparametersTable.get(Mysql.PASSWORD);
        String opengaussUserName = migrationparametersTable.get(Opengauss.USER);
        String opengaussUserPassword = migrationparametersTable.get(Opengauss.PASSWORD);
        String opengaussDatabaseName = migrationparametersTable.get(Opengauss.DATABASE_NAME);
        String confluentPath = PortalControl.toolsConfigParametersTable.get(Debezium.Confluent.PATH);
        Hashtable<String, String> debeziumMysqlTable = new Hashtable<>();
        String confluentMysqlPath = confluentPath + "etc/kafka/mysql-source.properties";
        debeziumMysqlTable.put(Debezium.Source.HOST, mysqlDatabaseHost);
        debeziumMysqlTable.put(Debezium.Source.PORT, mysqlDatabasePort);
        debeziumMysqlTable.put(Debezium.Source.USER, mysqlUserName);
        debeziumMysqlTable.put(Debezium.Source.PASSWORD, mysqlUserPassword);
        debeziumMysqlTable.put(Debezium.Source.WHITELIST, mysqlDatabaseName);
        if(PortalControl.toolsMigrationParametersTable.containsKey(Offset.FILE)){
            debeziumMysqlTable.put(Offset.FILE,PortalControl.toolsMigrationParametersTable.get(Offset.FILE));
        }
        if(PortalControl.toolsMigrationParametersTable.containsKey(Offset.POSITION)){
            debeziumMysqlTable.put(Offset.POSITION,PortalControl.toolsMigrationParametersTable.get(Offset.POSITION));
        }
        if(PortalControl.toolsMigrationParametersTable.containsKey(Offset.GTID)){
            debeziumMysqlTable.put(Offset.GTID, PortalControl.toolsMigrationParametersTable.get(Offset.GTID));
        }
        Tools.changePropertiesParameters(debeziumMysqlTable, confluentMysqlPath);
        String confluentMysqlSinkPath = confluentPath + "etc/kafka/mysql-sink.properties";
        Hashtable<String, String> debeziumMysqlSinkTable = new Hashtable<>();
        debeziumMysqlSinkTable.put(Debezium.Sink.Opengauss.USER, opengaussUserName);
        debeziumMysqlSinkTable.put(Debezium.Sink.Opengauss.PASSWORD, opengaussUserPassword);
        String opengaussDebeziumUrl = "jdbc:opengauss://" + opengaussDatabaseHost + ":" + opengaussDatabasePort + "/" + opengaussDatabaseName + "?loggerLevel=OFF";
        debeziumMysqlSinkTable.put(Debezium.Sink.Opengauss.URL, opengaussDebeziumUrl);
        Tools.changePropertiesParameters(debeziumMysqlSinkTable, confluentMysqlSinkPath);
    }

    /**
     * Change reverse migration parameters.
     *
     * @param migrationparametersTable migrationparametersTable
     */
    public static void changeReverseMigrationParameters(Hashtable<String, String> migrationparametersTable){
        String mysqlDatabaseName = migrationparametersTable.get(Mysql.DATABASE_NAME);
        String mysqlDatabaseHost = migrationparametersTable.get(Mysql.DATABASE_HOST);
        String mysqlDatabasePort = migrationparametersTable.get(Mysql.DATABASE_PORT);
        String opengaussDatabaseHost = migrationparametersTable.get(Opengauss.DATABASE_HOST);
        String opengaussDatabasePort = migrationparametersTable.get(Opengauss.DATABASE_PORT);
        String mysqlUserName = migrationparametersTable.get(Mysql.USER);
        String mysqlUserPassword = migrationparametersTable.get(Mysql.PASSWORD);
        String opengaussUserName = migrationparametersTable.get(Opengauss.USER);
        String opengaussUserPassword = migrationparametersTable.get(Opengauss.PASSWORD);
        String opengaussDatabaseName = migrationparametersTable.get(Opengauss.DATABASE_NAME);
        String confluentPath = PortalControl.toolsConfigParametersTable.get(Debezium.Confluent.PATH);
        Hashtable<String, String> debeziumOpenGaussTable = new Hashtable<>();
        String confluentOpenGaussPath = confluentPath + "etc/kafka/opengauss-source.properties";
        debeziumOpenGaussTable.put(Debezium.Source.HOST, opengaussDatabaseHost);
        debeziumOpenGaussTable.put(Debezium.Source.PORT, opengaussDatabasePort);
        debeziumOpenGaussTable.put(Debezium.Source.USER, opengaussUserName);
        debeziumOpenGaussTable.put(Debezium.Source.PASSWORD, opengaussUserPassword);
        debeziumOpenGaussTable.put(Debezium.Source.NAME, opengaussDatabaseName);
        Tools.changePropertiesParameters(debeziumOpenGaussTable, confluentOpenGaussPath);
        Hashtable<String, String> debeziumOpenGaussSinkTable = new Hashtable<>();
        String confluentOpenGaussSinkPath = confluentPath + "etc/kafka/opengauss-sink.properties";
        debeziumOpenGaussSinkTable.put(Debezium.Sink.Mysql.USER, mysqlUserName);
        debeziumOpenGaussSinkTable.put(Debezium.Sink.Mysql.PASSWORD, mysqlUserPassword);
        debeziumOpenGaussSinkTable.put(Debezium.Sink.Mysql.NAME, mysqlDatabaseName);
        debeziumOpenGaussSinkTable.put(Debezium.Sink.Mysql.PORT, mysqlDatabasePort);
        debeziumOpenGaussSinkTable.put(Debezium.Sink.Mysql.URL, mysqlDatabaseHost);
        Tools.changePropertiesParameters(debeziumOpenGaussSinkTable, confluentOpenGaussSinkPath);
    }

    /**
     * Find t_binlog_name,i_binlog_position,t_gtid_set in opengauss and set parameters in increment tool's config files.
     */
    public static void findOffset() {
        String offsetPath = PortalControl.portalControlPath + "config/migrationConfig.properties";
        Properties pps = new Properties();
        try {
            pps.load(new FileInputStream(PortalControl.migrationConfigPath));
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in loading parameters in properties files.");
        }
        String opengaussDatabaseHost = pps.getProperty(Opengauss.DATABASE_HOST);
        String opengaussDatabasePort = pps.getProperty(Opengauss.DATABASE_PORT);
        String opengaussDatabaseName = pps.getProperty(Opengauss.DATABASE_NAME);
        String opengaussUserName = pps.getProperty(Opengauss.USER);
        String opengaussUserPassword = pps.getProperty(Opengauss.PASSWORD);
        String opengaussUrl = "jdbc:opengauss://" + opengaussDatabaseHost + ":" + opengaussDatabasePort + "/" + opengaussDatabaseName;
        try {
            PgConnection conn = (PgConnection) DriverManager.getConnection(opengaussUrl, opengaussUserName, opengaussUserPassword);
            String sql = "select t_binlog_name,i_binlog_position,t_gtid_set from sch_chameleon.t_replica_batch;";
            ResultSet rs = conn.execSQLQuery(sql);
            if (rs.next()) {
                String tBinlogName = rs.getString("t_binlog_name");
                String iBinlogPosition = rs.getString("i_binlog_position");
                String tGtidSet = rs.getString("t_gtid_set");
                int offset = Integer.parseInt(tGtidSet.substring(tGtidSet.lastIndexOf("-") + 1));
                offset--;
                String offsetGtidSet = tGtidSet.substring(0, tGtidSet.lastIndexOf("-") + 1) + offset;
                Hashtable<String, String> offsetHashtable = new Hashtable<>();
                offsetHashtable.put(Offset.FILE, tBinlogName);
                offsetHashtable.put(Offset.POSITION, iBinlogPosition);
                offsetHashtable.put(Offset.GTID, offsetGtidSet);
                Tools.changePropertiesParameters(offsetHashtable, offsetPath);
            }
            rs.close();
            conn.close();
        } catch (SQLException e) {
            LOGGER.error("SQL exception occurred in searching parameters in mysql database.");
        }
    }

    /**
     * Generate plan history.
     *
     * @param taskList The tasklist of rhe plan.
     */
    public static void generatePlanHistory(List<String> taskList) {
        File file = new File(PortalControl.portalControlPath + "logs/planHistory.log");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd:hh:mm:ss");
            LOGGER.info(dateFormat.format(date));
            LOGGER.info("Current plan: ");
            for (String str : taskList) {
                LOGGER.info(str);
            }
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in generating plan history.");
        }
    }

    /**
     * Read status to get information.
     */
    public static void readStatus() {
        File file = new File(PortalControl.portalControlPath + "config/status");
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String str = "";
            String flag = "";
            ArrayList<String> runningTaskList = new ArrayList<>();
            ArrayList<String> taskList = new ArrayList<>();
            ArrayList<RunningTaskThread> runningTaskThreadsList = new ArrayList<>();
            while ((str = br.readLine()) != null) {
                if (str.contains("Plan status:running")) {
                    Plan.isPlanRunnable = false;
                } else if (str.contains("Plan status:runnable")) {
                    Plan.isPlanRunnable = true;
                }
                if (str.contains("Current plan:")) {
                    flag = "task";
                    continue;
                } else if (str.contains("Running task list:")) {
                    flag = "running task";
                    continue;
                } else if (str.contains("Running task threads list:")) {
                    flag = "running thread";
                    continue;
                }
                switch (flag) {
                    case "task": {
                        taskList.add(str);
                        break;
                    }
                    case "running task": {
                        runningTaskList.add(str);
                        break;
                    }
                    case "running thread": {
                        if (str.contains("method name | process name | pid ")) {
                            continue;
                        }
                        String strs[] = str.split("\\|");
                        RunningTaskThread runningTaskThread = new RunningTaskThread(strs[0], strs[1], Integer.parseInt(strs[2]));
                        runningTaskThreadsList.add(runningTaskThread);
                        break;
                    }
                    default:
                        break;
                }
            }
            Plan.runningTaskList = runningTaskList;
            PortalControl.taskList = taskList;
            Plan.setRunningTaskThreadsList(runningTaskThreadsList);
        } catch (FileNotFoundException e) {
            LOGGER.error("File portal.lock not found.");
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in reading portal.lock.");
        }
    }

    /**
     * Read input order to execute.
     */
    public static int readInputOrder() {
        int temp = 0;
        boolean flag = false;
        File file = new File(PortalControl.portalControlPath + "config/input");
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String str = "";
            while ((str = br.readLine()) != null) {
                temp += 1;
                if (temp > PortalControl.commandCounts) {
                    PortalControl.command = str.trim();
                }
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("File flag not found.");
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in reading portal.lock.");
        }
        return temp;
    }

    /**
     * Clean input order.
     */
    public static void cleanInputOrder() {
        File file = new File(PortalControl.portalControlPath + "config/input");
        if (file.exists()) {
            file.delete();
            try {
                file.createNewFile();
            } catch (IOException e) {
                LOGGER.error("IO exception occurred in creating flag file.");
            }
        }
    }

    /**
     * Check input order.
     *
     * @param scanner Scanner to input.
     * @param regex   Regex to match.
     * @return String Valid input String.
     */
    public static String checkInputString(Scanner scanner, String regex) {
        while (true) {
            String value = scanner.nextLine().trim();
            if (value.matches(regex) || regex.equals("")) {
                return value;
            } else {
                LOGGER.error("Invalid input string.Please checkout the input string.");
            }
        }
    }

    /**
     * Create file.
     *
     * @param path Path.
     * @param isFile IsFile.If the value is true,it means the file is a file.If the value is false,it means the file is a directory.
     * @return String Valid input String.
     */
    public static void createFile(String path, boolean isFile) {
        File file = new File(path);
        if (!file.exists()) {
            if (isFile) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    LOGGER.error("IO exception occurred in creating new file.");
                }
            } else {
                file.mkdirs();
            }
        } else {
            LOGGER.info("File " + path + " has existed.");
        }
    }

    /**
     * Get package path.
     *
     * @param pkgPath PkgPath parameter.
     * @param pkgName PkgName parameter.
     * @return String Get package path.
     */
    public static String getPackagePath(String pkgPath,String pkgName) {
        Hashtable<String, String> hashtable = PortalControl.toolsConfigParametersTable;
        String path = "";
        String name = "";
        if (PortalControl.noinput) {
            path = hashtable.get(pkgPath);
            name = hashtable.get(pkgName);
        }else {
            Scanner scanner = new Scanner(System.in);
            LOGGER.info("You can input change to change the path,or input other command to use default parameters.");
            String skipFlag = scanner.nextLine().trim();
            if (!skipFlag.equals("change")) {
                path = hashtable.get(pkgPath);
                name = hashtable.get(pkgName);
            } else {
                LOGGER.info("Please input the value of parameter " + pkgPath + " in toolspath.properties");
                path = Tools.checkInputString(scanner, Regex.FOLDER_PATH);
                Tools.changeSinglePropertiesParameter(pkgPath, path, PortalControl.toolsConfigPath);
                LOGGER.info("Please input the name of parameter " + pkgName + " in toolspath.properties");
                name = Tools.checkInputString(scanner,Regex.PKG_NAME);
                Tools.changeSinglePropertiesParameter(pkgName, name, PortalControl.toolsConfigPath);
            }
        }
        path += name;
        return path;
    }

    /**
     * Install package.
     *
     * @param pkgPathParameter PkgPath parameter.
     * @param pkgNameParameter PkgName parameter.
     * @param pathParameter Path parameter.
     */
    public static void installPackage(String pkgPathParameter,String pkgNameParameter,String pathParameter) {
        String packagePath = Tools.getPackagePath(pkgPathParameter,pkgNameParameter);
        Tools.createFile(pathParameter,false);
        RuntimeExecTools.unzipFile(packagePath, pathParameter);
    }

    /**
     * Change parameters.
     *
     * @param parameterTable ParameterTable.
     *
     * @return boolean Result of change parameters.
     */
    public static boolean changeParameters(Hashtable<String,String> parameterTable) {
        boolean flag = true;
        for(String key:parameterTable.keySet()){
            String path = parameterTable.get(key);
            if(path.endsWith(".properties")){
                String value = Tools.getSinglePropertiesParameter(key,path);
                Tools.changeSinglePropertiesParameter(key, PortalControl.portalControlPath + value, path);
            }else if(path.endsWith(".yml")){
                String value = Tools.getSingleYmlParameter(key,path);
                Tools.changeSingleYmlParameter(key, PortalControl.portalControlPath + value, path);
            }else{
                LOGGER.error("Invalid file type.");
                flag = false;
            }
        }
        return flag;
    }
}
