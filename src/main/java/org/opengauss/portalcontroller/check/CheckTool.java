package org.opengauss.portalcontroller.check;

public interface CheckTool {
    void installPackage();
    void changeParameters();
    void copyConfigFiles();
}
