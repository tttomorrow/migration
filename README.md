# openGauss-migration-portal

#### 介绍
{**以下是 Gitee 平台说明，您可以替换此简介**
Gitee 是 OSCHINA 推出的基于 Git 的代码托管平台（同时支持 SVN）。专为开发者提供稳定、高效、安全的云端软件开发协作平台
无论是个人、团队、或是企业，都能够用 Gitee 实现代码托管、项目管理、协作开发。企业项目请看 [https://gitee.com/enterprises](https://gitee.com/enterprises)}

 #### 文件结构： 

   ```
   /portal
   	config/    
   		migrationConfig.properties
   		toolspath.properties
   		status
   		currentPlan
   		input
   		chameleon/
           	config-example.yml
           datacheck/
           	application-source.yml
           	application-sink.yml
           	application.yml
           	log4j2.xml
           	log4j2source.xml
           	log4j2sink.xml
           debezium/
               connect-avro-standalone.properties
           	mysql-sink.properties
           	mysql-source.properties
           	opengauss-sink.properties
           	opengauss-source.properties
   	logs/      
   		portal.log 
   	pkg/           
           chameleon/
           	chameleon-3.1.1-py3-none-any.whl
           datacheck/
           	openGauss-datachecker-performance-3.1.0.tar.gz
           debezium/
           	confluent-5.5.1.tar.gz
           	debezium-connector-mysql-1.8.1.Final-plugin.tar.gz
           	debezium-connector-opengauss-1.8.1.Final-plugin.tar.gz
           	kafka_2.13-3.2.3.tgz
       tmp/
       tools/
           chameleon/
           datacheck/
           debezium/
           	confluent-5.5.1/
           	kafka_2.13-3.2.3/
           	plugin/
           		debezium-connector-mysql/
           		debezium-connector-opengauss/
       portal.lock
       portalControl-1.0-SNAPSHOT-exec.jar
       README.md
   ```

#### 安装教程

工作目录为portal的安装目录，默认为/ops/portal，工作目录可根据实际需要更换。

##### 安装portal

1.下载源代码，将源代码中的portal文件夹复制到/ops下。

编译源代码得到jar包portalControl-1.0-SNAPSHOT-exec.jar，并将jar包放在/ops/portal下。

java版本：open JDK11及以上

maven版本：3.8.3以上

##### 启动方式

使用java -jar -Dpath=/ops/portal/ -Dskip=true -Dorder=指令 -jar portalControl-1.0-SNAPSHOT-exec.jar启动portal，通过指令使用portal的各项功能。

其中path的值为工作目录，如果这里输入错误会导致portal报错，并且要以/结尾，指令为数个单词之间加空格，比如"start mysql full migration"这种形式，但使用order参数传入时，需要把空格换成下划线。

##### 安装迁移工具

在/ops/portal/config/toolspath.properties下修改工具安装路径：

| 参数名称                     | 参数说明                                                     |
| ---------------------------- | ------------------------------------------------------------ |
| chameleon.venv.path          | 变色龙虚拟环境所在位置                                       |
| chameleon.pkg.path           | 变色龙的安装包所在路径                                       |
| chameleon.pkg.name           | 变色龙的安装包名                                             |
| chameleon.pkg.url            | 变色龙的安装包下载链接                                       |
| debezium.path                | debezium+kafka所在路径（默认kafka、confluent、connector都安装在该路径下） |
| kafka.path                   | kafka所在路径                                                |
| confluent.path               | confluent所在路径                                            |
| connector.path               | connector所在路径                                            |
| debezium.pkg.path            | debezium+kafka安装包所在路径（默认kafka、confluent、connector安装包都在该路径下） |
| kafka.pkg.name               | kafka安装包名                                                |
| kafka.pkg.url                | kafka安装包下载链接                                          |
| confluent.pkg.name           | confluent安装包名                                            |
| confluent.pkg.url            | confluent安装包下载链接                                      |
| connector.mysql.pkg.name     | mysql connector安装包名                                      |
| connector.mysql.pkg.url      | mysql connector安装包下载链接                                |
| connector.opengauss.pkg.name | opengauss connector安装包名                                  |
| connector.opengauss.pkg.url  | opengauss connector安装包下载链接                            |
| datacheck.install.path       | datacheck安装路径                                            |
| datacheck.path               | datacheck所在路径                                            |
| datacheck.pkg.path           | datacheck安装包所在路径                                      |
| datacheck.pkg.name           | datacheck安装包名                                            |
| datacheck.pkg.url            | datachec安装包下载链接                                       |

工具的安装支持离线安装和在线安装，在线安装将会从指定链接下载安装包到安装包指定位置，离线不会。如果输入命令时不指定安装方式，那么portal会根据/ops/portal/config/migrationConfig.properties下的参数决定安装方式：

| 参数名称                                              | 参数说明                                              |
| ----------------------------------------------------- | ----------------------------------------------------- |
| default.install.mysql.full.migration.tools.way        | 全量迁移工具默认安装方式：offline为离线，online为在线 |
| default.install.mysql.incremental.migration.tools.way | 增量迁移工具默认安装方式：offline为离线，online为在线 |
| default.install.mysql.datacheck.tools.way             | 数据校验工具默认安装方式：offline为离线，online为在线 |
| default.install.mysql.reverse.migration.tools.way     | 反向迁移工具默认安装方式：offline为离线，online为在线 |

安装指令：

| 指令名称                                          | 指令说明                                          |
| ------------------------------------------------- | ------------------------------------------------- |
| install mysql full migration tools online         | 在线安装mysql全量迁移工具                         |
| install mysql full migration tools offline        | 离线安装mysql全量迁移工具                         |
| install mysql full migration tools                | 安装mysql全量迁移工具（安装方式由配置文件指定）   |
| install mysql incremental migration tools online  | 在线安装mysql增量迁移工具                         |
| install mysql incremental migration tools offline | 离线安装mysql增量迁移工具                         |
| install mysql incremental migration tools         | 安装mysql增量迁移工具（安装方式由配置文件指定）   |
| install mysql datacheck tools online              | 在线安装mysql数据校验工具                         |
| install mysql datacheck tools offline             | 离线安装mysql数据校验工具                         |
| install mysql datacheck tools                     | 安装mysql数据校验工具（安装方式由配置文件指定）   |
| install mysql all migration tools                 | 安装mysql迁移工具（各工具安装方式由配置文件指定） |

##### 配置参数



##### 执行迁移计划

portal支持启动多个进程执行不同的迁移计划，启动迁移计划时需要添加参数-Dworkspace.id="ID"，这样不同的迁移计划可以根据不同的workspaceID进行区分，如果不添加的话，workspaceID默认值为1。在执行计划

用控制台进行操作的情况下：

1. 使用java -jar -Dpath=/ops/portal/ -jar portalControl-1.0-SNAPSHOT-exec.jar启动portal，输入install mysql migration tools安装全部迁移工具

2. 在currentPlan中输入指令制定计划，或者使用默认计划plan1,plan2,plan3，输入show plans查看默认计划。

3. 启动计划

   输入start current plan可以执行currentPlan中的计划，输入start plan1使用默认计划1，默认计划2，3以此类推。

4. 停止计划

   输入stop plan停止计划。

5. 退出

   输入exit退出。

   

   不使用控制台进行操作的情况下：

   使用java -jar -Dpath=/data1/lt/test/portal/ -Dorder=install_mysql_all_migration_tools -Dskip=true -jar portalControl-1.0-SNAPSHOT-exec.jar启动portal

   在/opt/portal/config/input文件中写入start current plan启动计划，一次只能写入一条指令，且每次写入新指令都要另起一行，不能删除之前的指令

   在/opt/portal/config/input文件中写入stop plan停止计划。

   在/opt/portal/config/input文件中写入exit退出计划。



##### 指令列表

| 指令名称                                          | 指令说明                                                     |
| ------------------------------------------------- | ------------------------------------------------------------ |
| install mysql full migration tools online         | 在线安装mysql全量迁移工具                                    |
| install mysql full migration tools offline        | 离线安装mysql全量迁移工具                                    |
| install mysql full migration tools                | 安装mysql全量迁移工具（安装方式由配置文件指定）              |
| install mysql incremental migration tools online  | 在线安装mysql增量迁移工具                                    |
| install mysql incremental migration tools offline | 离线安装mysql增量迁移工具                                    |
| install mysql incremental migration tools         | 安装mysql增量迁移工具（安装方式由配置文件指定）              |
| install mysql datacheck tools online              | 在线安装mysql数据校验工具                                    |
| install mysql datacheck tools offline             | 离线安装mysql数据校验工具                                    |
| install mysql datacheck tools                     | 安装mysql数据校验工具（安装方式由配置文件指定）              |
| install mysql all migration tools                 | 安装mysql迁移工具（各工具安装方式由配置文件指定）            |
| uninstall mysql full migration tools              | 卸载mysql全量迁移工具                                        |
| uninstall mysql incremental migration tools       | 卸载mysql增量迁移工具                                        |
| uninstall mysql datacheck tools                   | 卸载mysql数据校验工具                                        |
| uninstall mysql all migration tools               | 卸载mysql迁移工具                                            |
| start mysql full migration                        | 开始mysql全量迁移                                            |
| start mysql incremental migration                 | 开始mysql增量迁移                                            |
| start mysql reverse migration                     | 开始mysql反向迁移                                            |
| start mysql full migration datacheck              | 开始mysql全量校验                                            |
| start mysql incremental migration datacheck       | 开始mysql增量校验                                            |
| start mysql reverse migration datacheck           | 开始mysql反向校验                                            |
| start plan1                                       | 开始默认计划plan1                                            |
| start plan2                                       | 开始默认计划plan2                                            |
| start plan3                                       | 开始默认计划plan3                                            |
| start current plan                                | 开始当前计划（currentPlan中的计划）                          |
| show plans                                        | 显示默认计划                                                 |
| show information                                  | 显示数据库相关信息，包括mysql和openGuass端的数据库名、用户名、密码、ip、端口等 |
| show parameters                                   | 显示命令参数                                                 |
| stop plan                                         | 停止计划                                                     |

#### 注意事项

1.目前portal仅集成了全量迁移、全量校验、增量迁移、反向迁移。一旦执行增量之后执行过反向，就不能再次执行增量，否则会引起数据不一致问题。
2.portal使用chameleon进行全量迁移，使用kafka和confluent进行增量与反向迁移（其中需要使用curl工具将数据转换成avro格式），使用datacheck进行数据校验。


#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 特技

1.  使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2.  Gitee 官方博客 [blog.gitee.com](https://blog.gitee.com)
3.  你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解 Gitee 上的优秀开源项目
4.  [GVP](https://gitee.com/gvp) 全称是 Gitee 最有价值开源项目，是综合评定出的优秀开源项目
5.  Gitee 官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6.  Gitee 封面人物是一档用来展示 Gitee 会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)
