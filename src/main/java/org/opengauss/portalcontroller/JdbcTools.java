package org.opengauss.portalcontroller;

import org.opengauss.jdbc.PgConnection;
import org.opengauss.portalcontroller.constant.Opengauss;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;

public class JdbcTools {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTools.class);

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

    public static boolean selectGlobalVariables(PgConnection connection,String key, String defaultValue) {
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
                    }
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
        }
        return flag;
    }

    public static boolean selectVersion(PgConnection connection) {
        boolean flag = false;
        if (connection != null) {
            try {
                PreparedStatement preparedStatement = connection.prepareStatement("select version();");
                if (preparedStatement.execute()) {
                    ResultSet rs = preparedStatement.getResultSet();
                    rs.next();
                    String value = rs.getString("version");
                    String openGauss = "openGauss";
                    int startIndex = value.indexOf(openGauss) + openGauss.length();
                    int endIndex = value.indexOf("build");
                    String version = value.substring(startIndex, endIndex).trim();
                    int versionNum = Integer.parseInt(version.replaceAll("\\.",""));
                    if(versionNum > 300){
                        flag = true;
                    }
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }
        }
        return flag;
    }
}
