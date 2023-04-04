package org.opengauss.portalcontroller;

import org.opengauss.jdbc.PgConnection;
import org.opengauss.portalcontroller.constant.Opengauss;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * The type Jdbc tools.
 */
public class JdbcTools {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTools.class);

    /**
     * Gets pg connection.
     *
     * @return the pg connection
     */
    public static PgConnection getPgConnection() {
        PgConnection conn = null;
        Hashtable<String, String> hashtable = PortalControl.toolsMigrationParametersTable;
        String opengaussDatabaseHost = hashtable.get(Opengauss.DATABASE_HOST);
        String opengaussDatabasePort = hashtable.get(Opengauss.DATABASE_PORT);
        String opengaussDatabaseName = hashtable.get(Opengauss.DATABASE_NAME);
        String opengaussUserName = hashtable.get(Opengauss.USER);
        String opengaussUserPassword = hashtable.get(Opengauss.PASSWORD);
        String opengaussUrl = "jdbc:opengauss://" + opengaussDatabaseHost + ":" + opengaussDatabasePort + "/" + opengaussDatabaseName;
        try {
            conn = (PgConnection) DriverManager.getConnection(opengaussUrl, opengaussUserName, opengaussUserPassword);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
        return conn;
    }

    /**
     * Select global variables boolean.
     *
     * @param connection   the connection
     * @param key          the key
     * @param defaultValue the default value
     * @return the boolean
     */
    public static boolean selectGlobalVariables(PgConnection connection, String key, String defaultValue) {
        boolean flag = false;
        if (connection != null) {
            try {
                PreparedStatement preparedStatement = connection.prepareStatement("SHOW GLOBAL VARIABLES where Variable_name = '" + key + "';");
                if (preparedStatement.execute()) {
                    ResultSet rs = preparedStatement.getResultSet();
                    rs.next();
                    String value = rs.getString("Value");
                    if (value.equals(defaultValue)) {
                        flag = true;
                    }else{
                        LOGGER.error("If you want to use reverse migration,please alter system set "+key+" to "+defaultValue+" and restart openGauss to make it work.");
                    }
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
        }
        return flag;
    }

    /**
     * Select version boolean.
     *
     * @param connection the connection
     * @return the boolean
     */
    public static boolean selectVersion(PgConnection connection) {
        boolean flag = false;
        if (connection != null) {
            try {
                Statement statement = connection.createStatement();
                String selectVersionSql = "select version()";
                if (statement.execute(selectVersionSql)) {
                    ResultSet rs = statement.getResultSet();
                    rs.next();
                    String value = rs.getString("version");
                    String openGauss = "openGauss";
                    int startIndex = value.indexOf(openGauss) + openGauss.length();
                    int endIndex = value.indexOf("build");
                    String version = value.substring(startIndex, endIndex).trim();
                    int versionNum = Integer.parseInt(version.replaceAll("\\.", ""));
                    if (versionNum > 300) {
                        flag = true;
                    }else{
                        LOGGER.error("Please upgrade openGauss to version 3.1.0 or higher to use reverse migration.");
                    }
                    statement.close();
                }
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
        }
        return flag;
    }

    /**
     * Change all table boolean.
     *
     * @param connection the connection
     * @return the boolean
     */
    public static boolean changeAllTable(PgConnection connection) {
        boolean flag = true;
        if (connection != null) {
            try {
                String schema = PortalControl.toolsMigrationParametersTable.get(Opengauss.DATABASE_SCHEMA);
                connection.setSchema(schema);
                String selectSql = "SELECT distinct(tablename) FROM pg_tables WHERE SCHEMANAME = '" + schema + "';";
                Statement selectTableStatement = connection.createStatement();
                selectTableStatement.execute(selectSql);
                ArrayList<String> arrayList = new ArrayList<>();
                ResultSet rs = selectTableStatement.getResultSet();
                while (rs.next()) {
                    String tableName = rs.getString("tablename");
                    arrayList.add(tableName);
                }
                String alterTableSql;
                Statement alterTableStatement = connection.createStatement();
                for (String tableName : arrayList) {
                    alterTableSql = String.format("ALTER table %s replica identity full", tableName);
                    alterTableStatement.execute(alterTableSql);
                }
                alterTableStatement.close();
                selectTableStatement.close();
                LOGGER.info("Alter all table replica identity full finished.");
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
        }
        return flag;
    }

    /**
     * Create logical replication slot boolean.
     *
     * @param connection the connection
     * @param slotName   the slot name
     * @return the boolean
     */
    public static boolean createLogicalReplicationSlot(PgConnection connection, String slotName){
        boolean flag = true;
        if (connection != null) {
            try {
                Statement statement = connection.createStatement();
                String selectSlotSql = "SELECT * FROM pg_get_replication_slots()";
                String columnName = "slot_name";
                boolean isReplicationSlotExists = isSpecifiedNameExist(statement, selectSlotSql, slotName, columnName);
                if (isReplicationSlotExists) {
                    slotName += "_" + System.currentTimeMillis();
                }
                String createSlotSql = "SELECT * FROM pg_create_logical_replication_slot('" + slotName + "', 'pgoutput')";
                statement.execute(createSlotSql);
                Plan.slotName = slotName;
                LOGGER.info("Create logical replication slot " + slotName + " finished.");
                String selectPublicationSql = "SELECT pubname from pg_publication";
                String publicationName = "dbz_publication";
                String pubName = "pubname";
                boolean isPublicationExist = isSpecifiedNameExist(statement, selectPublicationSql, publicationName, pubName);
                if (isPublicationExist) {
                    LOGGER.info("PUBLICATION dbz_publication already exists.");
                } else {
                    String createPublicationSql = "CREATE PUBLICATION dbz_publication FOR ALL TABLES;";
                    statement.execute(createPublicationSql);
                    LOGGER.info("Create publication dbz_publication finished.");
                }
                statement.close();
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
        }
        return flag;
    }

    public static boolean isSpecifiedNameExist(Statement statement, String sql, String name, String columnName) {
        boolean flag = false;
        try {
            statement.execute(sql);
            ResultSet resultSet = statement.getResultSet();
            while (resultSet.next()) {
                if (resultSet.getString(columnName).equals(name)) {
                    flag = true;
                    break;
                }
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
        return flag;
    }

    /**
     * Drop logical replication slot boolean.
     *
     * @param connection the connection
     * @param slotName   the slot name
     * @return the boolean
     */
    public static boolean dropLogicalReplicationSlot(PgConnection connection, String slotName) {
        boolean flag = true;
        if (connection != null) {
            try {
                Statement statement = connection.createStatement();
                String selectSlotSql = "SELECT * FROM pg_get_replication_slots()";
                String columnName = "slot_name";
                boolean isReplicationSlotExists = isSpecifiedNameExist(statement, selectSlotSql, slotName, columnName);
                if (isReplicationSlotExists) {
                    String createSlotSql = "SELECT * FROM pg_drop_replication_slot('" + slotName + "')";
                    statement.execute(createSlotSql);
                    LOGGER.info("Drop logical replication slot " + slotName + " finished.");
                }else{
                    LOGGER.info("No logical replication slot " + slotName + " to drop.");
                }
                String selectPublicationSql = "SELECT pubname from pg_publication";
                String publicationName = "dbz_publication";
                String pubName = "pubname";
                boolean isPublicationExist = isSpecifiedNameExist(statement, selectPublicationSql, publicationName, pubName);
                if (isPublicationExist) {
                    String createPublicationSql = "DROP PUBLICATION dbz_publication";
                    PreparedStatement preparedStatement2 = connection.prepareStatement(createPublicationSql);
                    preparedStatement2.execute();
                    preparedStatement2.close();
                    LOGGER.info("Drop publication dbz_publication finished.");
                } else {
                    LOGGER.info("PUBLICATION dbz_publication does not exist.");
                }
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
        }
        return flag;
    }
}
