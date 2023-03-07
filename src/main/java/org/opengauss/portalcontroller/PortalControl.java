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

import org.opengauss.portalcontroller.check.CheckTask;
import org.opengauss.portalcontroller.check.CheckTaskFullDatacheck;
import org.opengauss.portalcontroller.check.CheckTaskIncrementalDatacheck;
import org.opengauss.portalcontroller.check.CheckTaskIncrementalMigration;
import org.opengauss.portalcontroller.check.CheckTaskMysqlFullMigration;
import org.opengauss.portalcontroller.check.CheckTaskReverseMigration;
import org.opengauss.portalcontroller.constant.Chameleon;
import org.opengauss.portalcontroller.constant.Check;
import org.opengauss.portalcontroller.constant.Command;
import org.opengauss.portalcontroller.constant.Debezium;
import org.opengauss.portalcontroller.constant.MigrationParameters;
import org.opengauss.portalcontroller.constant.Mysql;
import org.opengauss.portalcontroller.constant.Offset;
import org.opengauss.portalcontroller.constant.Opengauss;
import org.opengauss.portalcontroller.constant.Regex;
import org.opengauss.portalcontroller.constant.Status;
import org.opengauss.portalcontroller.software.Software;
import org.opengauss.portalcontroller.status.ThreadStatusController;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

/**
 * Portal control.
 *
 * @author ：liutong
 * @date ：Created in 2022/12/24
 * @since ：1
 */
