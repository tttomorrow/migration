package org.opengauss.portalcontroller.constant;

public interface Chameleon {
    String VENV_PATH = "chameleon.venv.path";
    String PATH = "chameleon.path";
    String PKG_PATH = "chameleon.pkg.path";
    String PKG_NAME = "chameleon.pkg.name";
    String PKG_URL = "chameleon.pkg.url";
    interface Parameters{
        interface Mysql{
            String HOST = "sources.mysql.db_conn.host";
            String PORT = "sources.mysql.db_conn.port";
            String USER = "sources.mysql.db_conn.user";
            String PASSWORD = "sources.mysql.db_conn.password";
            String NAME = "sources.mysql.db_conn.database";
            String MAPPING = "sources.mysql.schema_mappings";
        }
        interface Opengauss{
            String HOST = "pg_conn.host";
            String PORT = "pg_conn.port";
            String USER = "pg_conn.user";
            String PASSWORD = "pg_conn.password";
            String NAME = "pg_conn.database";
        }
    }
}
