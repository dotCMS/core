#!/bin/sh


cygwin=false
os400=false
darwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
OS400*) os400=true;;
Darwin*) darwin=true;;
esac

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

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set DOTCMS_HOME if not already set
[ -z "$DOTCMS_HOME" ] && DOTCMS_HOME=`cd "$PRGDIR/.." ; pwd`

BASEDIR="$DOTCMS_HOME"

if [ -r "$DOTCMS_HOME"/bin/setenv.sh ]; then
echo setting env
  . "$DOTCMS_HOME"/bin/setenv.sh
fi

if [ -r "$DOTCMS_HOME"/bin/setclasspath.sh ]; then
echo setting class
  . "$DOTCMS_HOME"/bin/setclasspath.sh
fi


# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$JRE_HOME" ] && JRE_HOME=`cygpath --unix "$JRE_HOME"`
  [ -n "$DOTCMS_HOME" ] && DOTCMS_HOME=`cygpath --unix "$DOTCMS_HOME"`
  [ -n "$CLASSPATH" ] && CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
  [ -n "$JSSE_HOME" ] && JSSE_HOME=`cygpath --absolute --unix "$JSSE_HOME"`
fi

# For OS400
if $os400; then
  # Set job priority to standard for interactive (interactive - 6) by using
  # the interactive priority - 6, the helper threads that respond to requests
  # will be running at the same priority as interactive jobs.
  COMMAND='chgjob job('$JOBNAME') runpty(6)'
  system $COMMAND

  # Enable multi threading
  export QIBM_MULTI_THREADED=Y
fi

if [ -z "$DOTCMS_HOME" ] ; then
  DOTCMS_HOME="$DOTCMS_HOME"
fi


# Bugzilla 37848: When no TTY is available, don't output to console
have_tty=0
if [ "`tty`" != "not a tty" ]; then
    have_tty=1
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  JAVA_HOME=`cygpath --absolute --windows "$JAVA_HOME"`
  JRE_HOME=`cygpath --absolute --windows "$JRE_HOME"`
  DOTCMS_HOME=`cygpath --absolute --windows "$DOTCMS_HOME"`
  DOTCMS_TMPDIR=`cygpath --absolute --windows "$DOTCMS_TMPDIR"`
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
  [ -n "$JSSE_HOME" ] && JSSE_HOME=`cygpath --absolute --windows "$JSSE_HOME"`
  JAVA_ENDORSED_DIRS=`cygpath --path --windows "$JAVA_ENDORSED_DIRS"`
fi

# ----- Execute The Requested Command -----------------------------------------
if [ $have_tty -eq 1 ]; then
  echo "Using DOTCMS_HOME:   $DOTCMS_HOME"
  echo "Using JAVA_HOME:       $JAVA_HOME"
fi

CURRENT_PWD=`pwd`
cd $DOTCMS_HOME

echo Un-deploying plugins

$JAVA_HOME/bin/java -jar "$DOTCMS_HOME/bin/ant/ant-launcher.jar" undeploy-plugins


