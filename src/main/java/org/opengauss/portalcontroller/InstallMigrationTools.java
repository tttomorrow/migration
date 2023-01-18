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

import org.opengauss.portalcontroller.check.CheckTool;
import org.opengauss.portalcontroller.check.CheckToolsChameleon;
import org.opengauss.portalcontroller.check.CheckToolsDatacheck;
import org.opengauss.portalcontroller.check.CheckToolsIncrementalMigration;
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
     */
    public static boolean installMysqlFullMigrationToolsOffline() {
        boolean flag = true;
        CheckTool checkTool = new CheckToolsChameleon();
        checkTool.installPackage();
        checkTool.copyConfigFiles();
        checkTool.changeParameters();
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
        CheckTool checkTool = new CheckToolsIncrementalMigration();
        checkTool.installPackage();
        checkTool.copyConfigFiles();
        checkTool.changeParameters();
        LOGGER.info("Install incremental migration tools finished.");
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
        CheckTool checkTool = new CheckToolsDatacheck();
        checkTool.installPackage();
        checkTool.copyConfigFiles();
        LOGGER.info("Install datacheck tools finished.");
    }

    /**
     * Install migration tools.
     */
    public static void installMigrationTools() {
        installMysqlFullMigrationTools();
        installIncrementMigrationTools();
        installDatacheckTools();
        LOGGER.info("All migration tools have been installed.");
    }

    /**
     * Uninstall mysql full migration tools.
     */
    public static void uninstallMysqlFullMigrationTools() {
        RuntimeExecTools.removeFile(PortalControl.toolsConfigParametersTable.get(Chameleon.VENV_PATH) + "venv");
        RuntimeExecTools.removeFile(PortalControl.toolsConfigParametersTable.get(Chameleon.PATH).replaceFirst("~", System.getProperty("user.home")));
        RuntimeExecTools.removeFile(PortalControl.portalControlPath + "tmp/chameleon");
    }

    /**
     * Uninstall incremental migration tools.
     */
    public static void uninstallIncrementalMigrationTools() {
        RuntimeExecTools.removeFile(PortalControl.toolsConfigParametersTable.get(Debezium.PATH));
        RuntimeExecTools.removeFile(PortalControl.portalControlPath + "tmp/kafka-logs");
        RuntimeExecTools.removeFile(PortalControl.portalControlPath + "tmp/zookeeper");
    }

    /**
     * Uninstall datacheck tools.
     */
    public static void uninstallDatacheckTools() {
        RuntimeExecTools.removeFile(PortalControl.toolsConfigParametersTable.get(Check.PATH));
        RuntimeExecTools.removeFile(PortalControl.portalControlPath + "tmp/check_result");
    }

    /**
     * Uninstall migration tools.
     */
    public static void uninstallMigrationTools() {
        uninstallMysqlFullMigrationTools();
        uninstallIncrementalMigrationTools();
        uninstallDatacheckTools();
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
}