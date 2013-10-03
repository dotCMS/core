#!/bin/sh

# -----------------------------------------------------------------------------
# Start Script for the dotCMS Server
# -----------------------------------------------------------------------------

# Better OS/400 detection
os400=false
case "`uname`" in
OS400*) os400=true;;
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
DOTCMS_HOME=`cd "$PRGDIR/../webapps/ROOT" ; pwd`

## Script CONFIGURATION Options


# JAVA_OPTS: Below are the recommended minimum settings for the Java VM.
# These may (and should) be customized to suit your needs. Please check with
# Sun Microsystems and the Apache Tomcat websites for the latest information
# http://java.sun.com
# http://tomcat.apache.org

#Uncomment the following line to enable the JMX interface
#Please be aware that this configuration doesn't provide any authentication, so it could pose a security risk.
#More info at http://java.sun.com/j2se/1.5.0/docs/guide/management/agent.html

#JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.port=7788 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false "

##add agentpath to be enable ability to profile application
#JAVA_OPTS="-agentpath:/Applications/YourKit_Java_Profiler_9.0.5.app/bin/mac/libyjpagent.jnilib $JAVA_OPTS -Djava.awt.headless=true -Xverify:none -Dfile.encoding=UTF8 -server -Xms1024M -Xmx1024M -XX:PermSize=128m "

JAVA_OPTS="$JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"

JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true -Xverify:none -Dfile.encoding=UTF8 -server -Xmx1G -XX:MaxPermSize=256m -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -javaagent:$DOTCMS_HOME/WEB-INF/lib/jamm-0.2.5.jar"

if [ "$1" = "debug" ] ; then
   #debug
   JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8765 $JAVA_OPTS"
fi




## END Script CONFIGURATION Options

# Check that target executable exists
if $os400; then
  # -x will Only work on the os400 if the files are:
  # 1. owned by the user
  # 2. owned by the PRIMARY group of the user
  # this will not work if the user belongs in secondary groups
  eval
else
  if [ ! -x "$PRGDIR"/"$EXECUTABLE" ]; then
    echo "Cannot find $PRGDIR/$EXECUTABLE"
    echo "This file is needed to run this program"
    exit 1
  fi
fi

# Sets DOTSERVER if not specified and changes existing JAVA_OPTS to use it
if [ -z "$DOTSERVER" ]; then
        DOTSERVER=`echo "$DOTCMS_HOME" | sed -e 's/\(.*\)\///'`
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

echo "Using DOTCMS_HOME = $DOTCMS_HOME"
echo "Using DOTSERVER = $DOTSERVER"
echo "Using CATALINA_PID = $CATALINA_PID"
echo "Using JAVA_OPTS = $JAVA_OPTS"

if [ -z $1 ]; then
    cmd="start"
else
    cmd="$1"
    shift
fi

if [ $cmd = "-usage" -o $cmd = "usage" ]; then
  echo "Usage: startup.sh ( commands ... )"
  echo "commands:"
  echo "  usage        Prints out this info"
  echo "  No Arguments Start dotCMS"
  echo "  run          Start dotCMS redirecting the output to the console"
  exit 1;
elif [ $cmd = "run" ]; then
	exec "$PRGDIR"/"$EXECUTABLE" run "$@"
else
    exec "$PRGDIR"/"$EXECUTABLE" start "$@"
fi
