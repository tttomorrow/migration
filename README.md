# openGauss-migration-portal

#### 介绍
{**以下是 Gitee 平台说明，您可以替换此简介**
Gitee 是 OSCHINA 推出的基于 Git 的代码托管平台（同时支持 SVN）。专为开发者提供稳定、高效、安全的云端软件开发协作平台
无论是个人、团队、或是企业，都能够用 Gitee 实现代码托管、项目管理、协作开发。企业项目请看 [https://gitee.com/enterprises](https://gitee.com/enterprises)}

 #### 默认文件结构： 

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
       portal.portId.lock
       portalControl-1.0-SNAPSHOT-exec.jar
       README.md
   ```

#### 安装教程

portal的安装目录默认为/ops/portal，可根据实际需要更换。

##### 安装portal

下载源代码，将源代码中的portal文件夹复制到/ops下。

编译源代码得到jar包portalControl-1.0-SNAPSHOT-exec.jar，并将jar包放在/ops/portal下。

java版本：open JDK11及以上

maven版本：3.8.1以上

##### 启动方式

使用java -jar -Dpath=/ops/portal/ -Dskip=true -Dorder=指令 -Dworkspace.id=1 -jar portalControl-1.0-SNAPSHOT-exec.jar启动portal，通过指令使用portal的各项功能。

其中path的值为工作目录，如果这里输入错误会导致portal报错，并且要以/结尾。

指令为数个单词之间加空格，比如"start mysql full migration"这种形式，但使用order参数传入时，需要把空格换成下划线。

portal会在workspace文件夹下创造对应id的文件夹，并将执行任务时的参数和日志等信息存入该文件夹。如果不指定workspace.id，那么workspace的默认id为1。参数优先级：命令行输入 > workspace下设置的参数 > 公共空间参数。建议每次运行迁移任务时使用不同的workdspaceid。

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
| datacheck.pkg.url            | datacheck安装包下载链接                                      |

工具的安装支持离线安装和在线安装，在线安装将会从指定链接下载安装包到安装包指定位置，离线不会。如果输入命令时不指定安装方式，那么portal会根据/ops/portal/config/migrationConfig.properties下的参数决定安装方式：

| 参数名称                                              | 参数说明                                              |
| ----------------------------------------------------- | ----------------------------------------------------- |
| default.install.mysql.full.migration.tools.way        | 全量迁移工具默认安装方式：offline为离线，online为在线 |
| default.install.mysql.incremental.migration.tools.way | 增量迁移工具默认安装方式：offline为离线，online为在线 |
| default.install.mysql.datacheck.tools.way             | 数据校验工具默认安装方式：offline为离线，online为在线 |
| default.install.mysql.reverse.migration.tools.way     | 反向迁移工具默认安装方式：offline为离线，online为在线 |

使用以下指令可以安装对应的迁移工具，举例：

java -jar -Dpath=/ops/portal/ -Dskip=true -Dorder=install_mysql_full_migration_tools_online -Dworkspace.id=1 -jar portalControl-1.0-SNAPSHOT-exec.jar

在命令行运行这条命令可以从指定的链接下载并安装所有迁移功能用到的迁移工具。（安装包会放在toolspath.properties指定的路径下）

##### 安装指令：

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

用户可以在/ops/portal/config/migrationConfig.properties修改迁移所用参数。

参数优先级：命令行输入 > workspace下设置的参数 > 公共空间参数。所以如果使用之前用过的workspaceid执行任务，请在/ops/portal/workspace/要使用的ID/config/migrationConfig.properties下面修改参数。

| 参数名称                  | 参数说明                |
| ------------------------- | ----------------------- |
| mysql.user.name           | mysql数据库用户名       |
| mysql.user.password       | mysql数据库用户密码     |
| mysql.database.host       | mysql数据库ip           |
| mysql.database.port       | mysql数据库端口         |
| mysql.database.name       | mysql数据库名           |
| opengauss.user.name       | openGauss数据库用户名   |
| opengauss.user.password   | openGauss数据库用户密码 |
| opengauss.database.host   | openGauss数据库ip       |
| opengauss.database.port   | openGauss数据库端口     |
| opengauss.database.name   | openGauss数据库名       |
| opengauss.database.schema | openGauss数据库模式名   |

##### 执行迁移计划

portal支持启动多个进程执行不同的迁移计划，启动迁移计划时需要添加参数-Dworkspace.id="ID"，这样不同的迁移计划可以根据不同的workspaceID进行区分，如果不添加的话，workspaceID默认值为1。

举例：

启动全量迁移：

java -jar -Dpath=/ops/portal/ -Dskip=true -Dorder=start_mysql_full_migration -Dworkspace.id=2 -jar portalControl-1.0-SNAPSHOT-exec.jar

portal除了支持单项任务的启动与停止，也会提供一些组合的默认计划：

启动包括全量迁移和全量校验在内的迁移计划：

java -jar -Dpath=/ops/portal/ -Dskip=true -Dorder=start_plan1 -Dworkspace.id=3 -jar portalControl-1.0-SNAPSHOT-exec.jar

##### 计划列表

| 计划名称 | 包括指令                                     |
| -------- | -------------------------------------------- |
| plan1    | 全量迁移→全量校验                            |
| plan2    | 全量迁移→全量校验→增量迁移→增量校验          |
| plan3    | 全量迁移→全量校验→增量迁移→增量校验→反向迁移 |

以下为启动迁移计划的指令列表：

##### 指令列表

| 指令名称                                    | 指令说明                                                     |
| ------------------------------------------- | ------------------------------------------------------------ |
| start mysql full migration                  | 开始mysql全量迁移                                            |
| start mysql incremental migration           | 开始mysql增量迁移                                            |
| start mysql reverse migration               | 开始mysql反向迁移                                            |
| start mysql full migration datacheck        | 开始mysql全量校验                                            |
| start mysql incremental migration datacheck | 开始mysql增量校验                                            |
| start plan1                                 | 开始默认计划plan1                                            |
| start plan2                                 | 开始默认计划plan2                                            |
| start plan3                                 | 开始默认计划plan3                                            |
| show plans                                  | 显示默认计划                                                 |
| show information                            | 显示数据库相关信息，包括mysql和openGuass端的数据库名、用户名、密码、ip、端口等 |
| show parameters                             | 显示命令参数                                                 |
| stop plan                                   | 停止计划                                                     |

##### 卸载迁移工具

使用以下指令可以卸载不同功能对应的迁移工具，举例：

java -jar -Dpath=/ops/portal/ -Dskip=true -Dorder=uninstall_mysql_full_migration_tools_online -Dworkspace.id=1 -jar portalControl-1.0-SNAPSHOT-exec.jar

在命令行运行这条命令可以卸载所有功能用到的迁移工具。（安装包会放在toolspath.properties指定的路径下）

| 指令名称                                    | 指令说明              |
| ------------------------------------------- | --------------------- |
| uninstall mysql full migration tools        | 卸载mysql全量迁移工具 |
| uninstall mysql incremental migration tools | 卸载mysql增量迁移工具 |
| uninstall mysql datacheck tools             | 卸载mysql数据校验工具 |
| uninstall mysql all migration tools         | 卸载mysql迁移工具     |

#### 注意事项

1.目前portal仅集成了全量迁移、全量校验、增量迁移、反向迁移。一旦执行增量迁移之后执行过反向迁移，就不能再次执行增量迁移，否则会引起数据不一致问题。
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
