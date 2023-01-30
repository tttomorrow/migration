package org.opengauss.portalcontroller.check;

public interface CheckTask {
    void installAllPackages();
    void changeParameters(String workspaceId);
    void copyConfigFiles(String workspaceId);
    void prepareWork(String workspaceId);
    void start(String workspaceId);

    void checkEnd();
}