public class PortalControl {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PortalControl.class);

    /**
     * Task list in the executing plan.
     */
    public static List<String> taskList = new ArrayList<>() {

    };

    /**
     * Default plan list.
     */
    public static Hashtable<String, List<String>> planList = new Hashtable<>() {
    };

    /**
     * Hashmap to save the value of action and the lambda expression.
     */
    public static HashMap<String, EventHandler> actionHandlerHashMap = new HashMap<>();

    /**
     * Hashmap to save the value of command and the lambda expression.
     */
    public static HashMap<String, EventHandler> commandHandlerHashMap = new HashMap<>();

    /**
     * The portal control path.
     */
    public static String portalControlPath = "";

    /**
     * The constant portalWorkSpacePath.
     */
    public static String portalWorkSpacePath = "";

    /**
     * The path of the file which contains the path of the migration tools.
     */
    public static String toolsConfigPath = "";

    /**
     * The path of the file which contains migration parameters.
     */
    public static String migrationConfigPath = "";

    /**
     * Hashtable to save the config parameters about path of migration tools.
     */
    public static Hashtable<String, String> toolsConfigParametersTable = new Hashtable<>();

    /**
     * Hashtable to save the regex expression of the parameters of toolsConfigParametersTable.
     */
    public static HashMap<String, String> parametersRegexMap = new HashMap<>();

    /**
     * Hashtable to save the migration parameters.
     */
    public static Hashtable<String, String> toolsMigrationParametersTable = new Hashtable<>();

    /**
     * Thread to check process.
     */
    public static ThreadCheckProcess threadCheckProcess = new ThreadCheckProcess();

    /**
     * Hashmap to save the parameters in commandline and their values.
     */
    public static HashMap<String, String> commandLineParameterStringMap = new HashMap<>();

    /**
     * Parameter to decide if you can run portal no input.
     */
    public static boolean noinput = true;

    /**
     * Command counts.
     */
    public static int commandCounts = 0;

    /**
     * Command.
     */
    public static String latestCommand = "";

    /**
     * The constant status.
     */
    public static int status = Status.START_FULL_MIGRATION;

    /**
     * The constant fullDatacheckFinished.
     */
    public static boolean fullDatacheckFinished = false;

    /**
     * The constant startPort.
     */
    public static int startPort = 10000;

    /**
     * The constant threadGetOrder.
     */
    public static ThreadGetOrder threadGetOrder = new ThreadGetOrder();
    /**
     * The constant threadStatusController.
     */
    public static ThreadStatusController threadStatusController = new ThreadStatusController();

    /**
     * The constant errorMsg.
     */
    public static String errorMsg = "";

    /**
     * The constant softwareList.
     */
    public static ArrayList<Software> softwareList = new ArrayList<>();

    /**
     * Main method.The first parameter is path of portal control.
     *
     * @param args args
     */
    public static void main(String[] args) {
        File file = new File(PortalControl.portalControlPath + "workspace");
        if (file.exists() && file.isDirectory()) {
            int workspaces = Objects.requireNonNull(file.listFiles()).length;
            startPort += workspaces * 50;
        }
        Tools.cleanInputOrder();
        initPlanList();
        initParametersRegexMap();
        initCommandLineParameters();
        initActionHandlerHashMap();
        initCommandHandlerHashMap();
        String path = commandLineParameterStringMap.get(Command.Parameters.PATH);
        String workspaceId = commandLineParameterStringMap.get(Command.Parameters.ID);
        portalControlPath = path;
        if (workspaceId.equals("")) {
            portalWorkSpacePath = path;
        } else {
            portalWorkSpacePath = path + "workspace/" + workspaceId + "/";
        }
        toolsConfigPath = portalWorkSpacePath + "config/toolspath.properties";
        migrationConfigPath = portalWorkSpacePath + "config/migrationConfig.properties";
        Plan.createWorkspace(workspaceId);
        checkPath();
        Task.initTaskProcessMap();
        threadCheckProcess.setName("threadCheckProcess");
        threadCheckProcess.start();
        threadStatusController.setWorkspaceId(workspaceId);
        threadStatusController.start();
        noinput = PortalControl.commandLineParameterStringMap.get(Command.Parameters.SKIP).equals("true");
        threadGetOrder.start();
        if (noinput) {
            String order = commandLineParameterStringMap.get(Command.Parameters.ORDER);
            if (order != null) {
                String[] orders = order.split("_");
                String newOrder = orders[0];
                for (int i = 1; i < orders.length; i++) {
                    newOrder += " " + orders[i];
                }
                if (commandHandlerHashMap.containsKey(newOrder)) {
                    EventHandler eventHandler = commandHandlerHashMap.get(newOrder);
                    eventHandler.handle(newOrder);
                } else {
                    LOGGER.error("Invalid command.Please input help to get valid command.");
                }
            } else {
                String action = commandLineParameterStringMap.get(Command.Parameters.ACTION);
                actionHandlerHashMap.get(action).handle(action);
            }
        } else {
            Scanner sc = new Scanner(System.in);
            String command = "";
            while (true) {
                LOGGER.info("Please input command.");
                command = sc.nextLine().trim().replaceAll("\n", "");
                if (command.equals("exit")) {
                    break;
                } else if (commandHandlerHashMap.containsKey(command)) {
                    EventHandler eventHandler = commandHandlerHashMap.get(command);
                    eventHandler.handle(command);
                } else {
                    LOGGER.error("Invalid command.Please input help to get valid command.");
                }
            }
        }
        threadCheckProcess.exit = true;
        threadGetOrder.exit = true;
        threadStatusController.exit = true;
    }

    /**
     * Init task list of plan.
     *
     * @param path The path of file which contains task list.
     */
    public static void initTasklist(String path) {
        File file = new File(path);
        String str = "";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            while (true) {
                str = in.readLine();
                if (str != null) {
                    str = str.replaceFirst(System.lineSeparator(), "");
                    taskList.add(str);
                } else {
                    break;
                }
            }
            in.close();
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in changing single yml parameter.");
        }
    }

    /**
     * Init plan list.
     */
    public static void initPlanList() {
        List<String> plan1 = new ArrayList<>();
        plan1.add("start mysql full migration");
        plan1.add("start mysql full migration datacheck");
        planList.put("plan1", plan1);
        List<String> plan2 = new ArrayList<>();
        plan2.add("start mysql full migration");
        plan2.add("start mysql full migration datacheck");
        plan2.add("start mysql incremental migration");
        plan2.add("start mysql incremental migration datacheck");
        planList.put("plan2", plan2);
        List<String> plan3 = new ArrayList<>();
        plan3.add("start mysql full migration");
        plan3.add("start mysql full migration datacheck");
        plan3.add("start mysql incremental migration");
        plan3.add("start mysql incremental migration datacheck");
        plan3.add("start mysql reverse migration");
        plan3.add("start mysql reverse migration datacheck");
        planList.put("plan3", plan3);
    }

    /**
     * Show plan list.
     */
    public static void showPlanList() {
        LOGGER.info("plan1");
        List<String> defaultPlan1 = planList.get("plan1");
        for (String task : defaultPlan1) {
            LOGGER.info(task);
        }
        LOGGER.info("plan2");
        List<String> defaultPlan2 = planList.get("plan2");
        for (String task : defaultPlan2) {
            LOGGER.info(task);
        }
        LOGGER.info("plan3");
        List<String> defaultPlan3 = planList.get("plan3");
        for (String task : defaultPlan3) {
            LOGGER.info(task);
        }
    }

    /**
     * Show status.
     */
    public static void showStatus() {
        if (!Plan.getCurrentTask().equals("")) {
            LOGGER.info("Current task:" + Plan.getCurrentTask());
        } else {
            LOGGER.info("No running plan.");
        }
    }

    /**
     * Init toolsConfigParametersTable and toolsMigrationParametersTable.
     */
    public static void initHashTable() {
        PortalControl.toolsConfigParametersTable.clear();
        PortalControl.toolsMigrationParametersTable.clear();
        PortalControl.initParametersRegexMap();
        Tools.getParameterCommandLineFirst(PortalControl.toolsConfigParametersTable, PortalControl.portalWorkSpacePath + "config/toolspath.properties");
        Tools.getParameterCommandLineFirst(PortalControl.toolsMigrationParametersTable, PortalControl.portalWorkSpacePath + "config/migrationConfig.properties");
    }

    /**
     * Init action handler hashmap.Execute funciton based on the value of parameter action in commandline.
     */
    public static void initActionHandlerHashMap() {
        actionHandlerHashMap.clear();
        actionHandlerHashMap.put(Command.Action.HELP, (event) -> help());
        actionHandlerHashMap.put(Command.Action.SHOW, (event) -> show());
        actionHandlerHashMap.put(Command.Action.STOP, (event) -> stop());
        actionHandlerHashMap.put(Command.Action.INSTALL, (event) -> install());
        actionHandlerHashMap.put(Command.Action.UNINSTALL, (event) -> uninstall());
        actionHandlerHashMap.put(Command.Action.START, (event) -> start());
    }

    /**
     * Set migration parameters which include user name,password,host,port,database name,schema in mysql and openGauss database.
     */
    public static void setMigrationParameters() {
        Hashtable<String, String> migrationParametersSet = new Hashtable<>();
        Scanner sc = new Scanner(System.in);
        LOGGER.info("Please input mysql user name:");
        migrationParametersSet.put(Mysql.USER, Tools.checkInputString(sc, ""));
        LOGGER.info("Please input mysql database user password:");
        migrationParametersSet.put(Mysql.PASSWORD, Tools.checkInputString(sc, ""));
        LOGGER.info("Please input mysql database host:");
        migrationParametersSet.put(Mysql.DATABASE_HOST, Tools.checkInputString(sc, Regex.IP));
        LOGGER.info("Please input mysql database port:");
        migrationParametersSet.put(Mysql.DATABASE_PORT, Tools.checkInputString(sc, Regex.PORT));
        LOGGER.info("Please input mysql database name:");
        String mysqlDatabaseName = Tools.checkInputString(sc, "");
        migrationParametersSet.put(Mysql.DATABASE_NAME, mysqlDatabaseName);
        LOGGER.info("Please input opengauss database user name:");
        migrationParametersSet.put(Opengauss.USER, Tools.checkInputString(sc, ""));
        LOGGER.info("Please input opengauss database user password:");
        migrationParametersSet.put(Opengauss.PASSWORD, Tools.checkInputString(sc, ""));
        LOGGER.info("Please input opengauss database host:");
        migrationParametersSet.put(Opengauss.DATABASE_HOST, Tools.checkInputString(sc, Regex.IP));
        LOGGER.info("Please input opengauss database port:");
        migrationParametersSet.put(Opengauss.DATABASE_PORT, Tools.checkInputString(sc, Regex.PORT));
        LOGGER.info("Please input opengauss database name:");
        migrationParametersSet.put(Opengauss.DATABASE_NAME, Tools.checkInputString(sc, ""));
        LOGGER.info("Please input opengauss database schema:");
        migrationParametersSet.put(Opengauss.DATABASE_SCHEMA, Tools.checkInputString(sc, ""));
        PortalControl.toolsMigrationParametersTable = migrationParametersSet;
        Tools.changePropertiesParameters(migrationParametersSet, migrationConfigPath);
        Tools.changeMigrationParameters(migrationParametersSet);
    }

    /**
     * If the value of action in commandline is show,execute this function.
     */
    public static void show() {
        String showOrder = "show";
        String parameter = commandLineParameterStringMap.get(Command.Parameters.PARAMETER);
        showOrder += " " + parameter;
        EventHandler showEventHandler = commandHandlerHashMap.get(showOrder);
        if (showEventHandler != null) {
            LOGGER.info(showOrder);
            showEventHandler.handle(showOrder);
        } else {
            LOGGER.error("Invalid command.");
        }
    }

    /**
     * If the value of action in commandline is stop,execute this function.
     */
    public static void stop() {
        String stopOrder = "stop";
        String plan = commandLineParameterStringMap.get(Command.Parameters.PARAMETER);
        if (plan.equals("plan")) {
            stopOrder += " plan";
        }
        EventHandler stopEventHandler = commandHandlerHashMap.get(stopOrder);
        if (stopEventHandler != null) {
            LOGGER.info(stopOrder);
            stopEventHandler.handle(stopOrder);
        } else {
            LOGGER.error("Invalid command.");
        }
    }

    /**
     * Show migration parameters which include user name,password,host,port,database name,schema in mysql and openGauss database.
     * If the parameters are wrong,you can change values of parameters.
     */
    public static void showMigrationParameters() {
        LOGGER.info("Migration parameters:");
        Set<String> parametersSet = new TreeSet<String>((o1, o2) -> (o1.compareTo(o2)));
        parametersSet.addAll(toolsMigrationParametersTable.keySet());
        for (String key : parametersSet) {
            if (key.contains("password")) {
                LOGGER.info(key + ":*****");
            } else {
                LOGGER.info(key + ":" + toolsMigrationParametersTable.get(key));
            }
        }
        if (!PortalControl.noinput) {
            LOGGER.info("Please sure the migration parameters are right,or you can input change to change migration parameters.");
            Scanner sc = new Scanner(System.in);
            String order = sc.nextLine().trim();
            if (order.equals("change")) {
                PortalControl.setMigrationParameters();
            } else {
                Tools.changeMigrationParameters(PortalControl.toolsMigrationParametersTable);
            }
        }
    }

    /**
     * If the value of action in commandline is install,execute this function.
     */
    public static void install() {
        String installOrder = "install";
        String type = commandLineParameterStringMap.get(Command.Parameters.TYPE);
        installOrder += " " + type;
        String migrationType = commandLineParameterStringMap.get(Command.Parameters.MIGRATION_TYPE);
        if (commandLineParameterStringMap.get(Command.Parameters.CHECK).equals("true")) {
            installOrder += " datacheck";
        } else {
            installOrder += " " + migrationType + " migration";
        }
        installOrder += " tools";
        String parameter = commandLineParameterStringMap.get(Command.Parameters.PARAMETER);
        if (parameter.equals("online") || parameter.equals("offline")) {
            installOrder += " " + parameter;
        }
        EventHandler installEventHandler = commandHandlerHashMap.get(installOrder);
        if (installEventHandler != null) {
            LOGGER.info(installOrder);
            installEventHandler.handle(installOrder);
        } else {
            LOGGER.error("Invalid command.");
        }
    }

    /**
     * If the value of action in commandline is uninstall,execute this function.
     */
    public static void uninstall() {
        String uninstallOrder = "uninstall";
        String migrationType = commandLineParameterStringMap.get(Command.Parameters.MIGRATION_TYPE);
        String type = commandLineParameterStringMap.get(Command.Parameters.TYPE);
        uninstallOrder += " " + type;
        if (commandLineParameterStringMap.get(Command.Parameters.CHECK).equals("true")) {
            uninstallOrder += " datacheck";
        } else {
            uninstallOrder += " " + migrationType + " migration";
        }
        uninstallOrder += " tools";
        EventHandler uninstallEventHandler = commandHandlerHashMap.get(uninstallOrder);
        if (uninstallEventHandler != null) {
            LOGGER.info(uninstallOrder);
            uninstallEventHandler.handle(uninstallOrder);
        } else {
            LOGGER.error("Invalid command.");
        }
    }

    /**
     * If the value of action in commandline is start,execute this function.
     */
    public static void start() {
        String startOrder = "start";
        String plan = commandLineParameterStringMap.get(Command.Parameters.PARAMETER);
        if (planList.containsKey(plan)) {
            startOrder += " " + plan;
        } else if (plan.equals("current")) {
            startOrder += " " + plan + " plan";
        } else {
            String type = commandLineParameterStringMap.get(Command.Parameters.TYPE);
            String migrationType = commandLineParameterStringMap.get(Command.Parameters.MIGRATION_TYPE);
            startOrder += " " + type + " " + migrationType + " migration";
            if (commandLineParameterStringMap.get(Command.Parameters.CHECK).equals("true")) {
                startOrder += " datacheck";
            }
        }
        EventHandler startEventHandler = commandHandlerHashMap.get(startOrder);
        if (startEventHandler != null) {
            LOGGER.info(startOrder);
            startEventHandler.handle(startOrder);
        } else {
            LOGGER.error("Invalid command.");
        }
    }

    /**
     * If the value of action in commandline is help,execute this function.
     */
    public static void help() {
        showParameters();
        LOGGER.info("The portal can splice command by the parameters of commandline,or you can input valid command.");
        LOGGER.info("Command list:");
        LOGGER.info("install mysql full migration tools online");
        LOGGER.info("install mysql full migration tools offline");
        LOGGER.info("install mysql full migration tools");
        LOGGER.info("install mysql incremental migration tools online");
        LOGGER.info("install mysql incremental migration tools offline");
        LOGGER.info("install mysql incremental migration tools");
        LOGGER.info("install mysql datacheck tools online");
        LOGGER.info("install mysql datacheck tools offline");
        LOGGER.info("install mysql datacheck tools");
        LOGGER.info("install mysql all migration tools");
        LOGGER.info("uninstall mysql full migration tools");
        LOGGER.info("uninstall mysql incremental migration tools");
        LOGGER.info("uninstall mysql datacheck tools");
        LOGGER.info("uninstall mysql all migration tools");
        LOGGER.info("start mysql full migration");
        LOGGER.info("start mysql incremental migration");
        LOGGER.info("start mysql reverse migration");
        LOGGER.info("start mysql full migration datacheck");
        LOGGER.info("start mysql incremental migration datacheck");
        LOGGER.info("start mysql reverse migration datacheck");
        LOGGER.info("start plan1 --You can execute plan1 in default plan list.");
        LOGGER.info("start plan2 --You can execute plan2 in default plan list.");
        LOGGER.info("start plan3 --You can execute plan3 in default plan list.");
        LOGGER.info("start current plan --You can execute current plan in currentPlan.");
        LOGGER.info("show plans --Show default plans.");
        LOGGER.info("show status --Show plan status.");
        LOGGER.info("show information --Show information of migration which include user name,password,host,port,database name,schema in mysql and openGauss database.");
        LOGGER.info("show parameters --Show parameters of commandline.");
        LOGGER.info("stop plan");
    }

    /**
     * Start default plan with plan name in default plan list.
     *
     * @param plan the plan
     */
    public static void startDefaultPlan(String plan) {
        if (!Plan.isPlanRunnable) {
            LOGGER.error("There is a plan already running.");
            return;
        }
        if (planList.containsKey(plan)) {
            taskList.addAll(planList.get(plan));
            startPlan();
        } else {
            LOGGER.error("Default plan list don't have plan whose name is " + plan + ".");
        }
    }

    /**
     * Start current plan.
     */
    public static void startCurrentPlan() {
        if (!Plan.isPlanRunnable) {
            LOGGER.error("There is a plan already running.");
            return;
        }
        String path = PortalControl.portalControlPath + "config/currentPlan";
        initTasklist(path);
        startPlan();
    }

    /**
     * Start plan which has only one task.
     *
     * @param task the task
     */
    public static void startSingleTaskPlan(String task) {
        if (!Plan.isPlanRunnable) {
            LOGGER.error("There is a plan already running.");
            return;
        }
        taskList.add(task);
        startPlan();
    }

    /**
     * Start plan.
     */
    public static void startPlan() {
        String workspaceId = commandLineParameterStringMap.get(Command.Parameters.ID);
        Tools.generatePlanHistory(taskList);
        if (!Task.checkPlan(taskList)) {
            Plan.installPlanPackages();
            LOGGER.error("Invalid plan.");
            return;
        }
        Plan.getInstance(workspaceId).execPlan(PortalControl.taskList);
    }

    /**
     * Stop plan.
     */
    public static void stopPlanCheck() {
        if (!PortalControl.noinput) {
            LOGGER.warn("Please input yes to stop current plan.");
            Scanner sc = new Scanner(System.in);
            String stopOrder = sc.nextLine();
            if (stopOrder.equals("yes")) {
                Plan.stopPlan = true;
                if (Plan.isPlanRunnable) {
                    Plan.stopPlanThreads();
                }
            } else {
                Plan.stopPlan = false;
            }
        } else {
            Plan.stopPlan = true;
            if (Plan.isPlanRunnable) {
                Plan.stopPlanThreads();
            }
        }
    }

    /**
     * Check if portalControlPath,toolsConfigPath or migrationConfigPath exists.
     *
     * @return The boolean parameter which express if portalControlPath,toolsConfigPath or migrationConfigPath exists.
     */
    public static boolean checkPath() {
        if (!new File(portalControlPath).exists() || new File(portalControlPath).isFile()) {
            LOGGER.error("portalControlPath not exist");
            return false;
        }
        return true;
    }

    /**
     * Show parameters of commandline.
     */
    public static void showParameters() {
        LOGGER.info("Parameters list:");
        LOGGER.info("path  --The path of portal.");
        LOGGER.info("action  --The action of portal.");
        LOGGER.info("type --The database type of migration which includes mysql.");
        LOGGER.info("migrationType --The migration type which includes full,increment and reverse.");
        LOGGER.info("skip --If the value of skip is true,you can skip all input parts and use default options in configuration files.");
        LOGGER.info("check --If the value of check is true, it means datacheck for the same type of migration.");
        LOGGER.info("parameter --The parameter of action.");
        LOGGER.info("order --The order which portal can execute.");
    }

    /**
     * Init parameters of commandline.
     */
    public static void initCommandLineParameters() {
        commandLineParameterStringMap.clear();
        setCommandLineParameters(Command.Parameters.PATH, "");
        setCommandLineParameters(Command.Parameters.ACTION, "");
        setCommandLineParameters(Command.Parameters.TYPE, "");
        setCommandLineParameters(Command.Parameters.MIGRATION_TYPE, "");
        setCommandLineParameters(Command.Parameters.PARAMETER, "");
        setCommandLineParameters(Command.Parameters.SKIP, "");
        setCommandLineParameters(Command.Parameters.CHECK, "");
        setCommandLineParameters(Command.Parameters.ORDER, "");
        setCommandLineParameters(Command.Parameters.ID, "1");
    }

    /**
     * Set parameters of commandline.
     *
     * @param parameter Parameter of commandline.
     */
    private static void setCommandLineParameters(String parameter, String defaultValue) {
        String temp = System.getProperty(parameter);
        if (temp != null) {
            commandLineParameterStringMap.put(parameter, temp);
        } else {
            commandLineParameterStringMap.put(parameter, defaultValue);
        }
    }

    /**
     * Init command handler hashmap.
     */
    public static void initCommandHandlerHashMap() {
        ArrayList<CheckTask> checkTasks = new ArrayList<>();
        ArrayList<String> installWays = new ArrayList<>();
        CheckTask checkTaskMysqlFullMigration = new CheckTaskMysqlFullMigration();
        checkTasks.add(checkTaskMysqlFullMigration);
        installWays.add(MigrationParameters.Install.FULL_MIGRATION);
        CheckTask checkTaskMysqlIncrementalMigration = new CheckTaskIncrementalMigration();
        checkTasks.add(checkTaskMysqlIncrementalMigration);
        installWays.add(MigrationParameters.Install.INCREMENTAL_MIGRATION);
        CheckTask checkTaskMysqlReverseMigration = new CheckTaskReverseMigration();
        checkTasks.add(checkTaskMysqlReverseMigration);
        installWays.add(MigrationParameters.Install.REVERSE_MIGRATION);
        CheckTask checkTaskDatacheck = new CheckTaskIncrementalDatacheck();
        checkTasks.add(checkTaskDatacheck);
        installWays.add(MigrationParameters.Install.CHECK);
        commandHandlerHashMap.put(Command.Install.Mysql.FullMigration.ONLINE, (event) -> checkTaskMysqlFullMigration.installAllPackages(true));
        commandHandlerHashMap.put(Command.Install.Mysql.FullMigration.OFFLINE, (event) -> checkTaskMysqlFullMigration.installAllPackages(false));
        commandHandlerHashMap.put(Command.Install.Mysql.FullMigration.DEFAULT, (event) -> checkTaskMysqlFullMigration.installAllPackages());
        commandHandlerHashMap.put(Command.Install.Mysql.IncrementalMigration.ONLINE, (event) -> checkTaskMysqlIncrementalMigration.installAllPackages(true));
        commandHandlerHashMap.put(Command.Install.Mysql.IncrementalMigration.OFFLINE, (event) -> checkTaskMysqlIncrementalMigration.installAllPackages(false));
        commandHandlerHashMap.put(Command.Install.Mysql.IncrementalMigration.DEFAULT, (event) -> checkTaskMysqlIncrementalMigration.installAllPackages());
        commandHandlerHashMap.put(Command.Install.Mysql.ReverseMigration.ONLINE, (event) -> checkTaskMysqlReverseMigration.installAllPackages(true));
        commandHandlerHashMap.put(Command.Install.Mysql.ReverseMigration.OFFLINE, (event) -> checkTaskMysqlReverseMigration.installAllPackages(false));
        commandHandlerHashMap.put(Command.Install.Mysql.ReverseMigration.DEFAULT, (event) -> checkTaskMysqlReverseMigration.installAllPackages());
        commandHandlerHashMap.put(Command.Install.Mysql.Check.ONLINE, (event) -> checkTaskDatacheck.installAllPackages(true));
        commandHandlerHashMap.put(Command.Install.Mysql.Check.OFFLINE, (event) -> checkTaskDatacheck.installAllPackages(false));
        commandHandlerHashMap.put(Command.Install.Mysql.Check.DEFAULT, (event) -> checkTaskDatacheck.installAllPackages());
        commandHandlerHashMap.put(Command.Install.Mysql.All.DEFAULT, (event) -> InstallMigrationTools.installAllMigrationTools(checkTasks));
        commandHandlerHashMap.put(Command.Install.Mysql.All.ONLINE, (event) -> InstallMigrationTools.installAllMigrationTools(true, checkTasks));
        commandHandlerHashMap.put(Command.Install.Mysql.All.OFFLINE, (event) -> InstallMigrationTools.installAllMigrationTools(false, checkTasks));
        commandHandlerHashMap.put(Command.Uninstall.Mysql.FULL, (event) -> checkTaskMysqlFullMigration.uninstall());
        commandHandlerHashMap.put(Command.Uninstall.Mysql.INCREMENTAL, (event) -> checkTaskMysqlIncrementalMigration.uninstall());
        commandHandlerHashMap.put(Command.Uninstall.Mysql.CHECK, (event) -> checkTaskDatacheck.uninstall());
        commandHandlerHashMap.put(Command.Uninstall.Mysql.REVERSE, (event) -> checkTaskMysqlReverseMigration.uninstall());
        commandHandlerHashMap.put(Command.Uninstall.Mysql.ALL, (event) -> InstallMigrationTools.uninstallMigrationTools());
        commandHandlerHashMap.put(Command.Start.Mysql.FULL, (event) -> startSingleTaskPlan(Command.Start.Mysql.FULL));
        commandHandlerHashMap.put(Command.Start.Mysql.INCREMENTAL, (event) -> startSingleTaskPlan(Command.Start.Mysql.INCREMENTAL));
        commandHandlerHashMap.put(Command.Start.Mysql.REVERSE, (event) -> startSingleTaskPlan(Command.Start.Mysql.REVERSE));
        commandHandlerHashMap.put(Command.Start.Mysql.FULL_CHECK, (event) -> startSingleTaskPlan(Command.Start.Mysql.FULL_CHECK));
        commandHandlerHashMap.put(Command.Start.Mysql.INCREMENTAL_CHECK, (event) -> startSingleTaskPlan(Command.Start.Mysql.INCREMENTAL_CHECK));
        commandHandlerHashMap.put(Command.Start.Mysql.REVERSE_CHECK, (event) -> startSingleTaskPlan(Command.Start.Mysql.REVERSE_CHECK));
        commandHandlerHashMap.put(Command.Start.Plan.PLAN1, (event) -> startDefaultPlan("plan1"));
        commandHandlerHashMap.put(Command.Start.Plan.PLAN2, (event) -> startDefaultPlan("plan2"));
        commandHandlerHashMap.put(Command.Start.Plan.PLAN3, (event) -> startDefaultPlan("plan3"));
        commandHandlerHashMap.put(Command.Start.Plan.CURRENT, (event) -> startCurrentPlan());
        commandHandlerHashMap.put(Command.Action.HELP, (event) -> help());
        commandHandlerHashMap.put(Command.Show.PLAN, (event) -> showPlanList());
        commandHandlerHashMap.put(Command.Show.STATUS, (event) -> showStatus());
        commandHandlerHashMap.put(Command.Show.INFORMATION, (event) -> showMigrationParameters());
        commandHandlerHashMap.put(Command.Show.PARAMETERS, (event) -> showParameters());
        commandHandlerHashMap.put(Command.Stop.PLAN, (event) -> Tools.writeInputOrder(Command.Stop.PLAN));
        commandHandlerHashMap.put(Command.Stop.INCREMENTAL_MIGRATION, (event) -> Tools.writeInputOrder(Command.Stop.INCREMENTAL_MIGRATION));
        commandHandlerHashMap.put(Command.Stop.REVERSE_MIGRATION, (event) -> Tools.writeInputOrder(Command.Stop.REVERSE_MIGRATION));
        commandHandlerHashMap.put(Command.Run.INCREMENTAL_MIGRATION, (event) -> Tools.writeInputOrder(Command.Run.INCREMENTAL_MIGRATION));
        commandHandlerHashMap.put(Command.Run.REVERSE_MIGRATION, (event) -> Tools.writeInputOrder(Command.Run.REVERSE_MIGRATION));
    }

    /**
     * Init parameters regex map.
     */
    public static void initParametersRegexMap() {
        parametersRegexMap.put(Chameleon.PATH, Regex.FOLDER_PATH);
        parametersRegexMap.put(Chameleon.VENV_PATH, Regex.FOLDER_PATH);
        parametersRegexMap.put(Chameleon.PKG_PATH, Regex.FOLDER_PATH);
        parametersRegexMap.put(Chameleon.PKG_NAME, Regex.PKG_NAME);
        parametersRegexMap.put(Chameleon.PKG_URL, Regex.URL);
        parametersRegexMap.put(Debezium.PATH, Regex.FOLDER_PATH);
        parametersRegexMap.put(Debezium.PKG_PATH, Regex.FOLDER_PATH);
        parametersRegexMap.put(Debezium.Kafka.PATH, Regex.FOLDER_PATH);
        parametersRegexMap.put(Debezium.Kafka.PKG_URL, Regex.URL);
        parametersRegexMap.put(Debezium.Kafka.PKG_NAME, Regex.PKG_NAME);
        parametersRegexMap.put(Debezium.Confluent.PATH, Regex.FOLDER_PATH);
        parametersRegexMap.put(Debezium.Confluent.PKG_NAME, Regex.PKG_NAME);
        parametersRegexMap.put(Debezium.Confluent.PKG_URL, Regex.URL);
        parametersRegexMap.put(Debezium.Connector.PATH, Regex.FOLDER_PATH);
        parametersRegexMap.put(Debezium.Connector.MYSQL_PATH, Regex.FOLDER_PATH);
        parametersRegexMap.put(Debezium.Connector.OPENGAUSS_PATH, Regex.FOLDER_PATH);
        parametersRegexMap.put(Debezium.Connector.MYSQL_PKG_NAME, Regex.PKG_NAME);
        parametersRegexMap.put(Debezium.Connector.MYSQL_PKG_URL, Regex.URL);
        parametersRegexMap.put(Debezium.Connector.OPENGAUSS_PKG_NAME, Regex.PKG_NAME);
        parametersRegexMap.put(Debezium.Connector.OPENGAUSS_PKG_URL, Regex.URL);
        parametersRegexMap.put(Check.PATH, Regex.FOLDER_PATH);
        parametersRegexMap.put(Check.INSTALL_PATH, Regex.FOLDER_PATH);
        parametersRegexMap.put(Check.PKG_NAME, Regex.PKG_NAME);
        parametersRegexMap.put(Check.PKG_PATH, Regex.FOLDER_PATH);
        parametersRegexMap.put(Check.PKG_URL, Regex.URL);
        parametersRegexMap.put(Mysql.DATABASE_NAME, Regex.NAME);
        parametersRegexMap.put(Mysql.DATABASE_HOST, Regex.IP);
        parametersRegexMap.put(Mysql.DATABASE_PORT, Regex.PORT);
        parametersRegexMap.put(Mysql.PASSWORD, Regex.NAME);
        parametersRegexMap.put(Mysql.USER, Regex.NAME);
        parametersRegexMap.put(Opengauss.USER, Regex.NAME);
        parametersRegexMap.put(Opengauss.PASSWORD, Regex.NAME);
        parametersRegexMap.put(Opengauss.DATABASE_SCHEMA, Regex.NAME);
        parametersRegexMap.put(Opengauss.DATABASE_NAME, Regex.NAME);
        parametersRegexMap.put(Opengauss.DATABASE_PORT, Regex.PORT);
        parametersRegexMap.put(Opengauss.DATABASE_HOST, Regex.IP);
        parametersRegexMap.put(Offset.FILE, Regex.OFFSET_FILE);
        parametersRegexMap.put(Offset.GTID, Regex.OFFSET_GTID);
        parametersRegexMap.put(Offset.POSITION, Regex.POSITION);
    }

    /**
     * Interface eventHandler.There is only one method.Use the method to execute the method in the lambda expression.
     */
    public interface EventHandler {
        /**
         * Handle.
         *
         * @param str the str
         */
        void handle(String str);
    }
}