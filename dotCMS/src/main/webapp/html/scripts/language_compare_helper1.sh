#!/bin/bash
OUTP=$1
## rm ${OUTP}missingkeys.txt
## touch ${OUTP}usedkeys.txt
echo ---$2--- >> ${OUTP}keys.txt
find $1html $1../src -name '*.jsp' -o -name '*.java' | xargs grep -l $2 >> ${OUTP}keys.txt
