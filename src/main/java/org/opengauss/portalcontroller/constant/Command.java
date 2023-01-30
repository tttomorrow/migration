package org.opengauss.portalcontroller.constant;

public interface Command {
    interface Install{
        interface Mysql{
            interface FullMigration{
                String ONLINE = "install mysql full migration tools online";
                String OFFLINE = "install mysql full migration tools offline";
                String DEFAULT = "install mysql full migration tools";
            }
            interface IncrementalMigration{
                String ONLINE = "install mysql incremental migration tools online";
                String OFFLINE = "install mysql incremental migration tools offline";
                String DEFAULT = "install mysql incremental migration tools";
            }
            interface Check{
                String ONLINE = "install mysql datacheck tools online";
                String OFFLINE = "install mysql datacheck tools offline";
                String DEFAULT = "install mysql datacheck tools";
            }

            interface ReverseMigration{
                String ONLINE = "install mysql reverse migration tools online";
                String OFFLINE = "install mysql reverse migration tools offline";
                String DEFAULT = "install mysql reverse migration tools";
            }
            interface All{
                String DEFAULT = "install mysql all migration tools";
            }
        }
    }
    interface Uninstall{
        interface Mysql{
            String FULL = "uninstall mysql full migration tools";
            String INCREMENTAL = "uninstall mysql incremental migration tools";
            String CHECK = "uninstall mysql datacheck tools";
            String REVERSE = "uninstall mysql reverse migration tools";
            String ALL = "uninstall mysql all migration tools";
        }
    }
    interface Start{
        interface Mysql{
            String FULL = "start mysql full migration";
            String INCREMENTAL = "start mysql incremental migration";
            String REVERSE = "start mysql reverse migration";
            String FULL_CHECK = "start mysql full migration datacheck";
            String INCREMENTAL_CHECK = "start mysql incremental migration datacheck";
            String REVERSE_CHECK = "start mysql reverse migration datacheck";
        }
        interface Plan{
            String PLAN1 = "start plan1";
            String PLAN2 = "start plan2";
            String PLAN3 = "start plan3";
            String CURRENT = "start current plan";
        }
    }
    interface Show{
        String PLAN = "show plans";
        String STATUS = "show status";
        String INFORMATION = "show information";
        String PARAMETERS = "show parameters";
    }
    interface Stop{
        String PLAN = "stop plan";
        String INCREMENTAL_MIGRATION = "stop incremental migration";
        String REVERSE_MIGRATION = "stop reverse migration";
    }
    interface Parameters{
        String ID = "workspace.id";
        String PATH = "path";
        String ACTION = "action";
        String TYPE = "type";
        String MIGRATION_TYPE = "migration.type";
        String PARAMETER = "parameter";
        String SKIP = "skip";
        String CHECK = "check";
        String ORDER = "order";
        interface Default{
            String ID = "1";
            String PATH = "";
            String ACTION = "";
            String TYPE = "";
            String MIGRATION_TYPE = "";
            String PARAMETER = "";
            String SKIP = "";
            String CHECK = "";
            String ORDER = "";
        }
    }
    interface Action{
        String HELP = "help";
        String SHOW = "show";
        String STOP = "stop";
        String INSTALL = "install";
        String UNINSTALL = "uninstall";
        String START = "start";
    }

    interface Run{
        String INCREMENTAL_MIGRATION = "run incremental migration";
        String REVERSE_MIGRATION = "run reverse migration";
    }
}
