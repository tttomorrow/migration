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
import org.opengauss.portalcontroller.constant.MigrationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.opengauss.portalcontroller.PortalControl.noinput;
import static org.opengauss.portalcontroller.PortalControl.portalControlPath;


/**
 * Plan.
 *
 * @author ：liutong
 * @date ：Created in 2022/12/24
 * @since ：1
 */
public final class Plan {
    private static volatile Plan plan;

    private Plan() {

    }

    private static volatile List<RunningTaskThread> runningTaskThreadsList = new CopyOnWriteArrayList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(Plan.class);
    private static String currentTask = "";
    /**
     * Get currentTask.
     *
     * @return String currentTask.
     */
    public static String getCurrentTask() {
        return currentTask;
    }
    /**
     * Set currentTask.
     *
     * @param currentTask currentTask.
     */
    public static void setCurrentTask(String currentTask) {
        Plan.currentTask = currentTask;
    }

    /**
     * Hashmap to save string and the lambda expression.
     */
    public static HashMap<String, PortalControl.EventHandler> taskHandlerHashMap = new HashMap<>();

    /**
     * Get running task list.
     */
    public static List<String> runningTaskList = new ArrayList<>();

    /**
     * Boolean parameter express that the plan is runnable.
     */
    public static boolean isPlanRunnable = true;

    /**
     * Boolean parameter express that the plan is stopping.
     */
    public static boolean stopPlan = false;

    /**
     * Get a instance of class plan.
     */
    public static Plan getInstance() {
        if (plan == null) {
            synchronized (Plan.class) {
                if (plan == null) {
                    plan = new Plan();
                }
            }
        }
        return plan;
    }

    /**
     * Get running threads list.
     *
     * @return runningTaskThreadsList
     */
    public static List<RunningTaskThread> getRunningTaskThreadsList() {
        return runningTaskThreadsList;
    }

    /**
     * Set running threads list.
     *
     * @param runningThreadList runningThreadList
     */
    public static void setRunningTaskThreadsList(List<RunningTaskThread> runningThreadList) {
        Plan.runningTaskThreadsList = runningThreadList;
    }

