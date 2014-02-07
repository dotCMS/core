#!/bin/sh

#Some constants
JARFILE=autoUpdater.jar
NEWFILE=autoUpdater.new
OLDFILE=autoUpdater.old
if [ ! -e  $JAVA_HOME/bin/java ]
then
	echo "JAVA_HOME needs to be set.  Aborting."
	exit 1
fi

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

PRGDIR=`dirname "$PRG"`

# Read an optional running configuration file
if [ "x$RUN_CONF" = "x" ]; then
    RUN_CONF="$PRGDIR/build.conf"
fi
if [ -r "$RUN_CONF" ]; then
    . "$RUN_CONF"
fi

# Get directory where we need to run from
AUTO_UPDATER_HOME=`cd "$PRGDIR/../autoupdater" ; pwd`
TOMCAT_HOME=`cd "$PRGDIR/../$SERVER_FOLDER" ; pwd`
DOTCMS_HOME=`cd "$PRGDIR/../$HOME_FOLDER" ; pwd`

if [ ! -e $AUTO_UPDATER_HOME/$JARFILE ]
then  
  echo "Couldn't find $JARFILE. Aborting."
  exit 1
fi
  
# Save the parameters for later
CMD_LINE_ARGS=""
while [ "$1" != "" ]
do
    CMD_LINE_ARGS="$CMD_LINE_ARGS $1"
    shift
done


SOURCE="-home"
if echo "$CMD_LINE_ARGS" | grep -q -- "$SOURCE"; then
  echo "home parameter not found setting default";
else
  DIRECTORY=$(cd `dirname $0` && pwd)
  BASE_FOLDER=`cd "$DIRECTORY/.." ; pwd`
  CMD_LINE_ARGS="$CMD_LINE_ARGS -home $BASE_FOLDER"
fi

SOURCE="-dotcms_home"
if echo "$CMD_LINE_ARGS" | grep -q -- "$SOURCE"; then
  echo "dotcms_home parameter not found setting default";
else
  CMD_LINE_ARGS="$CMD_LINE_ARGS -dotcms_home $HOME_FOLDER"
fi

SOURCE="-url"
if echo "$CMD_LINE_ARGS" | grep -q -- "$SOURCE"; then
  echo "url parameter not found setting default";
else
  CMD_LINE_ARGS="$CMD_LINE_ARGS -url http://www.dotcms.com/app/servlets/upgrade2x"
fi


echo "Running from $PRGDIR"

#Make sure we're clear to start
if [ -e $AUTO_UPDATER_HOME/$NEWFILE ]
then
  echo "$NEWFILE exists, it was most likely left by a failed attempt.  Please remove before starting again. Aborting."
  exit 1
fi

$JAVA_HOME/bin/java -jar $AUTO_UPDATER_HOME/autoUpdater.jar $CMD_LINE_ARGS

#echo "************************"
#echo "$AUTO_UPDATER_HOME/$NEWFILE"

if [ -e $AUTO_UPDATER_HOME/$NEWFILE ]
then
  # Make sure file is not zero length
  if [ -s $AUTO_UPDATER_HOME/$NEWFILE ]
  then
    echo "$NEWFILE exists, updating agent."
    mv $AUTO_UPDATER_HOME/$JARFILE $AUTO_UPDATER_HOME/$OLDFILE
    mv $AUTO_UPDATER_HOME/$NEWFILE $AUTO_UPDATER_HOME/$JARFILE
    echo "autoUpdater upgraded, restarting process."
    $JAVA_HOME/bin/java -jar $AUTO_UPDATER_HOME/autoUpdater.jar $CMD_LINE_ARGS
  fi
fi
