/*
 * Copyright (c) 2022-2022 Huawei Technologies Co.,Ltd.
 *
 * openGauss is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *
 *           http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.opengauss.portalcontroller;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;

public class ToolsTest {
    @Test
    public void getSinglePropertiesParameterTest() throws Exception {
        File file = new File("mysql.properties");
        file.createNewFile();
        FileWriter fw = new FileWriter(file);
        fw.write("snapshot.mode=schema_only");
        fw.write("");
        fw.flush();
        fw.close();
        String str = Tools.getSinglePropertiesParameter("snapshot.mode", file.getCanonicalPath());
        assert str.equals("schema_only");
        file.delete();
    }

    @Test
    public void getPropertiesParameterTest() throws Exception {
        File file = new File("mysql.properties");
        file.createNewFile();
        FileWriter fw = new FileWriter(file);
        fw.write("name=cdc-connector_test4636" + System.lineSeparator());
        fw.write("database.user=ltt" + System.lineSeparator());
        fw.write("snapshot.mode=schema_only" + System.lineSeparator());
        fw.write("");
        fw.flush();
        fw.close();
        Hashtable<String, String> hashtable = Tools.getPropertiesParameters(file.getCanonicalPath());
        assert hashtable.get("name").equals("cdc-connector_test4636");
        assert hashtable.get("database.user").equals("ltt");
        assert hashtable.get("snapshot.mode").equals("schema_only");
        file.delete();
    }

    @Test
    public void getYmlParameterTest() throws IOException {
        File file = new File("mysql.yml");
        file.createNewFile();
        FileWriter fw = new FileWriter(file);
        fw.write("log_level: info" + System.lineSeparator());
        fw.write("pg_conn: " + System.lineSeparator());
        fw.write(" user: lty" + System.lineSeparator());
        fw.flush();
        fw.close();
        String str = Tools.getSingleYmlParameter("log_level", file.getAbsolutePath());
        assert str.equals("info");
        str = Tools.getSingleYmlParameter("pg_conn.user", file.getCanonicalPath());
        assert str.equals("lty");
        file.delete();
    }

    @Test
    public void changeSinglePropertiesParameterTest() throws Exception {
        File file = new File("mysql.properties");
        file.createNewFile();
        String path = file.getAbsolutePath();
        FileWriter fw = new FileWriter(file);
        fw.write("snapshot.mode=schema_only" + System.lineSeparator());
        fw.write("");
        fw.flush();
        fw.close();
        Tools.changeSinglePropertiesParameter("snapshot.mode", "schema_only_test", path);
        String str = Tools.getSinglePropertiesParameter("snapshot.mode", path);
        assert str.equals("schema_only_test");
        file.delete();
    }

    @Test
    public void changePropertiesParametersTest() throws Exception {
        File file = new File("mysql.properties");
        file.createNewFile();
        String path = file.getAbsolutePath();
        FileWriter fw = new FileWriter(file);
        fw.write("snapshot.mode=schema_only" + System.lineSeparator());
        fw.write("database.user=ltt" + System.lineSeparator());
        fw.flush();
        fw.close();
        Hashtable<String, String> table = new Hashtable<>();
        table.put("name", "test");
        table.put("snapshot.mode", "schema_only_test");
        Tools.changePropertiesParameters(table, path);
        String str = Tools.getSinglePropertiesParameter("name", path);
        assert str.equals("test");
        str = Tools.getSinglePropertiesParameter("database.user", path);
        assert str.equals("ltt");
        str = Tools.getSinglePropertiesParameter("snapshot.mode", path);
        assert str.equals("schema_only_test");
        file.delete();
    }

    @Test
    public void changeSingleYmlParameterTest() throws IOException {
        File file = new File("mysql.yml");
        file.createNewFile();
        String path = file.getAbsolutePath();
        FileWriter fw = new FileWriter(file);
        fw.write("port: 1234" + System.lineSeparator());
        fw.flush();
        fw.close();
        Tools.changeSingleYmlParameter("port", "2345", path);
        String str = Tools.getSingleYmlParameter("port", path);
        assert str.equals("2345");
        Tools.changeSingleYmlParameter("pg_conn.database", "test123", path);
        str = Tools.getSingleYmlParameter("pg_conn.database", path);
        assert str.equals("test123");
        file.delete();
    }

    @Test
    public void changeYmlParametersTest() throws IOException {
        File file = new File("mysql.properties");
        file.createNewFile();
        String path = file.getAbsolutePath();
        FileWriter fw = new FileWriter(file);
        fw.write("port: 2345" + System.lineSeparator());
        fw.write("pg_conn: " + System.lineSeparator());
        fw.write(" database: test" + System.lineSeparator());
        fw.write("");
        fw.flush();
        fw.close();
        HashMap<String, Object> hashmap = new HashMap<>();
        hashmap.put("pg_conn.database", "test1234");
        hashmap.put("port", "1234");
        hashmap.put("testtest", "test");
        hashmap.put("test.test", "test");
        Tools.changeYmlParameters(hashmap, path);
        String str = Tools.getSingleYmlParameter("pg_conn.database", path);
        assert str.equals("test1234");
        str = Tools.getSingleYmlParameter("port", path);
        assert str.equals("1234");
        str = Tools.getSingleYmlParameter("testtest", path);
        assert str.equals("test");
        str = Tools.getSingleYmlParameter("test.test", path);
        assert str.equals("test");
        file.delete();
    }

    @Test
    public void lastLineTest() throws IOException {
        File file = new File("mysql.log");
        file.createNewFile();
        String path = file.getAbsolutePath();
        FileWriter fw = new FileWriter(file);
        fw.write("2022-12-19 20:01:51 MainProcess INFO: test1" + System.lineSeparator());
        fw.write("2022-12-19 20:01:51 MainProcess INFO: test2" + System.lineSeparator());
        fw.write("2022-12-19 20:01:51 MainProcess INFO: start_proc_replica finished." + System.lineSeparator());
        fw.flush();
        fw.close();
        String lastLine = Tools.lastLine(path);
        String str = "2022-12-19 20:01:51 MainProcess INFO: start_proc_replica finished.";
        assert str.equals(lastLine);
    }
}