    /**
     * Execute plan.
     *
     * @param taskList          The task list of the plan.
     */
    public void execPlan(List<String> taskList) {
        initTaskHandlerHashMap();
        Task.initTaskProcessMap();
        Task.initRunTaskHandlerHashMap();
        Task.initStopTaskHandlerHashMap();
        PortalControl.showMigrationParameters();
        if (isPlanRunnable) {
            isPlanRunnable = false;
            for (String taskName : taskList) {
                Plan.setCurrentTask(taskName);
                PortalControl.EventHandler eventHandler = taskHandlerHashMap.get(taskName);
                if (stopPlan) {
                    LOGGER.warn("Please wait for stopping plan.Don't restart plan frequently.");
                    break;
                }
                eventHandler.handle(taskName);
            }
            while (!stopPlan) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted exception occurred in running mysql migraiton.");
                }
            }
            if (stopPlan) {
                Plan.stopPlanThreads();
            }
            isPlanRunnable = true;

        } else {
            LOGGER.error("There is a plan running.Please stop current plan or wait.");
        }

    }

    /**
     * Init taskHandler hashmap.
     */
    public static void initTaskHandlerHashMap() {
        taskHandlerHashMap.clear();
        taskHandlerHashMap.put(Command.Start.Mysql.FULL, (event) -> startMysqlFullMigration());
        taskHandlerHashMap.put(Command.Start.Mysql.FULL_CHECK, (event) -> startMysqlFullMigrationDatacheck());
        taskHandlerHashMap.put(Command.Start.Mysql.INCREMENTAL, (event) -> startMysqlIncrementalMigration());
        taskHandlerHashMap.put(Command.Start.Mysql.INCREMENTAL_CHECK, (event) -> startMysqlIncrementMigrationDatacheck());
        taskHandlerHashMap.put(Command.Start.Mysql.REVERSE, (event) -> startMysqlReverseMigration());
        taskHandlerHashMap.put(Command.Start.Mysql.REVERSE_CHECK, (event) -> startMysqlIncrementMigrationDatacheck());
    }

    /**
     * Start mysql full migration.
     */
    public static void startMysqlFullMigration() {
        runningTaskList.add(Command.Start.Mysql.FULL);
        if (!new File(PortalControl.toolsConfigParametersTable.get(Chameleon.VENV_PATH) + "venv/bin/chameleon").exists() && noinput) {
            InstallMigrationTools.installMysqlFullMigrationTools();
        }
        Tools.changeFullMigrationParameters(PortalControl.toolsMigrationParametersTable);
        Task task = new Task();
        String chameleonVenv = Tools.getSinglePropertiesParameter(Chameleon.VENV_PATH, PortalControl.toolsConfigPath);
        Hashtable<String, String> chameleonParameterTable = new Hashtable<>();
        chameleonParameterTable.put("--config", "default");
        task.useChameleonReplicaOrder(chameleonVenv, "drop_replica_schema", chameleonParameterTable);
        task.useChameleonReplicaOrder(chameleonVenv, "create_replica_schema", chameleonParameterTable);
        chameleonParameterTable.put("--source", "mysql");
        task.useChameleonReplicaOrder(chameleonVenv, "add_source", chameleonParameterTable);
        task.useChameleonReplicaOrder(chameleonVenv, "init_replica", chameleonParameterTable);
        if (PortalControl.toolsMigrationParametersTable.get(MigrationParameters.SNAPSHOT_OBJECT).equals("yes")) {
            task.useChameleonReplicaOrder(chameleonVenv, "start_trigger_replica", chameleonParameterTable);
            task.useChameleonReplicaOrder(chameleonVenv, "start_view_replica", chameleonParameterTable);
            task.useChameleonReplicaOrder(chameleonVenv, "start_func_replica", chameleonParameterTable);
            task.useChameleonReplicaOrder(chameleonVenv, "start_proc_replica", chameleonParameterTable);
        }
        Tools.findOffset();
        chameleonParameterTable.clear();
        LOGGER.info("Mysql full migration finished.");
    }

    /**
     * Start mysql full migration datacheck.
     */
    public static void startMysqlFullMigrationDatacheck() {
        runningTaskList.add(Command.Start.Mysql.FULL_CHECK);
        File kafkaLastFile = new File(PortalControl.toolsConfigParametersTable.get(Debezium.Kafka.PATH) + "libs/kafka-streams-examples-3.2.3.jar");
        File checkLastFile = new File(PortalControl.toolsConfigParametersTable.get(Check.PATH) + "config/log4j2.xml");
        if ((!kafkaLastFile.exists() || !checkLastFile.exists()) && noinput) {
            InstallMigrationTools.installDatacheckTools();
        }
        Tools.changeMigrationDatacheckParameters(PortalControl.toolsMigrationParametersTable);
        String datacheckPath = Tools.getSinglePropertiesParameter(Check.PATH, PortalControl.toolsConfigPath);
        Task.startTaskMethod(Method.Run.ZOOKEEPER);
        Task.startTaskMethod(Method.Run.KAFKA);
        Tools.changeSingleYmlParameter("spring.extract.debezium-enable", false, datacheckPath + "config/application-source.yml");
        Task.startTaskMethod(Method.Run.CHECK_SINK);
        Task.startTaskMethod(Method.Run.CHECK_SOURCE);
        Task.startTaskMethod(Method.Run.CHECK);
        LOGGER.info("Mysql full migration datacheck has started.");
    }

    /**
     * Start mysql incremental migration.
     */
    public static void startMysqlIncrementalMigration() {
        runningTaskList.add(Command.Start.Mysql.INCREMENTAL);
        File kafkaLastFile = new File(PortalControl.toolsConfigParametersTable.get(Debezium.Kafka.PATH) + "libs/kafka-streams-examples-3.2.3.jar");
        File confluentLastFile = new File(PortalControl.toolsConfigParametersTable.get(Debezium.Confluent.PATH) + "etc/kafka/consumer.properties");
        File connectorMySqlLastFile = new File(PortalControl.toolsConfigParametersTable.get(Debezium.Connector.PATH) + "debezium-connector-mysql/debezium-connector-mysql-1.8.1.Final.jar");
        File connectorOpengaussLastFile = new File(PortalControl.toolsConfigParametersTable.get(Debezium.Connector.PATH) + "debezium-connector-opengauss/debezium-connector-opengauss-1.8.1.Final.jar");
        boolean flag = kafkaLastFile.exists() && confluentLastFile.exists() && connectorMySqlLastFile.exists() && connectorOpengaussLastFile.exists();
        if ((!flag) && noinput) {
            InstallMigrationTools.installIncrementMigrationTools();
        }
        Tools.changeIncrementalMigrationParameters(PortalControl.toolsMigrationParametersTable);
        Task.startTaskMethod(Method.Run.ZOOKEEPER);
        Task.startTaskMethod(Method.Run.KAFKA);
        Task.startTaskMethod(Method.Run.REGISTRY);
        if (Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.REVERSE_CONNECT)) != -1) {
            LOGGER.error("Reverse migration is running.Cannot run incremental migration.");
            return;
        }
        Task.startTaskMethod(Method.Run.CONNECT);
        while (!stopPlan) {
            try {
                LOGGER.info("Incremental migration is running...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted exception occurred in running incremental migraiton.");
            }
        }
    }

    /**
     * Start mysql reverse migration.
     */
    public static void startMysqlReverseMigration() {
        runningTaskList.add(Command.Start.Mysql.REVERSE);
        File kafkaLastFile = new File(PortalControl.toolsConfigParametersTable.get(Debezium.Kafka.PATH) + "libs/kafka-streams-examples-3.2.3.jar");
        File confluentLastFile = new File(PortalControl.toolsConfigParametersTable.get(Debezium.Confluent.PATH) + "etc/kafka/consumer.properties");
        File connectorMySqlLastFile = new File(PortalControl.toolsConfigParametersTable.get(Debezium.Connector.PATH) + "debezium-connector-mysql/debezium-connector-mysql-1.8.1.Final.jar");
        File connectorOpengaussLastFile = new File(PortalControl.toolsConfigParametersTable.get(Debezium.Connector.PATH) + "debezium-connector-opengauss/debezium-connector-opengauss-1.8.1.Final.jar");
        boolean flag = kafkaLastFile.exists() && confluentLastFile.exists() && connectorMySqlLastFile.exists() && connectorOpengaussLastFile.exists();
        if ((!flag) && noinput) {
            InstallMigrationTools.installIncrementMigrationTools();
        }
        Tools.changeReverseMigrationParameters(PortalControl.toolsMigrationParametersTable);
        Task.startTaskMethod(Method.Run.ZOOKEEPER);
        Task.startTaskMethod(Method.Run.KAFKA);
        Task.startTaskMethod(Method.Run.REGISTRY);
        if (Tools.getCommandPid(Task.getTaskProcessMap().get(Method.Run.CONNECT)) != -1) {
            LOGGER.error("Incremental migration is running.Cannot run reverse migration.");
            return;
        } else {
            Task.startTaskMethod(Method.Run.REVERSE_CONNECT);
        }
        while (!stopPlan) {
            try {
                Thread.sleep(1000);
                LOGGER.info("Reverse migration is running...");
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted exception occurred in running reverse migraiton.");
            }
        }
    }

    /**
     * Start mysql incremental migration datacheck.
     */
    public static void startMysqlIncrementMigrationDatacheck() {
        runningTaskList.add(Command.Start.Mysql.INCREMENTAL_CHECK);
        if (!new File(PortalControl.toolsConfigParametersTable.get(Check.PATH) + "config/log4j2.xml").exists() && noinput) {
            InstallMigrationTools.installDatacheckTools();
        }
        String datacheckPath = PortalControl.toolsConfigParametersTable.get(Check.PATH);
        if (Tools.getCommandPid("QuorumPeerMain") == -1 || Tools.getCommandPid("Kafka") == -1 || Tools.getCommandPid("ConnectStandalone") == -1) {
            LOGGER.error("There is no connector started.");
        } else {
            Tools.changeSingleYmlParameter("spring.extract.debezium-enable", true, datacheckPath + "config/application-source.yml");
            Task.startTaskMethod(Method.Run.CHECK_SINK);
            Task.startTaskMethod(Method.Run.CHECK_SOURCE);
            Task.startTaskMethod(Method.Run.CHECK);
        }
    }

    /**
     * Stop Plan.
     */
    public static void stopPlanThreads() {
        try {
            LOGGER.info("Stop plan.");
            int size = Plan.runningTaskThreadsList.size();
            for (int i = size - 1; i > -1; i--) {
                Thread.sleep(3000);
                Plan.runningTaskThreadsList.get(i).stopTask();
            }
            Plan.runningTaskThreadsList.clear();
            Plan.runningTaskList.clear();
            Plan.currentTask = "";
            PortalControl.taskList.clear();
            File portalFile = new File(portalControlPath + "config/status");
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(portalFile)));
            bw.write("Plan status: runnable");
            bw.flush();
            bw.close();
            ThreadCheckProcess.exit = true;
            isPlanRunnable = true;
            Plan.stopPlan = false;
            LOGGER.info("All tasks has stopped.");
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted exception occurred in stopping the plan.");
        } catch (FileNotFoundException e) {
            LOGGER.error("File status not found.");
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in stopping the plan.");
        }
    }

    /**
     * Check running threads whose pid of process changed.
     *
     * @return flag A boolean parameter express that threads are running.
     */
    public static boolean checkRunningThreads() {
        boolean flag = true;
        try {
            File file = new File(portalControlPath + "config/status");
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            if (Plan.isPlanRunnable) {
                bw.write("Plan status: runnable" + System.lineSeparator());
            } else {
                bw.write("Plan status: running" + System.lineSeparator());
            }
            bw.write("Current plan:" + System.lineSeparator());
            for (String task : PortalControl.taskList) {
                bw.write(task + System.lineSeparator());
            }
            bw.write("Running task list:" + System.lineSeparator());
            for (String task : runningTaskList) {
                bw.write(task + System.lineSeparator());
            }
            bw.flush();
            if (runningTaskThreadsList.size() != 0) {
                bw.write("Running task threads list:" + System.lineSeparator());
                bw.write("method name | process name | pid " + System.lineSeparator());
                for (RunningTaskThread thread : runningTaskThreadsList) {
                    int pid = Tools.getCommandPid(thread.getProcessName());
                    if ((pid != thread.getPid() || pid == -1) && (!PortalControl.commandLineParameterStringMap.get("action").equals("stop"))) {
                        String[] str = thread.getProcessName().split(" ");
                        LOGGER.error("Process " + str[0] + " exit abnormally or process " + str[0] + " has started.");
                        flag = false;
                    }
                    bw.write(thread.getMethodName() + "|" + thread.getProcessName() + "|" + pid + System.lineSeparator());
                }
                bw.flush();
            }
            bw.close();
        } catch (FileNotFoundException e) {
            LOGGER.error("Can't find the file status.Please check if status exists.");
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in executing the command.Execute command failed.");
        }
        return flag;
    }
}


