#!/bin/bash
APP_NAME=portalControl-1.0-SNAPSHOT-exec.jar
START_ORDER=start_mysql_full_migration
STOP_ORDER=stop_plan
INSTALL_ORDER=install_mysql_full_migration_tools
UNINSTALL_ORDER=uninstall_mysql_full_migration_tools
SIGN="-Dworkspace.id=$2"
NAME=$2
PORTAL_PATH="$PWD/"
SKIP=true

#使用说明，用来提示输入参数
usage() {
echo "Usage: sh 脚本名.sh [start|stop|install|uninstall] workspace.id"
echo "workspace.id is id of migration plan"
exit 1
}

#检查程序是否在运行
is_exist() {
pid=`ps -ef|grep $SIGN |grep $APP_NAME |grep -v grep|awk '{print $NAME}' `
#如果不存在返回1，存在返回0
if [ -z "${pid}" ]; then
return 1
else
return 0
fi
}

#安装方法
install(){
java -Dpath=${PORTAL_PATH} -Dskip=${SKIP} -Dworkspace.id=${NAME} -Dorder=${INSTALL_ORDER} -jar $APP_NAME &
wait
}

#启动方法
start(){
is_exist
if [ $? -eq "0" ]; then
echo "Migration plan $NAME is already running. pid=${pid} ."
else
java -Dpath=${PORTAL_PATH} -Dskip=${SKIP} -Dworkspace.id=${NAME} -Dorder=${START_ORDER} -jar $APP_NAME &
wait
fi
}

#停止方法
stop(){
java -Dpath=${PORTAL_PATH} -Dskip=${SKIP} -Dworkspace.id=${NAME} -Dorder=${STOP_ORDER} -jar $APP_NAME &
wait
echo "Stop migration plan $NAME"
}

#卸载方法
uninstall(){
java -Dpath=${PORTAL_PATH} -Dskip=${SKIP} -Dworkspace.id=${NAME} -Dorder=${UNINSTALL_ORDER} -jar $APP_NAME &
wait
}

#根据输入参数，选择执行对应方法，不输入则执行使用说明
case "$1" in
"start")
start
;;
"stop")
stop
;;
"install")
install
;;
"uninstall")
uninstall
;;
*)
usage
;;
esac

