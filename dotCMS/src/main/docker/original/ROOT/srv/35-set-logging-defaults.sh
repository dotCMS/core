#!/bin/bash

set -e

## Defaults for the log4j2 env vars consumed by webapps/ROOT/WEB-INF/log4j/log4j2.xml
## (installed by 20-copy-overriden-files.sh). Override any of these on the container
## to change log output per environment:
##
##   CMS_LOG4J_STACK_FILTER    - comma-separated packages collapsed in stack traces
##                               ("... suppressed NN lines")
##   CMS_LOG4J_CONSOLE_PATTERN - console appender pattern layout
##   CMS_LOG4J_MESSAGE_PATTERN - dotcms.log file appender pattern layout
##
## Pattern values must be self-contained: log4j2 does not resolve ${...} lookups
## inside values that come from env vars (log4shell hardening), so the default
## patterns are composed here with the filter list embedded.

DEFAULT_STACK_FILTER='org.apache.catalina,org.apache.coyote,org.apache.tomcat,javax.servlet,org.tuckey,io.vavr,graphql.execution,graphql.kickstart,graphql.GraphQL,java.util.concurrent,java.lang.Thread,sun.nio.ch,jdk.internal.reflect,java.lang.reflect'
export CMS_LOG4J_STACK_FILTER="${CMS_LOG4J_STACK_FILTER:-$DEFAULT_STACK_FILTER}"

## Control characters other than tab and line ends (a carriage return counts as a line end only
## before a line feed) are written as "?" in messages and exception text; %ex{none} keeps log4j
## from appending a second, unfiltered trace.
CONTROL_CHARS='[\x00-\x08\x0B\x0C\x0E-\x1F\x7F-\x9F\u2028\u2029]|\r(?!\n)'
DEFAULT_CONSOLE_PATTERN='%d{HH:mm:ss.SSS}  %-5level %logger{2} - %replace{%enc{%msg}{CRLF}}{'"$CONTROL_CHARS"'}{?}%n%replace{%xEx{filters('"$CMS_LOG4J_STACK_FILTER"')}}{'"$CONTROL_CHARS"'}{?}%ex{none}'
DEFAULT_MESSAGE_PATTERN='[%d{dd/MM/yy HH:mm:ss:SSS z}] %5p %c{2}: %replace{%enc{%m}{CRLF}}{'"$CONTROL_CHARS"'}{?}%n%replace{%xEx{filters('"$CMS_LOG4J_STACK_FILTER"')}}{'"$CONTROL_CHARS"'}{?}%ex{none}'

export CMS_LOG4J_CONSOLE_PATTERN="${CMS_LOG4J_CONSOLE_PATTERN:-$DEFAULT_CONSOLE_PATTERN}"
export CMS_LOG4J_MESSAGE_PATTERN="${CMS_LOG4J_MESSAGE_PATTERN:-$DEFAULT_MESSAGE_PATTERN}"
