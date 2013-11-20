#!/bin/bash
for OUTPUT in $(ls)
do
	shasum $OUTPUT
done
