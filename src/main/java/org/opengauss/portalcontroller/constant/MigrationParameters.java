package org.opengauss.portalcontroller.constant;

public interface MigrationParameters {
    String SNAPSHOT_OBJECT = "snapshot.object";
    interface Install {
        String FULL_MIGRATION = "default.install.mysql.full.migration.tools.way";
        String INCREMENTAL_MIGRATION = "default.install.mysql.incremental.migration.tools.way";
        String CHECK = "default.install.mysql.datacheck.tools.way";
    }
}
