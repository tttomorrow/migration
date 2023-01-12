package org.opengauss.portalcontroller;

import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;


public class RuntimeExecTools {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RuntimeExecTools.class);

    /**
     * Execute order.
     *
     * @param command Command to execute.
     * @param time    Time with unit milliseconds.If timeout,the process will exit.
     */
    public static void executeOrder(String command, int time) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        String[] commands = command.split(" ");
        processBuilder.command(commands);
        processBuilder.redirectError(new File(PortalControl.portalControlPath + "logs/error.log"));
        try {
            Process process = processBuilder.start();
            String errorStr = getInputStreamString(process.getErrorStream());
            if (time == 0) {
                int retCode = process.waitFor();
                if (retCode == 0) {
                    LOGGER.info("Execute order finished.");
                } else {
                    LOGGER.error(errorStr);
                }
            } else {
                process.waitFor(time, TimeUnit.MILLISECONDS);
            }
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in execute command " + command);
            Thread.interrupted();
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted exception occurred in execute command " + command);
            Thread.interrupted();
        }
    }

    /**
     * Execute order.
     *
     * @param urlParameter  Url parameter.
     * @param pathParameter Path parameter.
     */
    public static void download(String urlParameter, String pathParameter) {
        String url = PortalControl.toolsConfigParametersTable.get(urlParameter);
        String path = PortalControl.toolsConfigParametersTable.get(pathParameter);
        String[] urlParameters = url.split("/");
        String packageName = urlParameters[urlParameters.length - 1];
        Tools.createFile(path, false);
        File file = new File(path + packageName);
        if (file.exists() && file.isDirectory()) {
            LOGGER.error("Directory " + path + packageName + " has existed.Download failed.");
            Thread.interrupted();
        }
        String command = "wget -c -P " + path + " " + url + " --no-check-certificate";
        executeOrder(command, 600000);
        LOGGER.info("Download file " + url + " to " + path + " finished.");
    }

    /**
     * Execute order.
     *
     * @param in Inputstream.
     * @return String input.
     */
    public static String getInputStreamString(InputStream in) {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String str;
        StringBuilder sb = new StringBuilder();
        try {
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in get inputStream.");
            Thread.interrupted();
        }
        return sb.toString();
    }

    /**
     * Copy file.
     *
     * @param filePath Filepath.
     */
    public static void copyFile(String filePath, String directory) {
        String command = "cp " + filePath + " " + directory;
        executeOrder(command, 60000);
        LOGGER.info("Copy file " + filePath + " to " + directory + " finished.");
    }

    /**
     * Remove file.
     *
     * @param path Filepath.
     */
    public static void removeFile(String path) {
        String command = "rm -rf " + path;
        executeOrder(command, 60000);
        LOGGER.info("Remove file " + path + " finished.");
    }

    /**
     * Unzip file.
     *
     * @param packagePath Package path.
     */
    public static void unzipFile(String packagePath, String directory) {
        String command = "";
        if (!new File(packagePath).exists()) {
            LOGGER.error("No package to install.");
            Thread.interrupted();
        }
        if (packagePath.endsWith(".zip")) {
            command = "unzip " + packagePath + " " + directory;
            executeOrder(command, 300000);
            LOGGER.info("Unzip file finished.");
        } else if (packagePath.endsWith(".tar.gz") || packagePath.endsWith(".tgz")) {
            command = "tar -zxf " + packagePath + " -C " + directory;
            executeOrder(command, 300000);
            LOGGER.info("Unzip file " + packagePath + " to " + directory + " finished.");
        } else {
            LOGGER.error("Invalid package path.Please check if the package is ends with .zip or .tar.gz");
        }
    }
}
