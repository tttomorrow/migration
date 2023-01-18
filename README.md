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
           debezium/
           	mysql-sink.properties
           	mysql-source.properties
           	opengauss-sink.properties
           	opengauss-source.properties
   	logs/      
   		portal.log 
   	pkg/           
           chameleon/
           	chameleon-3.1.0-py3-none-any.whl
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

#### 安装及使用教程

假设工作目录为/opt/portal，工作目录可根据实际需要更换。

下载源代码，将源代码中的portal文件夹复制到/opt下。然后配置toolspath.properties文件指定文件安装及所在位置。

使用控制台进行操作的情况下：

1. 使用java -jar -Dpath=/opt/portal/ -jar portalControl-1.0-SNAPSHOT-exec.jar启动portal，输入install mysql migration tools安装全部迁移工具

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

##### 启动方式

使用java -jar -Dpath=/opt/portal/ -jar portalControl-1.0-SNAPSHOT-exec.jar启动portal

其中-Dpath=/opt/portal/是必加项，path的值是portal所在位置，如果不加会导致找不到配置文件，无法正常运行，并且要以/结尾。

可以在参数中添加-Dskip=true，此时不使用控制台进行输入输出，如果添加此项，可以通过向/opt/portal/config/input文件中添加命令对portal进行操作，或者添加其他参数，比如数据库类型，迁移类型，是否校验等等，portal将接收这些参数拼接成对应指令，也可以使用-Dorder=start_mysql_full_migration直接传指令。指令为数个单词之间加空格，比如"start mysql full migration"这种形式，但使用order参数传入时，需要把空格换成下划线。

如果不添加-Dskip=true，则使用控制台进行输入输出，除了工作目录之外的参数全部无效。

进入portal界面后可以使用install相关命令安装工具，使用show information查看并修改需要迁移的数据库的用户名、密码、ip、port、数据库名等，然后可以启动迁移计划。

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

目前portal仅集成了全量迁移、全量校验、增量迁移、反向迁移。且增量迁移和反向迁移不能放在同一个计划中执行，一旦执行增量之后执行过反向，就不能再次执行增量，否则会引起数据不一致问题。

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
