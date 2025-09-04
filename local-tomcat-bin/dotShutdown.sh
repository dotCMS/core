#!/bin/sh

# -----------------------------------------------------------------------------
# Start Script for the dotCMS Server
# -----------------------------------------------------------------------------

# Better OS/400 detection: see Bugzilla 31132
os400=false
darwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
OS400*) os400=true;;
Darwin*) darwin=true;;
esac

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

PRGDIR=`dirname "$PRG"`
EXECUTABLE=catalina.sh

# Read an optional running configuration file
if [ "x$RUN_CONF" = "x" ]; then
    RUN_CONF="$PRGDIR/build.conf"
fi
if [ -r "$RUN_CONF" ]; then
    . "$RUN_CONF" 2>/dev/null
fi

DISTRIBUTION_HOME=`cd "$PRGDIR/.." ; pwd`
TOMCAT_HOME=`cd "$PRGDIR/.." ; pwd`
DOTCMS_HOME=`cd "$PRGDIR/../$HOME_FOLDER" ; pwd`

# Check that target executable exists
if $os400; then
  # -x will Only work on the os400 if the files are:
  # 1. owned by the user
  # 2. owned by the PRIMARY group of the user
  # this will not work if the user belongs in secondary groups
  eval
else
  if [ ! -x "$TOMCAT_HOME"/bin/"$EXECUTABLE" ]; then
    echo "Cannot find $TOMCAT_HOME/bin/$EXECUTABLE"
    echo "This file is needed to run this program"
    exit 1
  fi
fi

# Sets DOTSERVER if not specified and changes existing JAVA_OPTS to use it
if [ -z "$DOTSERVER" ]; then
        DOTSERVER=`echo "$DISTRIBUTION_HOME" | sed -e 's/\(.*\)\///'`
fi
export JAVA_OPTS="$JAVA_OPTS -Ddotserver=$DOTSERVER"

# Sets PID to DOTSERVER if not already specified
if [ -z "$CATALINA_PID" ]; then
        export CATALINA_PID="/tmp/$DOTSERVER.pid"
        if [ "$1" = "start" ] ; then
                if [ -e "$CATALINA_PID" ]; then
                        echo
                        echo "Pid file $CATALINA_PID exists! Are you
sure dotCMS is not running?"
                        echo
                        echo "If dotCMS is not running, please remove
the Pid file or change the"
                        echo "setting in bin/catalina.sh before starting
your dotCMS application."
                        echo
                        exit 1
                fi
        fi
fi

#Sets default JAVA_OPTS
export JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true -Xverify:none -Dfile.encoding=UTF8"
MEMSET=`echo $JAVA_OPTS | grep '\-server'`
if [ -z "$MEMSET" ]; then
    export JAVA_OPTS="$JAVA_OPTS -server"
fi

MEMSET=`echo $JAVA_OPTS | grep '\-Xmx'`
if [ -z "$MEMSET" ]; then
    export JAVA_OPTS="$JAVA_OPTS -Xmx1G"
fi
MEMSET=`echo $JAVA_OPTS | grep '\-XX:PermSize'`
if [ -z "$MEMSET" ]; then
    export JAVA_OPTS="$JAVA_OPTS -XX:PermSize=196m"
fi

echo "Using DOTCMS_HOME = $DOTCMS_HOME"
echo "Using DOTSERVER = $DOTSERVER"
echo "Using CATALINA_PID = $CATALINA_PID"
echo "Using JAVA_OPTS = $JAVA_OPTS"

if [ -z $1 ]; then
    CMD="stop 8"
else
    CMD="stop $1"
fi

if [ "$1" = "-usage" -o "$1" = "usage" ]; then
  echo "Usage: shutdown.sh ( commands ... )"
  echo "commands:"
  echo "  usage        Prints out this info"
  echo "  No arguments Stop dotCMS, wait up to 8 seconds and then force (-KILL) to stop"
  echo "  n            Stop dotCMS, waiting up to n seconds for the process to end if not then it forces it (-KILL) to stop"
  exit 1
else
    exec "$TOMCAT_HOME"/bin/"$EXECUTABLE" $CMD -force
fi
