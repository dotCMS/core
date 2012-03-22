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

# Get directory where we need to run from
PRGDIR=`dirname "$PRG"`

if [ ! -e $PRGDIR/$JARFILE ] 
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
if echo "$CMD_LINE_ARGS" | grep -q "$SOURCE"; then
  echo "home parameter not found setting default";
else
  DIRECTORY=$(cd `dirname $0` && pwd)
  cd $DIRECTORY/../..
  DIRECTORY2=$(cd `dirname $0` && pwd)
  cd $DIRECTORY
  CMD_LINE_ARGS="$CMD_LINE_ARGS -home $DIRECTORY2"
fi

SOURCE="-url"
if echo "$CMD_LINE_ARGS" | grep -q "$SOURCE"; then
  echo "url parameter not found setting default";
else
  CMD_LINE_ARGS="$CMD_LINE_ARGS -url http://www.dotcms.com:8080/servlets/upgrade"
fi


echo "Running from $PRGDIR"

#Make sure we're clear to start
if [ -e $PRGDIR/$NEWFILE ]
then
  echo "$NEWFILE exists, it was most likely left by a failed attempt.  Please remove before starting again. Aborting."
  exit 1
fi

$JAVA_HOME/bin/java -jar $PRGDIR/autoUpdater.jar $CMD_LINE_ARGS


if [ -e $PRGDIR/$NEWFILE ]
then
  # Make sure file is not zero length
  if [ -s $PRGDIR/$NEWFILE ]
  then
    echo "$NEWFILE exists, updating agent."
    mv $PRGDIR/$JARFILE $PRGDIR/$OLDFILE
    mv $PRGDIR/$NEWFILE $PRGDIR/$JARFILE
    echo "autoUpdater upgraded, restarting process."
    $JAVA_HOME/bin/java -jar $PRGDIR/autoUpdater.jar $CMD_LINE_ARGS
  fi 
fi
