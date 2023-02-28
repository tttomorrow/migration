package org.opengauss.portalcontroller;

import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;


/**
 * The type Runtime exec tools.
 */
public class RuntimeExecTools {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RuntimeExecTools.class);

    /**
     * Execute order.
     *
     * @param command       Command to execute.
     * @param time          Time with unit milliseconds.If timeout,the process will exit.
     * @param errorFilePath the error file path
     */
    public static void executeOrder(String command, int time, String errorFilePath) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        String[] commands = command.split(" ");
        processBuilder.command(commands);
        processBuilder.redirectError(new File(errorFilePath));
        try {
            Process process = processBuilder.start();
            String errorStr = getInputStreamString(process.getErrorStream());
            if (time == 0) {
                int retCode = process.waitFor();
                if (retCode == 0) {
                    LOGGER.info("Execute order finished.");
                } else {
                    if (!errorStr.equals("")) {
                        LOGGER.error(errorStr);
                    }
                }
            } else {
                process.waitFor(time, TimeUnit.MILLISECONDS);
                if (!errorStr.equals("")) {
                    LOGGER.error(errorStr);
                }
            }
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in execute command " + command);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted exception occurred in execute command " + command);
        }
    }

    /**
     * Execute order.
     *
     * @param command        the command
     * @param time           the time
     * @param workDirectory  the work directory
     * @param outputFilePath the output file path
     */
    public static void executeOrder(String command, int time, String workDirectory, String outputFilePath) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        String[] commands = command.split(" ");
        processBuilder.directory(new File(workDirectory));
        processBuilder.command(commands);
        processBuilder.redirectOutput(new File(outputFilePath));
        processBuilder.redirectError(new File(PortalControl.portalWorkSpacePath + "logs/error.log"));
        try {
            Process process = processBuilder.start();
            String errorStr = "";
            if (time == 0) {
                int retCode = process.waitFor();
                if (retCode == 0) {
                    LOGGER.info("Execute order finished.");
                } else {
                    errorStr = getInputStreamString(process.getErrorStream());
                    if (!errorStr.equals("")) {
                        LOGGER.error(errorStr);
                    }
                }
            } else {
                process.waitFor(time, TimeUnit.MILLISECONDS);
                errorStr = getInputStreamString(process.getErrorStream());
//                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//                String str = "";
//                String temp = "";
//                BufferedWriter bufferedOutputWriter = new BufferedWriter(new FileWriter(outputFilePath,true));
//                while((temp = bufferedReader.readLine()) != null){
//                    str += temp + System.lineSeparator();
//                    if(!temp.equals("")){
//                        bufferedOutputWriter.write(temp + System.lineSeparator());
//                        bufferedOutputWriter.flush();
//                    }
//                }
//                bufferedReader.close();
//                bufferedOutputWriter.close();
                if (!errorStr.equals("")) {
                    LOGGER.error(errorStr);
                }
                BufferedWriter bufferedErrorWriter = new BufferedWriter(new FileWriter(PortalControl.portalWorkSpacePath + "logs/error.log", true));
                if (!errorStr.equals("")) {
                    bufferedErrorWriter.write(errorStr);
                    bufferedErrorWriter.flush();
                }
                bufferedErrorWriter.close();
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
     * Execute order current runtime.
     *
     * @param cmdParts       the cmd parts
     * @param time           the time
     * @param outputFilePath the output file path
     */
    public static void executeOrderCurrentRuntime(String[] cmdParts, int time, String outputFilePath) {
        try {
            Process process = Runtime.getRuntime().exec(cmdParts);
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
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String str = bufferedReader.readLine();
                bufferedReader.close();
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFilePath, true));
                bufferedWriter.write(str);
                bufferedWriter.flush();
                bufferedWriter.write(errorStr);
                bufferedWriter.flush();
                bufferedWriter.close();
            }
        } catch (IOException e) {
            LOGGER.error("IO exception occurred in execute commands.");
            Tools.stopPortal();
            Thread.interrupted();
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted exception occurred in execute commands.");
            Tools.stopPortal();
            Thread.interrupted();
        }
    }


    /**
     * Execute order.
     *
     * @param cmdParts       the cmd parts
     * @param time           the time
     * @param workDirectory  the work directory
     * @param outputFilePath the output file path
     */
    public static void executeOrder(String[] cmdParts, int time, String workDirectory, String outputFilePath) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(cmdParts);
        processBuilder.directory(new File(workDirectory));
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(new File(outputFilePath));
        try {
            Process process = processBuilder.start();
            if (time == 0) {
                int retCode = process.waitFor();
                if (retCode == 0) {
                    LOGGER.info("Execute order finished.");
                } else {
                    LOGGER.error("Execute order failed.");
                }
            } else {
                process.waitFor(time, TimeUnit.MILLISECONDS);
            }

        } catch (IOException e) {
            LOGGER.error("IO exception occurred in execute commands.");
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted exception occurred in execute commands.");
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
        executeOrder(command, 600000, PortalControl.portalControlPath + "logs/error.log");
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
        String str = "";
        StringBuilder sb = new StringBuilder();
        try {
            while ((str = br.readLine()) != null) {
                sb.append(str + System.lineSeparator());
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
     * @param filePath  Filepath.
     * @param directory the directory
     */
    public static void copyFile(String filePath, String directory) {
        String command = "cp -R " + filePath + " " + directory;
        executeOrder(command, 60000, PortalControl.portalWorkSpacePath + "logs/error.log");
    }

    /**
     * Copy file not exist.
     *
     * @param filePath  the file path
     * @param directory the directory
     */
    public static void copyFileNotExist(String filePath, String directory) {
        if (new File(filePath).exists()) {
            String command = "cp -R " + filePath + " " + directory;
            executeOrder(command, 60000, PortalControl.portalWorkSpacePath + "logs/error.log");
        }
    }


    /**
     * Remove file.
     *
     * @param path          Filepath.
     * @param errorFilePath the error file path
     */
    public static void removeFile(String path, String errorFilePath) {
        if (new File(path).exists()) {
            String command = "rm -rf " + path;
            executeOrder(command, 60000, errorFilePath);
            LOGGER.info("Remove file " + path + " finished.");
        } else {
            LOGGER.info("No file " + path + " to remove.");
        }
    }

    /**
     * Unzip file.
     *
     * @param packagePath Package path.
     * @param directory   the directory
     */
    public static void unzipFile(String packagePath, String directory) {
        String command = "";
        if (!new File(packagePath).exists()) {
            LOGGER.error("No package to install.");
            Thread.interrupted();
        }
        if (packagePath.endsWith(".zip")) {
            command = "unzip " + packagePath + " " + directory;
            executeOrder(command, 300000, PortalControl.portalControlPath + "logs/error.log");
            LOGGER.info("Unzip file finished.");
        } else if (packagePath.endsWith(".tar.gz") || packagePath.endsWith(".tgz")) {
            command = "tar -zxf " + packagePath + " -C " + directory;
            executeOrder(command, 300000, PortalControl.portalControlPath + "logs/error.log");
            LOGGER.info("Unzip file " + packagePath + " to " + directory + " finished.");
        } else {
            LOGGER.error("Invalid package path.Please check if the package is ends with .zip or .tar.gz");
        }
    }

    /**
     * Rename.
     *
     * @param oldName the old name
     * @param newName the new name
     */
    public static void rename(String oldName, String newName) {
        String command = "mv " + oldName + " " + newName;
        executeOrder(command, 600000, PortalControl.portalWorkSpacePath + "logs/error.log");
        LOGGER.info("Rename file " + oldName + " to " + newName + " finished.");
    }
}
