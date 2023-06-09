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

import org.opengauss.portalcontroller.check.CheckTaskFullDatacheck;
import org.opengauss.portalcontroller.check.CheckTaskIncrementalDatacheck;
import org.opengauss.portalcontroller.check.CheckTaskIncrementalMigration;
import org.opengauss.portalcontroller.check.CheckTaskMysqlFullMigration;
import org.opengauss.portalcontroller.check.CheckTaskReverseDatacheck;
import org.opengauss.portalcontroller.check.CheckTaskReverseMigration;
import org.opengauss.portalcontroller.constant.Chameleon;
import org.opengauss.portalcontroller.constant.Check;
import org.opengauss.portalcontroller.constant.Command;
import org.opengauss.portalcontroller.constant.Debezium;
import org.opengauss.portalcontroller.constant.Method;
import org.opengauss.portalcontroller.constant.Parameter;
import org.opengauss.portalcontroller.constant.Regex;
import org.opengauss.portalcontroller.constant.Status;
import org.opengauss.portalcontroller.status.PortalStatusWriter;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;


import java.io.File;
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
    private static HashMap<String, String> taskLogMap = new HashMap<>();

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
     *
     * @return the task process map
     */
    public static HashMap<String, String> getTaskProcessMap() {
        return Task.taskProcessMap;
    }

    /**
     * Set parameter taskProcessMap.This parameter is a map of method name and process name which can be find uniquely.
     *
     * @param map the map
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
     * Gets task log map.
     *
     * @return the task log map
     */
    public static HashMap<String, String> getTaskLogMap() {
        return taskLogMap;
    }

    /**
     * Sets task log map.
     *
     * @param taskLogMap the task log map
     */
    public static void setTaskLogMap(HashMap<String, String> taskLogMap) {
        Task.taskLogMap = taskLogMap;
    }

    /**
     * Init parameter taskProcessMap.This parameter is a map of method name and process name which can be find uniquely.
     */
    public static void initTaskProcessMap() {
        HashMap<String, String> tempTaskProcessMap = new HashMap<>();
        String kafkaPath = PortalControl.toolsConfigParametersTable.get(Debezium.Kafka.PATH);
        String confluentPath = PortalControl.toolsConfigParametersTable.get(Debezium.Confluent.PATH);
        String datacheckPath = PortalControl.toolsConfigParametersTable.get(Check.PATH);
        tempTaskProcessMap.put(Method.Run.ZOOKEEPER, "QuorumPeerMain " + kafkaPath + "config/zookeeper.properties");
        tempTaskProcessMap.put(Method.Run.KAFKA, "Kafka " + kafkaPath + "config/server.properties");
        tempTaskProcessMap.put(Method.Run.REGISTRY, "SchemaRegistryMain " + confluentPath + "etc/schema-registry/schema-registry.properties");
        tempTaskProcessMap.put(Method.Run.CONNECT_SOURCE, "ConnectStandalone " + PortalControl.portalWorkSpacePath + "config/debezium/connect-avro-standalone-source.properties " + PortalControl.portalWorkSpacePath + "config/debezium/mysql-source.properties");
        tempTaskProcessMap.put(Method.Run.CONNECT_SINK, "ConnectStandalone " + PortalControl.portalWorkSpacePath + "config/debezium/connect-avro-standalone-sink.properties " + PortalControl.portalWorkSpacePath + "config/debezium/mysql-sink.properties");
        tempTaskProcessMap.put(Method.Run.REVERSE_CONNECT_SOURCE, "ConnectStandalone " + PortalControl.portalWorkSpacePath + "config/debezium/connect-avro-standalone-reverse-source.properties " + PortalControl.portalWorkSpacePath + "config/debezium/opengauss-source.properties");
        tempTaskProcessMap.put(Method.Run.REVERSE_CONNECT_SINK, "ConnectStandalone " + PortalControl.portalWorkSpacePath + "config/debezium/connect-avro-standalone-reverse-sink.properties " + PortalControl.portalWorkSpacePath + "config/debezium/opengauss-sink.properties");
        tempTaskProcessMap.put(Method.Run.CHECK_SOURCE, "java -Dspring.config.additional-location=" + PortalControl.portalWorkSpacePath + "config/datacheck/application-source.yml -jar " + datacheckPath + "datachecker-extract-0.0.1.jar --spring.profiles.active=source");
        tempTaskProcessMap.put(Method.Run.CHECK_SINK, "java -Dspring.config.additional-location=" + PortalControl.portalWorkSpacePath + "config/datacheck/application-sink.yml -jar " + datacheckPath + "datachecker-extract-0.0.1.jar --spring.profiles.active=sink");
        tempTaskProcessMap.put(Method.Run.CHECK, "java -Dspring.config.additional-location=" + PortalControl.portalWorkSpacePath + "config/datacheck/application.yml -jar " + datacheckPath + "datachecker-check-0.0.1.jar");
        setTaskProcessMap(tempTaskProcessMap);
    }

    /**
     * Init task log map.
     */
    public static void initTaskLogMap() {
        HashMap<String, String> tempTaskLogMap = new HashMap<>();
        tempTaskLogMap.put(Method.Run.ZOOKEEPER, PortalControl.portalWorkSpacePath + "logs/debezium/server.log");
        tempTaskLogMap.put(Method.Run.KAFKA, PortalControl.portalWorkSpacePath + "logs/debezium/server.log");
        tempTaskLogMap.put(Method.Run.REGISTRY, PortalControl.portalWorkSpacePath + "logs/debezium/schema-registry.log");
        tempTaskLogMap.put(Method.Run.CONNECT_SOURCE, PortalControl.portalWorkSpacePath + "logs/debezium/connect_source.log");
        tempTaskLogMap.put(Method.Run.CONNECT_SINK, PortalControl.portalWorkSpacePath + "logs/debezium/connect_sink.log");
        tempTaskLogMap.put(Method.Run.REVERSE_CONNECT_SOURCE, PortalControl.portalWorkSpacePath + "logs/debezium/reverse_connect_source.log");
        tempTaskLogMap.put(Method.Run.REVERSE_CONNECT_SINK, PortalControl.portalWorkSpacePath + "logs/debezium/reverse_connect_sink.log");
        tempTaskLogMap.put(Method.Run.CHECK_SOURCE, PortalControl.portalWorkSpacePath + "logs/datacheck/source.log");
        tempTaskLogMap.put(Method.Run.CHECK_SINK, PortalControl.portalWorkSpacePath + "logs/datacheck/sink.log");
        tempTaskLogMap.put(Method.Run.CHECK, PortalControl.portalWorkSpacePath + "logs/datacheck/check.log");
        setTaskLogMap(tempTaskLogMap);
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
        runTaskHandlerHashMap.put(Method.Run.CONNECT_SOURCE, (event) -> task.runKafkaConnectSource(confluentPath));
        runTaskHandlerHashMap.put(Method.Run.CONNECT_SINK, (event) -> task.runKafkaConnectSink(confluentPath));
        runTaskHandlerHashMap.put(Method.Run.REVERSE_CONNECT_SOURCE, (event) -> task.runReverseKafkaConnectSource(confluentPath));
        runTaskHandlerHashMap.put(Method.Run.REVERSE_CONNECT_SINK, (event) -> task.runReverseKafkaConnectSink(confluentPath));
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
        stopTaskHandlerHashMap.put(Method.Stop.CONNECT_SOURCE, (event) -> task.stopKafkaConnectSource());
        stopTaskHandlerHashMap.put(Method.Stop.CONNECT_SINK, (event) -> task.stopKafkaConnectSink());
        stopTaskHandlerHashMap.put(Method.Stop.REVERSE_CONNECT_SOURCE, (event) -> task.stopReverseKafkaConnectSource());
        stopTaskHandlerHashMap.put(Method.Stop.REVERSE_CONNECT_SINK, (event) -> task.stopReverseKafkaConnectSink());
        stopTaskHandlerHashMap.put(Method.Stop.CHECK_SINK, (event) -> task.stopDataCheckSink());
        stopTaskHandlerHashMap.put(Method.Stop.CHECK_SOURCE, (event) -> task.stopDataCheckSource());
        stopTaskHandlerHashMap.put(Method.Stop.CHECK, (event) -> task.stopDataCheck());
    }

    /**
     * Start task method.A method to start task.
     *
     * @param methodName Task name.
     * @param sleepTime  the sleep time
     */
    public static void startTaskMethod(String methodName, int sleepTime) {
        if (Plan.stopPlan) {
            return;
        }
        if (taskProcessMap.containsKey(methodName)) {
            String methodProcessName = taskProcessMap.get(methodName);
            int pid = Tools.getCommandPid(methodProcessName);
            List<RunningTaskThread> runningTaskThreadThreadList = Plan.getRunningTaskThreadsList();
            String logPath = taskLogMap.get(methodName);
            RunningTaskThread runningTaskThread = new RunningTaskThread(methodName, methodProcessName, logPath);
            if (pid == -1) {
                runningTaskThread.startTask();
                runningTaskThread.setPid(Tools.getCommandPid(methodProcessName));
                runningTaskThreadThreadList.add(runningTaskThread);
                Plan.setRunningTaskThreadsList(runningTaskThreadThreadList);
                Tools.sleepThread(sleepTime, "starting task");
            } else if (runningTaskThreadThreadList.contains(runningTaskThread)) {
                Tools.sleepThread(sleepTime, "starting task");
                LOGGER.info(methodName + " has started.");
            } else {
                Tools.sleepThread(sleepTime, "starting task");
                LOGGER.info(methodName + " has started.");
                runningTaskThread.setPid(Tools.getCommandPid(methodProcessName));
            }
        }
    }

    /**
     * Stop task method.
     *
     * @param methodName the method name
     */
    public static void stopTaskMethod(String methodName) {
        String methodProcessName = taskProcessMap.get(methodName);
        int pid = Tools.getCommandPid(methodProcessName);
        List<RunningTaskThread> runningTaskThreadThreadList = Plan.getRunningTaskThreadsList();
        int index = -1;
        for (RunningTaskThread runningTaskThread : runningTaskThreadThreadList) {
            if (runningTaskThread.getMethodName().equals(methodName)) {
                runningTaskThread.stopTask();
                index = runningTaskThreadThreadList.indexOf(runningTaskThread);
                break;
            }
        }
        if (index != -1) {
            runningTaskThreadThreadList.remove(index);
        }
        Plan.setRunningTaskThreadsList(runningTaskThreadThreadList);
    }

    /**
     * Use chameleon replica order.
     *
     * @param chameleonVenvPath the chameleon venv path
     * @param order             the order
     * @param parametersTable   the parameters table
     */
    public void useChameleonReplicaOrder(String chameleonVenvPath, String order, Hashtable<String, String> parametersTable) {
        startChameleonReplicaOrder(chameleonVenvPath, order, parametersTable);
        checkChameleonReplicaOrder(order);
    }

    /**
     * Execute chameleon order.
     *
     * @param chameleonVenvPath The virtual environment which installed chameleon path.
     * @param order             Chameleon order.
     * @param parametersTable   Parameters table.
     */
    public void startChameleonReplicaOrder(String chameleonVenvPath, String order, Hashtable<String, String> parametersTable) {
        if (Plan.stopPlan && !order.equals("drop_replica_schema")) {
            return;
        }
        String chameleonOrder = Tools.jointChameleonOrders(parametersTable, order);
        RuntimeExecTools.executeOrder(chameleonOrder, 2000, chameleonVenvPath, PortalControl.portalWorkSpacePath + "logs/full_migration.log");
    }

    /**
     * Check chameleon replica order.
     *
     * @param order the order
     */
    public void checkChameleonReplicaOrder(String order) {
        if (Plan.stopPlan && !order.equals("drop_replica_schema")) {
            return;
        }
        String endFlag = order + " finished";
        while (!Plan.stopPlan) {
            Tools.sleepThread(1000, "starting task");
            String processString = "chameleon " + order + " --config default_" + Plan.workspaceId;
            LOGGER.info(order + " running");
            boolean processQuit = Tools.getCommandPid(processString) == -1;
            boolean finished = Tools.lastLine(PortalControl.portalWorkSpacePath + "logs/full_migration.log").contains(endFlag);
            if (processQuit && finished) {
                LOGGER.info(order + " finished");
                break;
            } else if (processQuit) {
                LOGGER.error("Error message: Process " + processString + " exit abnormally.Please read " + PortalControl.portalWorkSpacePath + "logs/full_migration.log or error.log to get information.");
                PortalControl.status = Status.ERROR;
                PortalControl.errorMsg = Tools.readFileNotMatchesRegex(new File(PortalControl.portalWorkSpacePath + "logs/full_migration.log"), Regex.CHAMELEON_LOG);
                LOGGER.warn(PortalControl.errorMsg);
                Plan.stopPlan = true;
                break;
            }
        }
    }

    /**
     * Run zookeeper.
     *
     * @param path Path.
     */
    public void runZookeeper(String path) {
        RuntimeExecTools.executeOrder(path + "bin/zookeeper-server-start.sh -daemon " + path + "config/zookeeper.properties", 3000, PortalControl.portalWorkSpacePath + "logs/error.log");
        LOGGER.info("Start zookeeper.");
    }

    /**
     * Stop zookeeper.
     *
     * @param path Path.
     */
    public void stopZookeeper(String path) {
        String order = path + "bin/zookeeper-server-stop.sh " + path + "config/zookeeper.properties";
        String executeFile = path + "bin/zookeeper-server-stop.sh";
        Tools.stopPublicSoftware(Method.Run.ZOOKEEPER, executeFile, order, "zookeeper");
    }

    /**
     * Run kafka.
     *
     * @param path Path.
     */
    public void runKafka(String path) {
        RuntimeExecTools.executeOrder(path + "bin/kafka-server-start.sh -daemon " + path + "config/server.properties", 8000, PortalControl.portalWorkSpacePath + "logs/error.log");
        LOGGER.info("Start kafka.");
    }

    /**
     * Stop kafka.
     *
     * @param path Path.
     */
    public void stopKafka(String path) {
        String order = path + "bin/kafka-server-stop.sh " + path + "config/server.properties";
        String executeFile = path + "bin/kafka-server-stop.sh";
        Tools.stopPublicSoftware(Method.Run.KAFKA, executeFile, order, "kafka");
    }

    /**
     * Run kafka schema registry.
     *
     * @param path Path.
     */
    public void runSchemaRegistry(String path) {
        RuntimeExecTools.executeOrder(path + "bin/schema-registry-start -daemon " + path + "etc/schema-registry/schema-registry.properties", 3000, PortalControl.portalWorkSpacePath + "logs/error.log");
        LOGGER.info("Start kafka schema registry.");
    }

    /**
     * Stop kafka schema registry.
     *
     * @param path Path.
     */
    public void stopKafkaSchema(String path) {
        String order = path + "bin/schema-registry-stop " + path + "etc/schema-registry/schema-registry.properties";
        String executeFile = path + "bin/schema-registry-stop";
        Tools.stopPublicSoftware(Method.Run.REGISTRY, executeFile, order, "kafka schema registry");
    }

    /**
     * Run kafka connect source.
     *
     * @param path Path.
     */
    public void runKafkaConnectSource(String path) {
        Tools.runCurl(PortalControl.portalWorkSpacePath + "curl.log", PortalControl.portalWorkSpacePath + "config/debezium/connect-avro-standalone-source.properties");
        RuntimeExecTools.executeOrder(path + "bin/connect-standalone -daemon " + PortalControl.portalWorkSpacePath + "config/debezium/connect-avro-standalone-source.properties " + PortalControl.portalWorkSpacePath + "config/debezium/mysql-source.properties", 3000, PortalControl.portalWorkSpacePath + "logs/error.log");
        LOGGER.info("Start mysql connector source.");
    }

    /**
     * Run kafka connect sink.
     *
     * @param path Path.
     */
    public void runKafkaConnectSink(String path) {
        RuntimeExecTools.executeOrder(path + "bin/connect-standalone -daemon " + PortalControl.portalWorkSpacePath + "config/debezium/connect-avro-standalone-sink.properties " + PortalControl.portalWorkSpacePath + "config/debezium/mysql-sink.properties", 3000, PortalControl.portalWorkSpacePath + "logs/error.log");
        LOGGER.info("Start mysql connector sink.");
    }

    /**
     * Run reverse kafka connect source.
     *
     * @param path the path
     */
    public void runReverseKafkaConnectSource(String path) {
        Tools.runCurl(PortalControl.portalWorkSpacePath + "curl-reverse.log", PortalControl.portalWorkSpacePath + "config/debezium/connect-avro-standalone-reverse-source.properties");
        RuntimeExecTools.executeOrder(path + "bin/connect-standalone -daemon " + PortalControl.portalWorkSpacePath + "config/debezium/connect-avro-standalone-reverse-source.properties " + PortalControl.portalWorkSpacePath + "config/debezium/opengauss-source.properties", 5000, PortalControl.portalWorkSpacePath + "logs/error.log");
        LOGGER.info("Start opengauss connector source.");
    }


    /**
     * Run reverse kafka connect sink.
     *
     * @param path the path
     */
    public void runReverseKafkaConnectSink(String path) {
        RuntimeExecTools.executeOrder(path + "bin/connect-standalone -daemon " + PortalControl.portalWorkSpacePath + "config/debezium/connect-avro-standalone-reverse-sink.properties " + PortalControl.portalWorkSpacePath + "config/debezium/opengauss-sink.properties", 5000, PortalControl.portalWorkSpacePath + "logs/error.log");
        LOGGER.info("Start opengauss connector sink.");
    }

    /**
     * Stop kafka connect.
     */
    public void stopKafkaConnectSource() {
        Tools.stopExclusiveSoftware(Method.Run.CONNECT_SOURCE, Parameter.MYSQL_CONNECTOR_SOURCE_NAME);
    }

    /**
     * Stop kafka connect sink.
     */
    public void stopKafkaConnectSink() {
        Tools.stopExclusiveSoftware(Method.Run.CONNECT_SINK, Parameter.MYSQL_CONNECTOR_SINK_NAME);
    }

    /**
     * Stop reverse kafka connect.
     */
    public void stopReverseKafkaConnectSource() {
        Tools.stopExclusiveSoftware(Method.Run.REVERSE_CONNECT_SOURCE, Parameter.OPENGAUSS_CONNECTOR_SOURCE_NAME);
    }

    /**
     * Stop reverse kafka connect sink.
     */
    public void stopReverseKafkaConnectSink() {
        Tools.stopExclusiveSoftware(Method.Run.REVERSE_CONNECT_SINK, Parameter.OPENGAUSS_CONNECTOR_SINK_NAME);
    }

    /**
     * Get data from sink database to datacheck.
     *
     * @param path Path.
     */
    public void runDataCheckSink(String path) {
        RuntimeExecTools.executeOrder("nohup java -Dspring.config.additional-location=" + PortalControl.portalWorkSpacePath + "config/datacheck/application-sink.yml -jar " + path + "datachecker-extract-0.0.1.jar --spring.profiles.active=sink > " + PortalControl.portalWorkSpacePath + "logs/sink.log 2>&1 &", 5000, PortalControl.portalWorkSpacePath + "logs/error.log");
        LOGGER.info("Start datacheck sink.");
    }

    /**
     * Get data from source database to datacheck.
     *
     * @param path Path.
     */
    public void runDataCheckSource(String path) {
        RuntimeExecTools.executeOrder("nohup java -Dspring.config.additional-location=" + PortalControl.portalWorkSpacePath + "config/datacheck/application-source.yml -jar " + path + "datachecker-extract-0.0.1.jar --spring.profiles.active=source > " + PortalControl.portalWorkSpacePath + "logs/source.log 2>&1 &", 5000, PortalControl.portalWorkSpacePath + "logs/error.log");
        LOGGER.info("Start datacheck source.");
    }

    /**
     * Run datacheck.
     *
     * @param path Path.
     */
    public void runDataCheck(String path) {
        RuntimeExecTools.executeOrder("nohup java -Dspring.config.additional-location=" + PortalControl.portalWorkSpacePath + "config/datacheck/application.yml -jar " + path + "datachecker-check-0.0.1.jar > " + PortalControl.portalWorkSpacePath + "logs/checkResult.log 2>&1 &", 5000, PortalControl.portalWorkSpacePath + "logs/error.log");
        LOGGER.info("Start datacheck.");
    }

    /**
     * Stop datacheck.
     */
    public void stopDataCheck() {
        int pid = -1;
        pid = Tools.getCommandPid(taskProcessMap.get(Method.Run.CHECK));
        if (pid != -1) {
            RuntimeExecTools.executeOrder("kill -9 " + pid, 3000, PortalControl.portalWorkSpacePath + "logs/error.log");
        }
        for (RunningTaskThread runningTaskThread : Plan.getRunningTaskThreadsList()) {
            if (runningTaskThread.getMethodName().equals(Method.Run.CHECK)) {
                LOGGER.info("Stop datacheck.");
                break;
            }
        }

    }

    /**
     * Stop getting data from sink database to datacheck.
     */
    public void stopDataCheckSink() {
        int pid = -1;
        pid = Tools.getCommandPid(taskProcessMap.get(Method.Run.CHECK_SINK));
        if (pid != -1) {
            RuntimeExecTools.executeOrder("kill -9 " + pid, 3000, PortalControl.portalWorkSpacePath + "logs/error.log");
        }
        for (RunningTaskThread runningTaskThread : Plan.getRunningTaskThreadsList()) {
            if (runningTaskThread.getMethodName().equals(Method.Run.CHECK_SINK)) {
                LOGGER.info("Stop datacheck sink.");
                break;
            }
        }
    }

    /**
     * Stop getting data from source database to datacheck.
     */
    public void stopDataCheckSource() {
        int pid = -1;
        pid = Tools.getCommandPid(taskProcessMap.get(Method.Run.CHECK_SOURCE));
        if (pid != -1) {
            RuntimeExecTools.executeOrder("kill -9 " + pid, 3000, PortalControl.portalWorkSpacePath + "logs/error.log");
        }
        for (RunningTaskThread runningTaskThread : Plan.getRunningTaskThreadsList()) {
            if (runningTaskThread.getMethodName().equals(Method.Run.CHECK_SOURCE)) {
                LOGGER.info("Stop datacheck source.");
                break;
            }
        }
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
                        LOGGER.error("The task already exists.");
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
            addCheckTask(taskList);
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

    private static void addCheckTask(List<String> taskList) {
        for (String task : taskList) {
            switch (task) {
                case "start mysql full migration": {
                    Plan.checkTaskList.add(new CheckTaskMysqlFullMigration());
                    break;
                }
                case "start mysql full migration datacheck": {
                    Plan.checkTaskList.add(new CheckTaskFullDatacheck());
                    break;
                }
                case "start mysql incremental migration": {
                    Plan.checkTaskList.add(new CheckTaskIncrementalMigration());
                    break;
                }
                case "start mysql incremental migration datacheck": {
                    Plan.checkTaskList.add(new CheckTaskIncrementalDatacheck());
                    break;
                }
                case "start mysql reverse migration": {
                    Plan.checkTaskList.add(new CheckTaskReverseMigration());
                    break;
                }
                case "start mysql reverse migration datacheck": {
                    Plan.checkTaskList.add(new CheckTaskReverseDatacheck());
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }

}

