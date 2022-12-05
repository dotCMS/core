#!/bin/bash

oc delete configmap dotcmslicensepack

oc delete -f prereq.yaml
#rm ./prereq.yaml

