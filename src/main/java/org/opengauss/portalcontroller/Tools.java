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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.opengauss.jdbc.PgConnection;
import org.opengauss.portalcontroller.check.CheckTask;
import org.opengauss.portalcontroller.constant.Chameleon;
import org.opengauss.portalcontroller.constant.Check;
import org.opengauss.portalcontroller.constant.Command;
import org.opengauss.portalcontroller.constant.Debezium;
import org.opengauss.portalcontroller.constant.Default;
import org.opengauss.portalcontroller.constant.Mysql;
import org.opengauss.portalcontroller.constant.Offset;
import org.opengauss.portalcontroller.constant.Opengauss;
import org.opengauss.portalcontroller.constant.Regex;
import org.opengauss.portalcontroller.constant.Status;
import org.opengauss.portalcontroller.status.CheckRules;
import org.opengauss.portalcontroller.status.FullMigrationStatus;
import org.opengauss.portalcontroller.status.ObjectStatus;
import org.opengauss.portalcontroller.status.TableStatus;
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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
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
            LOGGER.error("IO exception occurred in changing single yml parameter " + key + ".");
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
                bufferedWriter.flush();
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
                bufferedWriter.flush();
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
     * @return the command pid
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
     * Close all process.
     *
     * @param command the command
     */
    public static void closeAllProcess(String command) {
        try {
            Process pro = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ps ux"});
            BufferedInputStream in = new BufferedInputStream(pro.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String s;
            while ((s = br.readLine()) != null) {
                if (s.contains(command)) {
                    String[] strs = s.split("\\s+");
                    int pid = Integer.parseInt(strs[1]);
                    RuntimeExecTools.executeOrder("kill -9 " + pid, 20,PortalControl.portalWorkSpacePath + "logs/error.log");
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
    }

    /**
     * Check another process exist boolean.
     *
     * @param command the command
     * @return the boolean
     */
    public static boolean checkAnotherProcessExist(String command) {
        boolean signal = false;
        int count = 0;
        try {
            Process pro = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ps ux"});
            BufferedInputStream in = new BufferedInputStream(pro.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String s = "";
            while ((s = br.readLine()) != null) {
                if (s.contains(command)) {
                    count++;
                    if (count > 1) {
                        signal = true;
                        break;
                    }
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
        return signal;
    }

    /**
     * Get value in properties file with the key.If key is not in properties file,return "".
     *
     * @param key  The key of the parameter you want to get.
     * @param path The path of the configuration file.
     * @return the single properties parameter
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
     * @return the properties parameters
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
     * @return the single yml parameter
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
            LOGGER.error("IO exception occurred in changing yml parameter " + key + " to " + value + " in file " + path + ".");
        }
        return value;
    }

    /**
     * Get last line in file with the path.
     *
     * @param path The path of the file.
     * @return the string
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
        String workspaceId = PortalControl.commandLineParameterStringMap.get(Command.Parameters.ID);
        changeFullMigrationParameters(migrationparametersTable, workspaceId);
        changeMigrationDatacheckParameters(migrationparametersTable);
        changeIncrementalMigrationParameters(migrationparametersTable, workspaceId);
        changeReverseMigrationParameters(migrationparametersTable);
    }

    /**
     * Change full migration parameters.
     *
     * @param migrationparametersTable migrationparametersTable
     * @param workspaceId              the workspace id
     */
    public static void changeFullMigrationParameters(Hashtable<String, String> migrationparametersTable, String workspaceId) {
        String chameleonConfigPath = PortalControl.portalWorkSpacePath + "config/chameleon/default_" + workspaceId + ".yml";
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
    public static void changeMigrationDatacheckParameters(Hashtable<String, String> migrationparametersTable) {
        String datacheckSourcePath = PortalControl.portalWorkSpacePath + "config/datacheck/application-source.yml";
        String datacheckSinkPath = PortalControl.portalWorkSpacePath + "config/datacheck/application-sink.yml";
        String datacheckServicePath = PortalControl.portalWorkSpacePath + "config/datacheck/application.yml";
        ArrayList<Integer> portList = Tools.getAvailablePorts(3, 1000);
        int sourcePort = portList.get(0);
        int sinkPort = portList.get(1);
        int servicePort = portList.get(2);
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
        datacheckSourceMap.put("spring.check.server-uri", "http://127.0.0.1:" + servicePort);
        datacheckSourceMap.put("server.port", sourcePort);
        datacheckSourceMap.put("logging.config", PortalControl.portalWorkSpacePath + "config/datacheck/log4j2source.xml");
        Tools.changeYmlParameters(datacheckSourceMap, datacheckSourcePath);
        HashMap<String, Object> datacheckSinkMap = new HashMap<>();
        datacheckSinkMap.put(Check.Parameters.SCHEMA, opengaussDatabaseSchema);
        String opengaussDatacheckUrl = "jdbc:opengauss://" + opengaussDatabaseHost + ":" + opengaussDatabasePort + "/" + opengaussDatabaseName + "?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC";
        datacheckSinkMap.put(Check.Parameters.URL, opengaussDatacheckUrl);
        datacheckSinkMap.put(Check.Parameters.USER_NAME, opengaussUserName);
        datacheckSinkMap.put(Check.Parameters.PASSWORD, opengaussUserPassword);
        datacheckSinkMap.put("spring.check.server-uri", "http://127.0.0.1:" + servicePort);
        datacheckSinkMap.put("server.port", sinkPort);
        datacheckSinkMap.put("logging.config", PortalControl.portalWorkSpacePath + "config/datacheck/log4j2sink.xml");
        Tools.changeYmlParameters(datacheckSinkMap, datacheckSinkPath);
        HashMap<String, Object> datacheckServiceMap = new HashMap<>();
        datacheckServiceMap.put("data.check.source-uri", "http://127.0.0.1:" + sourcePort);
        datacheckServiceMap.put("data.check.sink-uri", "http://127.0.0.1:" + sinkPort);
        datacheckServiceMap.put("server.port", servicePort);
        datacheckServiceMap.put("data.check.data-path", PortalControl.portalWorkSpacePath + "check_result");
        datacheckServiceMap.put("logging.config", PortalControl.portalWorkSpacePath + "config/datacheck/log4j2.xml");
        Tools.changeYmlParameters(datacheckServiceMap, datacheckServicePath);
    }

    /**
     * Change incremental migration parameters.
     *
     * @param migrationparametersTable migrationparametersTable
     * @param workspaceId              the workspace id
     */
    public static void changeIncrementalMigrationParameters(Hashtable<String, String> migrationparametersTable, String workspaceId) {
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
        String openGaussSchemaName = migrationparametersTable.get(Opengauss.DATABASE_SCHEMA);
        Hashtable<String, String> debeziumMysqlTable = new Hashtable<>();
        String confluentMysqlSourcePath = PortalControl.portalWorkSpacePath + "config/debezium/mysql-source.properties";
        debeziumMysqlTable.put(Debezium.Source.HOST, mysqlDatabaseHost);
        debeziumMysqlTable.put(Debezium.Source.PORT, mysqlDatabasePort);
        debeziumMysqlTable.put(Debezium.Source.USER, mysqlUserName);
        debeziumMysqlTable.put(Debezium.Source.PASSWORD, mysqlUserPassword);
        debeziumMysqlTable.put(Debezium.Source.WHITELIST, mysqlDatabaseName);
        if (PortalControl.toolsMigrationParametersTable.containsKey(Offset.FILE)) {
            debeziumMysqlTable.put(Offset.FILE, PortalControl.toolsMigrationParametersTable.get(Offset.FILE));
        }
        if (PortalControl.toolsMigrationParametersTable.containsKey(Offset.POSITION)) {
            debeziumMysqlTable.put(Offset.POSITION, PortalControl.toolsMigrationParametersTable.get(Offset.POSITION));
        }
        if (PortalControl.toolsMigrationParametersTable.containsKey(Offset.GTID)) {
            debeziumMysqlTable.put(Offset.GTID, PortalControl.toolsMigrationParametersTable.get(Offset.GTID));
        }
        Tools.changePropertiesParameters(debeziumMysqlTable, confluentMysqlSourcePath);
        String confluentMysqlSinkPath = PortalControl.portalWorkSpacePath + "config/debezium/mysql-sink.properties";
        Hashtable<String, String> debeziumMysqlSinkTable = new Hashtable<>();
        debeziumMysqlSinkTable.put(Debezium.Sink.SCHEMA_MAPPING, mysqlDatabaseName + ":" + openGaussSchemaName);
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
    public static void changeReverseMigrationParameters(Hashtable<String, String> migrationparametersTable) {
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
        String openGaussSchema = migrationparametersTable.get(Opengauss.DATABASE_SCHEMA);
        Hashtable<String, String> debeziumOpenGaussTable = new Hashtable<>();
        String confluentOpenGaussSourcePath = PortalControl.portalWorkSpacePath + "config/debezium/opengauss-source.properties";
        debeziumOpenGaussTable.put(Debezium.Source.HOST, opengaussDatabaseHost);
        debeziumOpenGaussTable.put(Debezium.Source.PORT, opengaussDatabasePort);
        debeziumOpenGaussTable.put(Debezium.Source.USER, opengaussUserName);
        debeziumOpenGaussTable.put(Debezium.Source.PASSWORD, opengaussUserPassword);
        debeziumOpenGaussTable.put(Debezium.Source.NAME, opengaussDatabaseName);
        Tools.changePropertiesParameters(debeziumOpenGaussTable, confluentOpenGaussSourcePath);
        Hashtable<String, String> debeziumOpenGaussSinkTable = new Hashtable<>();
        String confluentOpenGaussSinkPath = PortalControl.portalWorkSpacePath + "config/debezium/opengauss-sink.properties";
        debeziumOpenGaussSinkTable.put(Debezium.Sink.Mysql.USER, mysqlUserName);
        debeziumOpenGaussSinkTable.put(Debezium.Sink.Mysql.PASSWORD, mysqlUserPassword);
        debeziumOpenGaussSinkTable.put(Debezium.Sink.Mysql.NAME, mysqlDatabaseName);
        debeziumOpenGaussSinkTable.put(Debezium.Sink.Mysql.PORT, mysqlDatabasePort);
        debeziumOpenGaussSinkTable.put(Debezium.Sink.Mysql.URL, mysqlDatabaseHost);
        debeziumOpenGaussSinkTable.put(Debezium.Sink.SCHEMA_MAPPING, openGaussSchema + ":" + mysqlDatabaseName);
        Tools.changePropertiesParameters(debeziumOpenGaussSinkTable, confluentOpenGaussSinkPath);
    }

    /**
     * Find t_binlog_name,i_binlog_position,t_gtid_set in opengauss and set parameters in increment tool's config files.
     *
     * @param workspaceId the workspace id
     */
    public static void findOffset(String workspaceId) {
        String offsetPath = PortalControl.portalWorkSpacePath + "config/debezium/mysql-source.properties";
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
     * Read input order to execute.
     */
    public static void readInputOrder() {
        File file = new File(PortalControl.portalWorkSpacePath + "config/input");
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String str = "";
            while ((str = br.readLine()) != null) {
                if (!PortalControl.latestCommand.equals(str.trim())) {
                    LOGGER.warn(str);
                    PortalControl.latestCommand = str.trim();
                    changeMigrationStatus(str.trim());
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("File flag not found.");
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in reading portal.lock.");
        }
    }

    /**
     * Write input order int.
     *
     * @param command the command
     * @return the int
     */
    public static int writeInputOrder(String command) {
        int temp = 0;
        boolean flag = false;
        File file = new File(PortalControl.portalWorkSpacePath + "config/input");
        try {
            RuntimeExecTools.executeOrder("mkfifo " + PortalControl.portalWorkSpacePath + "config/input", 2000,PortalControl.portalWorkSpacePath + "logs/error.log");
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.write(command);
            bufferedWriter.flush();
            bufferedWriter.close();
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
     * @param path   Path.
     * @param isFile IsFile.If the value is true,it means the file is a file.If the value is false,it means the file is a directory.
     * @return String Valid input String.
     */
    public static boolean createFile(String path, boolean isFile) {
        boolean flag = true;
        File file = new File(path);
        if (!file.exists()) {
            if (isFile) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    flag = false;
                    LOGGER.error("IO exception occurred in creating new file.");
                }
            } else {
                file.mkdirs();
            }
        } else {
            flag = false;
            LOGGER.info("File " + path + " has existed.");
        }
        return flag;
    }

    /**
     * Get package path.
     *
     * @param pkgPath PkgPath parameter.
     * @param pkgName PkgName parameter.
     * @return String Get package path.
     */
    public static String getPackagePath(String pkgPath, String pkgName) {
        Hashtable<String, String> hashtable = PortalControl.toolsConfigParametersTable;
        String path = "";
        String name = "";
        if (PortalControl.noinput) {
            path = hashtable.get(pkgPath);
            name = hashtable.get(pkgName);
        } else {
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
                name = Tools.checkInputString(scanner, Regex.PKG_NAME);
                Tools.changeSinglePropertiesParameter(pkgName, name, PortalControl.toolsConfigPath);
            }
        }
        path += name;
        return path;
    }

    /**
     * Install package.
     *
     * @param lastFilePath     the last file path
     * @param pkgPathParameter PkgPath parameter.
     * @param pkgNameParameter PkgName parameter.
     * @param pathParameter    Path parameter.
     */
    public static void installPackage(String lastFilePath, String pkgPathParameter, String pkgNameParameter, String pathParameter) {
        File file = new File(lastFilePath);
        if (file.exists()) {
            LOGGER.info("File " + lastFilePath + " has existed.");
        } else {
            String packagePath = Tools.getPackagePath(pkgPathParameter, pkgNameParameter);
            Tools.createFile(pathParameter, false);
            RuntimeExecTools.unzipFile(packagePath, pathParameter);
        }
        if (file.exists()) {
            LOGGER.info("Installation of " + pkgNameParameter + " is finished.");
        } else {
            LOGGER.error("Installation of " + pkgNameParameter + " is failed.");
        }
    }

    /**
     * Search available ports.
     *
     * @param size  The size of available port list.
     * @param total The total ports to search.
     * @return List of integer. The list of available port list.
     */
    public static ArrayList<Integer> getAvailablePorts(int size, int total) {
        int tempPort = PortalControl.startPort;
        ArrayList<Integer> list = new ArrayList<>();
        int availablePortNumber = 0;
        for (int i = 0; i < total; i++) {
            if (isPortAvailable("127.0.0.1", tempPort)) {
                list.add(tempPort);
                availablePortNumber++;
                LOGGER.info(String.valueOf(availablePortNumber));
                if (availablePortNumber == size) {
                    PortalControl.startPort = ++tempPort;
                    break;
                }
            }
            tempPort++;
        }
        return list;
    }

    /**
     * Check if the port is available.
     *
     * @param host The test host.
     * @param port The test port.
     * @return List of integer. The list of available port list.
     */
    public static boolean isPortAvailable(String host, int port) {
        boolean flag = true;
        try {
            InetAddress Address = InetAddress.getByName(host);
            Socket socket = new Socket(Address, port);
            flag = false;
            socket.close();
        } catch (UnknownHostException e) {
            LOGGER.error("Unknown host address,Please check host.");
        } catch (IOException e) {
            LOGGER.info("The port " + host + ":" + port + " is available.");
        }
        return flag;
    }

    /**
     * Wait for incremental signal.
     *
     * @param msg the msg
     */
    public static void waitForIncrementalSignal(String msg) {
        try {
            while (true) {
                Thread.sleep(1000);
                if (Plan.runReverseMigration || Plan.runIncrementalMigration || Plan.stopPlan) {
                    LOGGER.info(msg);
                    break;
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted exception waiting for signal.");
        }
    }

    /**
     * Wait for reverse signal.
     *
     * @param msg the msg
     */
    public static void waitForReverseSignal(String msg) {
        try {
            while (true) {
                Thread.sleep(1000);
                if (Plan.runReverseMigration || Plan.stopPlan) {
                    LOGGER.info(msg);
                    break;
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted exception waiting for signal.");
        }
    }

    /**
     * Change migration status.
     *
     * @param command the command
     */
    public static void changeMigrationStatus(String command) {
        switch (command) {
            case Command.Stop.INCREMENTAL_MIGRATION: {
                Plan.stopIncrementalMigration = true;
                Plan.runIncrementalMigration = false;
                break;
            }
            case Command.Stop.REVERSE_MIGRATION: {
                Plan.stopReverseMigration = true;
                Plan.runReverseMigration = false;
                break;
            }
            case Command.Run.INCREMENTAL_MIGRATION: {
                Plan.runIncrementalMigration = true;
                Plan.stopIncrementalMigration = false;
                break;
            }
            case Command.Run.REVERSE_MIGRATION: {
                Plan.runReverseMigration = true;
                Plan.stopReverseMigration = false;
                break;
            }
            case Command.Stop.PLAN: {
                Plan.stopPlan = true;
                break;
            }
            default:
                break;
        }
    }

    /**
     * Change command line parameters.
     */
    public static void changeCommandLineParameters() {
        String checkSinkPath = PortalControl.portalWorkSpacePath + "config/datacheck/application-sink.yml";
        String checkSourcePath = PortalControl.portalWorkSpacePath + "config/datacheck/application-source.yml";
        HashMap<String, Object> checkSinkTable = new HashMap<>();
        checkSinkTable.put("spring.extract.query-dop", Integer.parseInt(getOrDefault(Check.Sink.QUERY_DOP, Default.Check.Sink.QUERY_DOP)));
        checkSinkTable.put("spring.datasource.druid.dataSourceOne.initialSize", Integer.parseInt(getOrDefault(Check.Sink.INITIAL_SIZE, Default.Check.Sink.INITIAL_SIZE)));
        checkSinkTable.put("spring.datasource.druid.dataSourceOne.minIdle", Integer.parseInt(getOrDefault(Check.Sink.MIN_IDLE, Default.Check.Sink.MIN_IDLE)));
        checkSinkTable.put("spring.datasource.druid.dataSourceOne.maxActive", Integer.parseInt(getOrDefault(Check.Sink.MAX_ACTIVE, Default.Check.Sink.MAX_ACTIVE)));
        checkSinkTable.put("spring.extract.debezium-time-period", Integer.parseInt(getOrDefault(Check.Sink.TIME_PERIOD, Default.Check.Sink.TIME_PERIOD)));
        checkSinkTable.put("spring.extract.debezium-num-period", Integer.parseInt(getOrDefault(Check.Sink.NUM_PERIOD, Default.Check.Sink.NUM_PERIOD)));
        Tools.changeYmlParameters(checkSinkTable, checkSinkPath);
        HashMap<String, Object> checkSourceTable = new HashMap<>();
        checkSourceTable.put("spring.extract.query-dop", Integer.parseInt(getOrDefault(Check.Source.QUERY_DOP, Default.Check.Source.QUERY_DOP)));
        checkSourceTable.put("spring.datasource.druid.dataSourceOne.initialSize", Integer.parseInt(getOrDefault(Check.Source.INITIAL_SIZE, Default.Check.Source.INITIAL_SIZE)));
        checkSourceTable.put("spring.datasource.druid.dataSourceOne.minIdle", Integer.parseInt(getOrDefault(Check.Source.MIN_IDLE, Default.Check.Source.MIN_IDLE)));
        checkSourceTable.put("spring.datasource.druid.dataSourceOne.maxActive", Integer.parseInt(getOrDefault(Check.Source.MAX_ACTIVE, Default.Check.Source.MAX_ACTIVE)));
        checkSourceTable.put("spring.extract.debezium-time-period", Integer.parseInt(getOrDefault(Check.Source.TIME_PERIOD, Default.Check.Source.TIME_PERIOD)));
        checkSourceTable.put("spring.extract.debezium-num-period", Integer.parseInt(getOrDefault(Check.Source.NUM_PERIOD, Default.Check.Source.NUM_PERIOD)));
        Tools.changeYmlParameters(checkSourceTable, checkSourcePath);

        Tools.writeCheckRules();
        Tools.writeChameleonOverrideType();

    }

    /**
     * Gets or default.
     *
     * @param parameter    the parameter
     * @param defaultValue the default value
     * @return the or default
     */
    public static String getOrDefault(String parameter, String defaultValue) {
        String value;
        if (System.getProperty(parameter) != null) {
            value = System.getProperty(parameter);
        } else {
            value = defaultValue;
        }
        return value;
    }


    /**
     * Change file.
     *
     * @param oldString the old string
     * @param newString the new string
     * @param path      the path
     */
    public static void changeFile(String oldString, String newString, String path) {
        try {
            String result = "";
            String temp = "";
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            while ((temp = bufferedReader.readLine()) != null) {
                if (temp.contains(oldString)) {
                    temp = temp.replaceFirst(oldString, newString);
                }
                result += temp + System.lineSeparator();
            }
            bufferedReader.close();
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(path));
            bufferedWriter.write(result);
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in changing file " + path + ".");
        }
    }

    /**
     * Change connect xml file.
     *
     * @param workspaceId the workspace id
     * @param path        the path
     */
    public static void changeConnectXmlFile(String workspaceId, String path) {
        try {
            String result = "";
            String temp = "";
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            while ((temp = bufferedReader.readLine()) != null) {
                if (temp.contains("/connect") && temp.contains(".log")) {
                    int start = temp.indexOf("/connect");
                    String connectLogName = temp.substring(start);
                    temp = temp.replace(connectLogName, "/connect_" + workspaceId + ".log");
                }
                result += temp + System.lineSeparator();
            }
            bufferedReader.close();
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(path));
            bufferedWriter.write(result);
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in changing file " + path + ".");
        }
    }

    public static void writeCheckRules() {
        StringBuilder rules = new StringBuilder();
        String rulesTableAmount = getOrDefault(Check.Rules.Table.AMOUNT, String.valueOf(Default.Check.TABLE_AMOUNT));
        int tableAmount = Integer.parseInt(rulesTableAmount);
        if (Boolean.parseBoolean(getOrDefault(Check.Rules.ENABLE, Default.Check.RULES_ENABLE)) && tableAmount != 0) {
            rules.append(rules);
            rules.append("table-parameter:" + System.lineSeparator());
            for (int i = 1; i <= tableAmount; i++) {
                String rulesTableName = System.getProperty(Check.Rules.Table.NAME + i);
                String rulesTableText = System.getProperty(Check.Rules.Table.TEXT + i);
                rules.append("table-name" + i + ":" + rulesTableName + System.lineSeparator());
                rules.append("table-text" + i + ":" + rulesTableText + System.lineSeparator());
            }
            rules.append("row-parameter:" + System.lineSeparator());
            int rulesRowAmount = Integer.parseInt(getOrDefault(Check.Rules.Row.AMOUNT, String.valueOf(Default.Check.ROW_AMOUNT)));
            for (int i = 1; i <= rulesRowAmount; i++) {
                String rulesRowName = System.getProperty(Check.Rules.Row.NAME + i);
                String rulesRowText = System.getProperty(Check.Rules.Row.TEXT + i);
                rules.append("row-name" + i + ":" + rulesRowName + System.lineSeparator());
                rules.append("row-text" + i + ":" + rulesRowText + System.lineSeparator());
            }
            rules.append("column-parameter:" + System.lineSeparator());
            int rulesColumnAmount = Integer.parseInt(getOrDefault(Check.Rules.Row.AMOUNT, String.valueOf(Default.Check.COLUMN_AMOUNT)));
            for (int i = 1; i <= rulesColumnAmount; i++) {
                String rulesColumnName = System.getProperty(Check.Rules.Column.NAME + i);
                String rulesColumnText = System.getProperty(Check.Rules.Column.TEXT + i);
                String rulesColumnAttribute = System.getProperty(Check.Rules.Column.ATTRIBUTE + i);
                rules.append("column-name" + i + ":" + rulesColumnName + System.lineSeparator());
                rules.append("column-text" + i + ":" + rulesColumnText + System.lineSeparator());
                rules.append("column-attribute" + i + ":" + rulesColumnAttribute + System.lineSeparator());
            }
        }
        try {
            Tools.createFile(PortalControl.portalWorkSpacePath + "parameter-datacheck.txt", true);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(PortalControl.portalWorkSpacePath + "parameter-datacheck.txt"));
            bufferedWriter.write(rules.toString());
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in writing parameter");
        }
    }

    public static void writeChameleonOverrideType() {
        StringBuilder rules = new StringBuilder();
        rules.append("chameleon-parameter:" + System.lineSeparator());

        int chameleonOverrideTypeAmount = Integer.parseInt(getOrDefault(Chameleon.Override.AMOUNT, String.valueOf(Default.Chameleon.Override.AMOUNT)));
        for (int i = 0; i <= chameleonOverrideTypeAmount; i++) {
            rules.append("override" + i + ": " + System.lineSeparator());
            String overrideType = System.getProperty(Chameleon.Override.SOURCE_TYPE + i);
            String overrideTo = System.getProperty(Chameleon.Override.SINK_TYPE + i);
            String overrideTables = System.getProperty(Chameleon.Override.TABLES + i);
            rules.append(overrideType + System.lineSeparator());
            rules.append(overrideTo + System.lineSeparator());
            rules.append(overrideTables + System.lineSeparator());
            rules.append(System.lineSeparator());
        }
        try {
            Tools.createFile(PortalControl.portalWorkSpacePath + "parameter-chameleon.txt", true);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(PortalControl.portalWorkSpacePath + "parameter-chameleon.txt"));
            bufferedWriter.write(rules.toString());
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in writing parameter");
        }

    }

    public static ArrayList<TableStatus> getChameleonTableStatus() {
        ArrayList<TableStatus> tableStatusList = new ArrayList<>();
        String chameleonVenvPath = PortalControl.toolsConfigParametersTable.get(Chameleon.VENV_PATH);
        String path = chameleonVenvPath + "data_default_" + Plan.workspaceId + "_init_replica.json";
        String tableChameleonStatus = Tools.readFile(new File(path));
        JSONObject root = JSONObject.parseObject(tableChameleonStatus);
        JSONArray table = root.getJSONArray("table");
        Iterator iterator = table.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            String tableName = table.getJSONObject(i).getString("name");
            double percent = table.getJSONObject(i).getDouble("percent");
            int status = table.getJSONObject(i).getInteger("status");
            if (new File(PortalControl.portalWorkSpacePath + "check_result/result").exists()) {
                File[] fileList = new File(PortalControl.portalWorkSpacePath + "check_result/result").listFiles();
                for (File file1 : fileList) {
                    String fileName = file1.getName();
                    if (fileName.contains("_" + tableName + "_")) {
                        String tableCheckStatus = Tools.readFile(file1);
                        JSONObject tableObject = JSONObject.parseObject(tableCheckStatus);
                        String tableName1 = tableObject.getString("table");
                        String result1 = tableObject.getString("result");
                        if (tableName1.equals(tableName) && status < Status.Object.FULL_MIGRATION_CHECK_FINISHED && result1.equals("success")) {
                            status = Status.Object.FULL_MIGRATION_CHECK_FINISHED;
                        } else if (tableName1.equals(tableName) && status < Status.Object.FULL_MIGRATION_CHECK_FINISHED && result1.equals("failed")) {
                            status = Status.Object.ERROR;
                        }
                    }
                }
            }
            TableStatus tableStatus = new TableStatus(tableName, status, percent);
            tableStatusList.add(tableStatus);
            i++;
            iterator.next();
        }
        return tableStatusList;
    }

    public static ArrayList<ObjectStatus> getChameleonObjectStatus(String name, String order) {
        ArrayList<ObjectStatus> objectStatusList = new ArrayList<>();
        try {
            String chameleonStr = "";
            String chameleonVenvPath = PortalControl.toolsConfigParametersTable.get(Chameleon.VENV_PATH);
            String path = "";
            if (new File(path).exists()) {
                path = chameleonVenvPath + "data_default_" + Plan.workspaceId + "_" + order + ".json";
            } else {
                path = chameleonVenvPath + "data_default_" + Plan.workspaceId + "_init_replica.json";
            }
            BufferedReader fileReader = new BufferedReader((new InputStreamReader(new FileInputStream(path))));
            String tempStr = "";
            while ((tempStr = fileReader.readLine()) != null) {
                chameleonStr += tempStr;
            }
            fileReader.close();
            JSONObject root = JSONObject.parseObject(chameleonStr);
            JSONArray objects = root.getJSONArray(name);
            Iterator iterator = objects.iterator();
            int i = 0;
            if (iterator.hasNext()) {
                String objectName = objects.getJSONObject(i).getString("name");
                int status = objects.getJSONObject(i).getInteger("status");
                ObjectStatus objectStatus = new ObjectStatus(objectName, status);
                objectStatusList.add(objectStatus);
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("File not found exception occurred in get chameleon table status.");
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in get chameleon table status.");
        }
        return objectStatusList;
    }

    public static void changeFullStatus() {
        ArrayList<TableStatus> tableStatusArrayList = Tools.getChameleonTableStatus();
        ArrayList<ObjectStatus> viewStatusArrayList = Tools.getChameleonObjectStatus("view", "start_view_replica");
        ArrayList<ObjectStatus> functionStatusArrayList = Tools.getChameleonObjectStatus("function", "start_func_replica");
        ArrayList<ObjectStatus> triggerStatusArrayList = Tools.getChameleonObjectStatus("trigger", "start_trigger_replica");
        ArrayList<ObjectStatus> procedureStatusArrayList = Tools.getChameleonObjectStatus("procedure", "start_proc_replica");
        FullMigrationStatus fullMigrationStatus = new FullMigrationStatus(tableStatusArrayList, viewStatusArrayList, functionStatusArrayList, triggerStatusArrayList, procedureStatusArrayList);
        String fullMigrationStatusString = JSON.toJSONString(fullMigrationStatus);
        Tools.writeFile(fullMigrationStatusString, new File(PortalControl.portalWorkSpacePath + "status/full_migration.txt"),false);
    }

    public static String readFile(File file) {
        String str = "";
        try {
            BufferedReader fileReader = new BufferedReader((new InputStreamReader(new FileInputStream(file))));
            String tempStr = "";
            while ((tempStr = fileReader.readLine()) != null) {
                str += tempStr;
            }
            fileReader.close();
        } catch (IOException e) {
            LOGGER.info("IO exception occurred in read file " + file.getAbsolutePath());
        }
        return str;

    }

    public static void writeFile(String str, File file,boolean append) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file,append));
            bufferedWriter.write(str);
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            LOGGER.info("IO exception occurred in read file " + file.getAbsolutePath());
        }
    }

    public static void stopPortal(){
        PortalControl.threadCheckProcess.exit = true;
        PortalControl.threadGetOrder.exit = true;
        PortalControl.threadStatusController.exit = true;
    }
}
