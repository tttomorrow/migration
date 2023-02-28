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
import org.opengauss.portalcontroller.check.CheckTaskMysqlFullMigration;
import org.opengauss.portalcontroller.check.CheckTaskFullDatacheck;
import org.opengauss.portalcontroller.check.CheckTaskIncrementalMigration;
import org.opengauss.portalcontroller.check.CheckTaskReverseMigration;
import org.opengauss.portalcontroller.constant.Chameleon;
import org.opengauss.portalcontroller.constant.Check;
import org.opengauss.portalcontroller.constant.Debezium;
import org.opengauss.portalcontroller.constant.MigrationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Install migration tools.
 *
 * @author ：liutong
 * @date ：Created in 2022/12/24
 * @since ：1
 */
public class InstallMigrationTools {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstallMigrationTools.class);

    /**
     * Install mysql full migration tools online.
     */
    public static void installMysqlFullMigrationToolsOnline() {
        RuntimeExecTools.download(Chameleon.PKG_URL, Chameleon.PKG_PATH);
        InstallMigrationTools.installMysqlFullMigrationToolsOffline();
    }

    /**
     * Install mysql full migration tools offline.
     *
     * @return the boolean
     */
    public static boolean installMysqlFullMigrationToolsOffline() {
        boolean flag = true;
        CheckTaskMysqlFullMigration checkTask = new CheckTaskMysqlFullMigration();
        checkTask.installAllPackages();
        LOGGER.info("Install full migration tools finished.");
        return flag;
    }

    /**
     * Install increment migration tools online.
     */
    public static void installIncrementalMigrationToolsOnline() {
        RuntimeExecTools.download(Debezium.Kafka.PKG_URL, Debezium.PKG_PATH);
        RuntimeExecTools.download(Debezium.Confluent.PKG_URL, Debezium.PKG_PATH);
        RuntimeExecTools.download(Debezium.Connector.MYSQL_PKG_URL, Debezium.PKG_PATH);
        RuntimeExecTools.download(Debezium.Connector.OPENGAUSS_PKG_URL, Debezium.PKG_PATH);
        InstallMigrationTools.installIncrementalMigrationToolsOffline();
    }

    /**
     * Install increment migration tools offline.
     */
    public static void installIncrementalMigrationToolsOffline() {
        CheckTask checkTask = new CheckTaskIncrementalMigration();
        checkTask.installAllPackages();
        LOGGER.info("Install incremental migration tools finished.");
    }

    /**
     * Install reverse migration tools online.
     */
    public static void installReverseMigrationToolsOnline() {
        RuntimeExecTools.download(Debezium.Kafka.PKG_URL, Debezium.PKG_PATH);
        RuntimeExecTools.download(Debezium.Confluent.PKG_URL, Debezium.PKG_PATH);
        RuntimeExecTools.download(Debezium.Connector.OPENGAUSS_PKG_URL, Debezium.PKG_PATH);
        InstallMigrationTools.installReverseMigrationToolsOffline();
    }

    /**
     * Install reverse migration tools offline.
     */
    public static void installReverseMigrationToolsOffline() {
        CheckTask checkTask = new CheckTaskReverseMigration();
        checkTask.installAllPackages();
        LOGGER.info("Install reverse migration tools finished.");
    }

    /**
     * Install datacheck tools online.
     */
    public static void installDatacheckToolsOnline() {
        RuntimeExecTools.download(Check.PKG_URL, Check.PKG_PATH);
        InstallMigrationTools.installDatacheckToolsOffline();
    }

    /**
     * Install datacheck tools offline.
     */
    public static void installDatacheckToolsOffline() {
        CheckTaskFullDatacheck checkTask = new CheckTaskFullDatacheck();
        checkTask.installAllPackages();
        LOGGER.info("Install datacheck tools finished.");
    }

    /**
     * Install migration tools.
     */
    public static void installMigrationTools() {
        installMysqlFullMigrationTools();
        installIncrementMigrationTools();
        installDatacheckTools();
        installReverseMigrationTools();
        LOGGER.info("All migration tools have been installed.");
    }

    /**
     * Uninstall mysql full migration tools.
     */
    public static void uninstallMysqlFullMigrationTools() {
        RuntimeExecTools.removeFile(PortalControl.toolsConfigParametersTable.get(Chameleon.VENV_PATH) + "venv", PortalControl.portalControlPath + "logs/error.log");
        RuntimeExecTools.removeFile(PortalControl.toolsConfigParametersTable.get(Chameleon.PATH).replaceFirst("~", System.getProperty("user.home")), PortalControl.portalControlPath + "logs/error.log");
        RuntimeExecTools.removeFile(PortalControl.portalControlPath + "tmp/chameleon", PortalControl.portalControlPath + "logs/error.log");
    }

    /**
     * Uninstall incremental migration tools.
     */
    public static void uninstallIncrementalMigrationTools() {
        RuntimeExecTools.removeFile(PortalControl.toolsConfigParametersTable.get(Debezium.PATH), PortalControl.portalControlPath + "logs/error.log");
        RuntimeExecTools.removeFile(PortalControl.portalControlPath + "tmp/kafka-logs", PortalControl.portalControlPath + "logs/error.log");
        RuntimeExecTools.removeFile(PortalControl.portalControlPath + "tmp/zookeeper", PortalControl.portalControlPath + "logs/error.log");
    }

    /**
     * Uninstall datacheck tools.
     */
    public static void uninstallDatacheckTools() {
        RuntimeExecTools.removeFile(PortalControl.toolsConfigParametersTable.get(Check.PATH), PortalControl.portalControlPath + "logs/error.log");
    }

    /**
     * Uninstall reverse migration tools.
     */
    public static void uninstallReverseMigrationTools() {
        RuntimeExecTools.removeFile(PortalControl.toolsConfigParametersTable.get(Debezium.PATH), PortalControl.portalControlPath + "logs/error.log");
        RuntimeExecTools.removeFile(PortalControl.portalControlPath + "tmp/kafka-logs", PortalControl.portalControlPath + "logs/error.log");
        RuntimeExecTools.removeFile(PortalControl.portalControlPath + "tmp/zookeeper", PortalControl.portalControlPath + "logs/error.log");
    }

    /**
     * Uninstall migration tools.
     */
    public static void uninstallMigrationTools() {
        uninstallMysqlFullMigrationTools();
        uninstallIncrementalMigrationTools();
        uninstallDatacheckTools();
        uninstallReverseMigrationTools();
    }

    /**
     * Install mysql full migration tools.You can install tools online or offline.
     */
    public static void installMysqlFullMigrationTools() {
        String installWay = PortalControl.toolsMigrationParametersTable.get(MigrationParameters.Install.FULL_MIGRATION);
        if (installWay.equals("online")) {
            installMysqlFullMigrationToolsOnline();
        } else if (installWay.equals("offline")) {
            installMysqlFullMigrationToolsOffline();
        } else {
            LOGGER.error("Please check default.install.fullmigration.tools.way in migrationConfig.properties.This property must be online or offline.");
        }
    }

    /**
     * Install increment migration tools.You can install tools online or offline.
     */
    public static void installIncrementMigrationTools() {
        String installWay = PortalControl.toolsMigrationParametersTable.get(MigrationParameters.Install.INCREMENTAL_MIGRATION);
        if (installWay.equals("online")) {
            installIncrementalMigrationToolsOnline();
        } else if (installWay.equals("offline")) {
            installIncrementalMigrationToolsOffline();
        } else {
            LOGGER.info("Please check default.install.incremental.migration.tools.way in migrationConfig.properties.This property must be online or offline.");
        }
    }

    /**
     * Install datacheck tools.You can install tools online or offline.
     */
    public static void installDatacheckTools() {
        String installWay = PortalControl.toolsMigrationParametersTable.get(MigrationParameters.Install.CHECK);
        if (installWay.equals("online")) {
            installDatacheckToolsOnline();
        } else if (installWay.equals("offline")) {
            installDatacheckToolsOffline();
        } else {
            LOGGER.info("Please check default.install.datacheck.tools.way in migrationConfig.properties.This property must be online or offline.");
        }
    }

    /**
     * Install reverse migration tools offline.
     */
    public static void installReverseMigrationTools() {
        String installWay = PortalControl.toolsMigrationParametersTable.get(MigrationParameters.Install.REVERSE_MIGRATION);
        if (installWay.equals("online")) {
            installReverseMigrationToolsOnline();
        } else if (installWay.equals("offline")) {
            installReverseMigrationToolsOffline();
        } else {
            LOGGER.info("Please check default.install.datacheck.tools.way in migrationConfig.properties.This property must be online or offline.");
        }
    }
}