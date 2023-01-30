package org.opengauss.portalcontroller.constant;

public interface Debezium {
    String PATH = "debezium.path";
    String PKG_PATH = "debezium.pkg.path";
    interface Kafka {
        String PATH = "kafka.path";
        String PKG_URL = "kafka.pkg.url";
        String PKG_NAME = "kafka.pkg.name";
    }

    interface Confluent {
        String PATH = "confluent.path";
        String PKG_URL = "confluent.pkg.url";
        String PKG_NAME = "confluent.pkg.name";

    }
    interface Connector {
        String PATH = "connector.path";
        String MYSQL_PKG_URL = "connector.mysql.pkg.url";
        String MYSQL_PKG_NAME = "connector.mysql.pkg.name";
        String OPENGAUSS_PKG_URL = "connector.opengauss.pkg.url";
        String OPENGAUSS_PKG_NAME = "connector.opengauss.pkg.name";
    }
    interface Source {
        String HOST = "database.hostname";
        String PORT = "database.port";
        String USER = "database.user";
        String PASSWORD = "database.password";
        String WHITELIST = "database.whitelist";
        String NAME = "database.dbname";
    }
    interface Sink {
        interface Mysql {
            String USER = "mysql.username";
            String PASSWORD = "mysql.password";
            String URL = "mysql.url";
            String PORT = "mysql.port";
            String NAME = "mysql.database";
        }

        interface Opengauss {
            String USER = "opengauss.username";
            String PASSWORD = "opengauss.password";
            String URL = "opengauss.url";
        }
        String SCHEMA_MAPPING = "schema.mappings";
    }
}
