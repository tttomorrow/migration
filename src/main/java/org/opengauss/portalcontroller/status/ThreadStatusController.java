package org.opengauss.portalcontroller.status;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.opengauss.portalcontroller.Plan;
import org.opengauss.portalcontroller.PortalControl;
import org.opengauss.portalcontroller.RuntimeExecTools;
import org.opengauss.portalcontroller.Tools;
import org.opengauss.portalcontroller.constant.Chameleon;
import org.opengauss.portalcontroller.constant.Debezium;
import org.opengauss.portalcontroller.constant.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The type Thread status controller.
 */
public class ThreadStatusController extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadStatusController.class);
    private String workspaceId;

    /**
     * Gets workspace id.
     *
     * @return the workspace id
     */
    public String getWorkspaceId() {
        return workspaceId;
    }

    /**
     * Sets workspace id.
     *
     * @param workspaceId the workspace id
     */
    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    /**
     * The Exit.
     */
    public boolean exit = false;

    /**
     * The constant portalStatusWriterArrayList.
     */
    public static ArrayList<PortalStatusWriter> portalStatusWriterArrayList = new ArrayList<>();

    @Override
    public void run() {
        while (!exit) {
            try {
                int time = 0;
                FileWriter fw = new FileWriter(new File(PortalControl.portalWorkSpacePath + "status/portal.txt"));
                PortalStatusWriter portalStatusWriter = new PortalStatusWriter(PortalControl.status, System.currentTimeMillis());
                portalStatusWriterArrayList.add(portalStatusWriter);
                String str = JSON.toJSONString(portalStatusWriterArrayList);
                fw.write(str);
                fw.flush();
                fw.close();
                String chameleonVenvPath = PortalControl.toolsConfigParametersTable.get(Chameleon.VENV_PATH);
                String path = chameleonVenvPath + "data_default_" + Plan.workspaceId + "_init_replica.json";
                if (new File(path).exists()) {
                    Tools.changeFullStatus();
                }
                if (PortalControl.status < Status.START_REVERSE_MIGRATION && PortalControl.status > Status.FULL_MIGRATION_CHECK_FINISHED) {
                    String sourceIncrementalStatusPath = PortalControl.portalWorkSpacePath + "status/incremental/forward-source-process.txt";
                    String sinkIncrementalStatusPath = PortalControl.portalWorkSpacePath + "status/incremental/forward-sink-process.txt";
                    String incrementalStatusPath = PortalControl.portalWorkSpacePath + "status/incremental_migration.txt";
                    if (new File(sourceIncrementalStatusPath).exists() && new File(sinkIncrementalStatusPath).exists()) {
                        time = Tools.changeIncrementalStatus(sourceIncrementalStatusPath, sinkIncrementalStatusPath, incrementalStatusPath);
                    }
                }
                if (PortalControl.status >= Status.START_REVERSE_MIGRATION && PortalControl.status != Status.ERROR) {
                    String sourceReverseStatusPath = PortalControl.portalWorkSpacePath + "status/reverse/reverse-source-process.txt";
                    String sinkReverseStatusPath = PortalControl.portalWorkSpacePath + "status/reverse/reverse-sink-process.txt";
                    String reverseStatusPath = PortalControl.portalWorkSpacePath + "status/reverse_migration.txt";
                    if (new File(sourceReverseStatusPath).exists() && new File(sinkReverseStatusPath).exists()) {
                        time = Tools.changeIncrementalStatus(sourceReverseStatusPath, sinkReverseStatusPath, reverseStatusPath);
                    }
                }
                String kafkaPath = PortalControl.toolsConfigParametersTable.get(Debezium.Kafka.PATH);
                String confluentPath = PortalControl.toolsConfigParametersTable.get(Debezium.Confluent.PATH);
                RuntimeExecTools.copyFileNotExist(kafkaPath + "logs/server.log", PortalControl.portalWorkSpacePath + "logs/debezium/server.log");
                RuntimeExecTools.copyFileNotExist(confluentPath + "logs/schema-registry.log", PortalControl.portalWorkSpacePath + "logs/debezium/schema-registry.log");
                RuntimeExecTools.copyFileNotExist(confluentPath + "logs/connect_" + workspaceId + ".log", PortalControl.portalWorkSpacePath + "logs/debezium/connect.log");
                RuntimeExecTools.copyFileNotExist(confluentPath + "logs/connect_" + workspaceId + "_reverse.log", PortalControl.portalWorkSpacePath + "logs/debezium/reverse_connect.log");
                Thread.sleep(1000 - time);
            } catch (IOException e) {
                LOGGER.error("Write status failed.");
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted exception.");
            }
        }
    }
}
