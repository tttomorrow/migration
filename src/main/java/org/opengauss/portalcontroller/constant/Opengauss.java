package org.opengauss.portalcontroller.constant;

public interface Opengauss {
    String USER = "opengauss.user.name";
    String PASSWORD = "opengauss.user.password";
    String DATABASE_HOST = "opengauss.database.host";
    String DATABASE_PORT = "opengauss.database.port";
    String DATABASE_NAME = "opengauss.database.name";
    String DATABASE_SCHEMA = "opengauss.database.schema";
    interface Default{
        String USER = "";
        String PASSWORD = "";
        String DATABASE_HOST = "127.0.0.1";
        String DATABASE_PORT = "3306";
        String DATABASE_NAME = "";
        String DATABASE_SCHEMA = "";
    }
}
