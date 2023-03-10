package org.opengauss.portalcontroller.constant;

public interface Check {
    String NAME = "datacheck";
    String PKG_URL = "datacheck.pkg.url";
    String INSTALL_PATH = "datacheck.install.path";
    String PATH = "datacheck.path";
    String PKG_PATH = "datacheck.pkg.path";
    String PKG_NAME = "datacheck.pkg.name";
    interface Parameters{
        String SCHEMA = "spring.extract.schema";
        String URL = "spring.datasource.druid.dataSourceOne.url";
        String USER_NAME = "spring.datasource.druid.dataSourceOne.username";
        String PASSWORD = "spring.datasource.druid.dataSourceOne.password";
    }

    interface Sink{
        String QUERY_DOP = "sink.query-dop";
        String MIN_IDLE = "sink.minIdle";
        String MAX_ACTIVE = "sink.maxActive";
        String INITIAL_SIZE = "sink.initialSize";
        String TIME_PERIOD = "sink.debezium-time-period";
        String NUM_PERIOD = "sink.debezium-num-period";
    }

    interface Source{
        String QUERY_DOP = "source.query-dop";
        String MIN_IDLE = "source.minIdle";
        String MAX_ACTIVE = "source.maxActive";
        String INITIAL_SIZE = "source.initialSize";
        String TIME_PERIOD = "source.debezium-time-period";
        String NUM_PERIOD = "source.debezium-num-period";
    }
    interface Rules{
        String ENABLE = "rules.enable";
        interface Table {
            String AMOUNT = "rules.table";
            String NAME = "rules.table.name";
            String TEXT = "rules.table.text";
        }
        interface Row{
            String AMOUNT = "rules.row";
            String NAME = "rules.row.name";
            String TEXT = "rules.row.text";
        }
        interface Column{
            String AMOUNT = "rules.column";
            String NAME = "rules.column.name";
            String TEXT = "rules.column.text";

            String ATTRIBUTE = "rules.column.attribute";
        }
    }
}
