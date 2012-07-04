#!/bin/bash
OUTP=$1
## rm ${OUTP}missingkeys.txt
## touch ${OUTP}usedkeys.txt
echo ---$2--- >> ${OUTP}usedkeys.txt
find $1html -name '*.jsp' | xargs grep -l $2 >> ${OUTP}usedkeys.txt