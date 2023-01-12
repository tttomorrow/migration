package org.opengauss.portalcontroller.constant;

public interface Check {
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
}
