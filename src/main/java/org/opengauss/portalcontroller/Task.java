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

import org.opengauss.portalcontroller.constant.Chameleon;
import org.opengauss.portalcontroller.constant.Check;
import org.opengauss.portalcontroller.constant.Command;
import org.opengauss.portalcontroller.constant.Debezium;
import org.opengauss.portalcontroller.constant.Method;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

/**
 * Task
 *
 * @author ：liutong
 * @date ：Created in 2022/12/24
 * @since ：1
 */
public class Task {
    private static HashMap<String, String> taskProcessMap = new HashMap<>();
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Task.class);



    /**
     * All valid task list.
     */
    public static final List<String> ALL_TASK_LIST = Arrays.asList(
            "start mysql full migration",
            "start mysql full migration datacheck",
            "start mysql incremental migration",
            "start mysql incremental migration datacheck",
            "start mysql reverse migration",
            "start mysql reverse migration datacheck"
    );

    /**
     * Get parameter taskProcessMap.This parameter is a map of method name and process name which can be find uniquely.
     */
    public static HashMap<String, String> getTaskProcessMap() {
        return Task.taskProcessMap;
    }

    /**
     * Set parameter taskProcessMap.This parameter is a map of method name and process name which can be find uniquely.
     */
    public static void setTaskProcessMap(HashMap<String, String> map) {
        Task.taskProcessMap = map;
    }

    /**
     * Run task handler hash map.This map contains methods to run tasks.
     */
    public static HashMap<String, PortalControl.EventHandler> runTaskHandlerHashMap = new HashMap<>();
    /**
     * Stop task handler hash map.This map contains methods to stop tasks.
     */
    public static HashMap<String, PortalControl.EventHandler> stopTaskHandlerHashMap = new HashMap<>();

    /**
     * Init parameter taskProcessMap.This parameter is a map of method name and process name which can be find uniquely.
     */
    public static void initTaskProcessMap() {
        HashMap<String, String> tempTaskProcessMap = new HashMap<>();
        String kafkaPath = PortalControl.toolsConfigParametersTable.get(Debezium.Kafka.PATH);
        String confluentPath = PortalControl.toolsConfigParametersTable.get(Debezium.Confluent.PATH);
        tempTaskProcessMap.put(Method.Run.ZOOKEEPER, "QuorumPeerMain " + kafkaPath + "config/zookeeper.properties");
        tempTaskProcessMap.put(Method.Run.KAFKA, "Kafka " + kafkaPath + "config/server.properties");
        tempTaskProcessMap.put(Method.Run.REGISTRY, "SchemaRegistryMain " + confluentPath + "etc/schema-registry/schema-registry.properties");
        tempTaskProcessMap.put(Method.Run.CONNECT, "ConnectStandalone " + confluentPath + "etc/schema-registry/connect-avro-standalone.properties " + confluentPath + "etc/kafka/mysql-source.properties " + confluentPath + "etc/kafka/mysql-sink.properties");
        tempTaskProcessMap.put(Method.Run.REVERSE_CONNECT, "ConnectStandalone " + confluentPath + "etc/schema-registry/connect-avro-standalone.properties " + confluentPath + "etc/kafka/opengauss-source.properties " + confluentPath + "etc/kafka/opengauss-sink.properties");
        tempTaskProcessMap.put(Method.Run.CHECK_SOURCE, "datachecker-extract-0.0.1.jar --spring.profiles.active=source");
        tempTaskProcessMap.put(Method.Run.CHECK_SINK, "datachecker-extract-0.0.1.jar --spring.profiles.active=sink");
        tempTaskProcessMap.put(Method.Run.CHECK, "datachecker-check-0.0.1.jar");
        setTaskProcessMap(tempTaskProcessMap);
    }

    /**
     * Init run task handler hash map.This map contains methods to run tasks.
     */
    public static void initRunTaskHandlerHashMap() {
        runTaskHandlerHashMap.clear();
        Task task = new Task();
        String kafkaPath = PortalControl.toolsConfigParametersTable.get(Debezium.Kafka.PATH);
        String confluentPath = PortalControl.toolsConfigParametersTable.get(Debezium.Confluent.PATH);
        String datacheckPath = PortalControl.toolsConfigParametersTable.get(Check.PATH);
        runTaskHandlerHashMap.put(Method.Run.ZOOKEEPER, (event) -> task.runZookeeper(kafkaPath));
        runTaskHandlerHashMap.put(Method.Run.KAFKA, (event) -> task.runKafka(kafkaPath));
        runTaskHandlerHashMap.put(Method.Run.REGISTRY, (event) -> task.runSchemaRegistry(confluentPath));
        runTaskHandlerHashMap.put(Method.Run.CONNECT, (event) -> task.runKafkaConnect(confluentPath));
        runTaskHandlerHashMap.put(Method.Run.REVERSE_CONNECT, (event) -> task.runReverseKafkaConnect(confluentPath));
        runTaskHandlerHashMap.put(Method.Run.CHECK_SINK, (event) -> task.runDataCheckSink(datacheckPath));
        runTaskHandlerHashMap.put(Method.Run.CHECK_SOURCE, (event) -> task.runDataCheckSource(datacheckPath));
        runTaskHandlerHashMap.put(Method.Run.CHECK, (event) -> task.runDataCheck(datacheckPath));
    }

    /**
     * Init stop task handler hash map.This map contains methods to stop tasks.
     */
    public static void initStopTaskHandlerHashMap() {
        stopTaskHandlerHashMap.clear();
        Task task = new Task();
        String kafkaPath = PortalControl.toolsConfigParametersTable.get(Debezium.Kafka.PATH);
        String confluentPath = PortalControl.toolsConfigParametersTable.get(Debezium.Confluent.PATH);
        stopTaskHandlerHashMap.put(Method.Stop.ZOOKEEPER, (event) -> task.stopZookeeper(kafkaPath));
        stopTaskHandlerHashMap.put(Method.Stop.KAFKA, (event) -> task.stopKafka(kafkaPath));
        stopTaskHandlerHashMap.put(Method.Stop.REGISTRY, (event) -> task.stopKafkaSchema(confluentPath));
        stopTaskHandlerHashMap.put(Method.Stop.CONNECT, (event) -> task.stopKafkaConnect(confluentPath));
        stopTaskHandlerHashMap.put(Method.Stop.REVERSE_CONNECT, (event) -> task.stopReverseKafkaConnect(confluentPath));
        stopTaskHandlerHashMap.put(Method.Stop.CHECK_SINK, (event) -> task.stopDataCheckSink());
        stopTaskHandlerHashMap.put(Method.Stop.CHECK_SOURCE, (event) -> task.stopDataCheckSource());
        stopTaskHandlerHashMap.put(Method.Stop.CHECK, (event) -> task.stopDataCheck());
    }

    /**
     * Start task method.A method to start task.
     *
     * @param methodName Task name.
     */
    public static void startTaskMethod(String methodName) {
        if (Plan.stopPlan) {
            return;
        }
        if (taskProcessMap.containsKey(methodName)) {
            String methodProcessName = taskProcessMap.get(methodName);
            int pid = Tools.getCommandPid(methodProcessName);
            List<RunningTaskThread> runningTaskThreadThreadList = Plan.getRunningTaskThreadsList();
            RunningTaskThread runningTaskThread = new RunningTaskThread(methodName, methodProcessName);
            if (pid == -1) {
                runningTaskThread.startTask();
                runningTaskThread.setPid(Tools.getCommandPid(methodProcessName));
                runningTaskThreadThreadList.add(runningTaskThread);
                Plan.setRunningTaskThreadsList(runningTaskThreadThreadList);
                try {
                    Thread.sleep(8000);
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted exception occurred in starting task.");
                }
            } else if (runningTaskThreadThreadList.contains(runningTaskThread)) {
                LOGGER.info(methodName + " has started.");
            } else {
                LOGGER.info(methodName + " has started.");
                runningTaskThread.setPid(Tools.getCommandPid(methodProcessName));
            }
        }
    }

    /**
     * Execute chameleon order.
     *
     * @param chameleonVenvPath The virtual environment which installed chameleon path.
     * @param order             Chameleon order.
     * @param parametersTable   Parameters table.
     */
    public void useChameleonReplicaOrder(String chameleonVenvPath, String order, Hashtable<String, String> parametersTable) {
        if (Plan.stopPlan) {
            return;
        }
        StringBuilder chameleonOrder = new StringBuilder(chameleonVenvPath + "venv/bin/chameleon " + order + " ");
        for (String key : parametersTable.keySet()) {
            chameleonOrder.append(key).append(" ").append(parametersTable.get(key)).append(" ");
        }
        try {
            RuntimeExecTools.executeOrder(chameleonOrder.toString(), 3000);
            String userHome = System.getProperty("user.home");
            String chameleonConfigPath = PortalControl.toolsConfigParametersTable.get(Chameleon.PATH).replaceFirst
                    ("~", userHome).concat("configuration/default.yml");
            String logDir = "";
            if (parametersTable.containsKey("--source")) {
                logDir = "default_" + parametersTable.get("--source") + ".log";
            } else {
                logDir = "default_general.log";
            }
            String chameleonLogMysqlDir = Tools.getSingleYmlParameter("log_dir", chameleonConfigPath).replaceFirst
                    ("~", userHome).concat(logDir);
            String endFlag = order + " finished";
            while (true) {
                Thread.sleep(1000);
                LOGGER.info(order + " running");
                if (Tools.lastLine(chameleonLogMysqlDir).contains(endFlag)) {
                    break;
                }
            }
            LOGGER.info(order + " finished");
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted exception occurred in starting task.");
        }
    }

    /**
     * Run zookeeper.
     *
     * @param path Path.
     */
    public void runZookeeper(String path) {
        RuntimeExecTools.executeOrder(path + "bin/zookeeper-server-start.sh -daemon " + path + "config/zookeeper.properties", 3000);
        LOGGER.info("Start zookeeper.");
    }

    /**
     * Stop zookeeper.
     *
     * @param path Path.
     */
    public void stopZookeeper(String path) {
        RuntimeExecTools.executeOrder(path + "bin/zookeeper-server-stop.sh " + path + "config/zookeeper.properties", 3000);
        LOGGER.info("Stop zookeeper.");
    }

    /**
     * Run kafka.
     *
     * @param path Path.
     */
    public void runKafka(String path) {
        RuntimeExecTools.executeOrder(path + "bin/kafka-server-start.sh -daemon " + path + "config/server.properties", 2000);
        LOGGER.info("Start kafka.");
    }

    /**
     * Stop kafka.
     *
     * @param path Path.
     */
    public void stopKafka(String path) {
        RuntimeExecTools.executeOrder(path + "bin/kafka-server-stop.sh " + path + "config/server.properties", 3000);
        LOGGER.info("Stop kafka.");
    }

    /**
     * Run kafka schema registry.
     *
     * @param path Path.
     */
    public void runSchemaRegistry(String path) {
        RuntimeExecTools.executeOrder(path + "bin/schema-registry-start -daemon " + path + "etc/schema-registry/schema-registry.properties", 3000);
        LOGGER.info("Start kafkaSchemaRegistry.");
    }

    /**
     * Stop kafka schema registry.
     *
     * @param path Path.
     */
    public void stopKafkaSchema(String path) {
        RuntimeExecTools.executeOrder(path + "bin/schema-registry-stop " + path + "etc/schema-registry/schema-registry.properties", 3000);
        LOGGER.info("Stop kafkaSchemaRegistry.");
    }

    /**
     * Run kafka connect.
     *
     * @param path Path.
     */
    public void runKafkaConnect(String path) {
        String configUrl = Tools.getSinglePropertiesParameter("key.converter.schema.registry.url", path + "etc/schema-registry/connect-avro-standalone.properties");
        configUrl += "/config";
        RuntimeExecTools.executeOrder("curl -X PUT -H \"Content-Type: application/vnd.schemaregistry.v1+json\" --data '{\"compatibility\": \"NONE\"}' " + configUrl, 3000);
        RuntimeExecTools.executeOrder(path + "bin/connect-standalone -daemon " + path + "etc/schema-registry/connect-avro-standalone.properties " + path + "etc/kafka/mysql-source.properties " + path + "etc/kafka/mysql-sink.properties", 3000);
        LOGGER.info("Start kafkaConnector.");
    }

    /**
     * Run reverse kafka connect.
     *
     * @param path Path.
     */
    public void runReverseKafkaConnect(String path) {
        RuntimeExecTools.executeOrder(path + "bin/connect-standalone -daemon " + path + "etc/schema-registry/connect-avro-standalone.properties " + path + "etc/kafka/opengauss-source.properties " + path + "etc/kafka/opengauss-sink.properties", 5000);
        LOGGER.info("Start reverseKafkaConnect.");
    }

    /**
     * Stop kafka connect.
     *
     * @param path Path.
     */
    public void stopKafkaConnect(String path) {
        int pid = Tools.getCommandPid(taskProcessMap.get(Method.Run.CONNECT));
        if (pid != -1) {
            RuntimeExecTools.executeOrder("kill -15 " + pid, 2000);
        }
        LOGGER.info("Stop kafkaConnect.");
    }

    /**
     * Stop reverse kafka connect.
     *
     * @param path Path.
     */
    public void stopReverseKafkaConnect(String path) {
        int pid = Tools.getCommandPid(taskProcessMap.get(Method.Run.REVERSE_CONNECT));
        if (pid != -1) {
            RuntimeExecTools.executeOrder("kill -15 " + pid, 2000);
        }
        LOGGER.info("Stop reverseKafkaConnect.");
    }

    /**
     * Get data from sink database to datacheck.
     *
     * @param path Path.
     */
    public void runDataCheckSink(String path) {
        RuntimeExecTools.executeOrder("nohup java -Dspring.config.additional-location=" + path + "config/application-sink.yml -jar " + path + "datachecker-extract-0.0.1.jar --spring.profiles.active=sink > " + path + "logs/sink.log 2>&1 &", 3000);
        LOGGER.info("Start datacheck sink.");
    }

    /**
     * Get data from source database to datacheck.
     *
     * @param path Path.
     */
    public void runDataCheckSource(String path) {
        RuntimeExecTools.executeOrder("nohup java -Dspring.config.additional-location=" + path + "config/application-source.yml -jar " + path + "datachecker-extract-0.0.1.jar --spring.profiles.active=source > " + path + "logs/source.log 2>&1 &", 3000);
        LOGGER.info("Start datacheck source.");
    }

    /**
     * Run datacheck.
     *
     * @param path Path.
     */
    public void runDataCheck(String path) {
        RuntimeExecTools.executeOrder("nohup java -Dspring.config.additional-location=" + path + "config/application.yml -jar " + path + "datachecker-check-0.0.1.jar > " + path + "logs/checkResult.log 2>&1 &", 3000);
        LOGGER.info("Start datacheck.");
    }

    /**
     * Stop datacheck.
     */
    public void stopDataCheck() {
        int pid = -1;
        pid = Tools.getCommandPid("datachecker-check-0.0.1.jar");
        if (pid != -1) {
            RuntimeExecTools.executeOrder("kill -15 " + pid, 3000);
        }
        LOGGER.info("Stop datacheck.");
    }

    /**
     * Stop getting data from sink database to datacheck.
     */
    public void stopDataCheckSink() {
        int pid = -1;
        pid = Tools.getCommandPid("datachecker-extract-0.0.1.jar --spring.profiles.active=sink");
        if (pid != -1) {
            RuntimeExecTools.executeOrder("kill -15 " + pid, 3000);
        }
        LOGGER.info("Stop datacheck sink.");
    }

    /**
     * Stop getting data from source database to datacheck.
     */
    public void stopDataCheckSource() {
        int pid = -1;
        pid = Tools.getCommandPid("datachecker-extract-0.0.1.jar --spring.profiles.active=source");
        if (pid != -1) {
            RuntimeExecTools.executeOrder("kill -15 " + pid, 3000);
        }
        LOGGER.info("Stop datacheck source.");
    }

    /**
     * Check plan to sure we can execute the plan.
     *
     * @param taskList Task list.
     * @return flag Boolean parameter to express a plan is valid.
     */
    public static boolean checkPlan(List<String> taskList) {
        if (taskList != null) {
            if (taskList.size() == 0) {
                LOGGER.error("No task in plan.Please check the plan.");
                return false;
            } else if (taskList.size() == 1) {
                if (!ALL_TASK_LIST.contains(taskList.get(0))) {
                    LOGGER.error("The task is not valid.");
                    return false;
                } else {
                    return true;
                }
            } else {
                List<String> existingTaskList = new ArrayList<>();
                for (String task : taskList) {
                    if (!ALL_TASK_LIST.contains(task)) {
                        LOGGER.error("The task is not valid.");
                        return false;
                    }
                    if (existingTaskList.contains(task)) {
                        LOGGER.error("The task has existed.");
                        return false;
                    }
                    if (!checkDatacheckType(taskList, task)) {
                        LOGGER.error("There must be the same type of migration before datacheck.");
                        return false;
                    }
                    existingTaskList.add(task);
                }
            }
            if (!checkMigrationSequence(taskList)) {
                LOGGER.error("Please set tasks in a particular sequence.");
                return false;
            }
        } else {
            LOGGER.error("The taskList is null.");
            return false;
        }
        return true;
    }

    private static boolean checkMigrationSequence(List<String> taskList) {
        Hashtable<String, Integer> strTable = new Hashtable<>();
        strTable.put(Command.Start.Mysql.FULL, 1);
        strTable.put(Command.Start.Mysql.FULL_CHECK, 2);
        strTable.put(Command.Start.Mysql.INCREMENTAL, 3);
        strTable.put(Command.Start.Mysql.INCREMENTAL_CHECK, 4);
        strTable.put(Command.Start.Mysql.REVERSE, 5);
        strTable.put(Command.Start.Mysql.REVERSE_CHECK, 6);
        int temp = 0;
        for (String task : taskList) {
            if (strTable.get(task) < temp) {
                return false;
            }
            temp = strTable.get(task);
        }
        return true;
    }

    private static boolean checkDatacheckType(List<String> taskList, String task) {
        if (task.contains("datacheck")) {
            int index = taskList.indexOf(task);
            if (index == 0) {
                return false;
            }
            String migrationOrder = taskList.get(taskList.indexOf(task) - 1);
            String datacheckType = task.replace(" datacheck", "");
            if (!migrationOrder.equals(datacheckType)) {
                return false;
            }
        }
        return true;
    }
}

