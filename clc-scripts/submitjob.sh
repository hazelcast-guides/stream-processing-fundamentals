#!/bin/bash
set -e
JOBNAME=temp_monitor_`date +%H%M%S`
echo $JOBNAME > ../job/jobname.txt
clc -c Docker job submit \
    --verbose \
    --class=hazelcast.platform.labs.machineshop.TemperatureMonitorPipeline \
    --name=$JOBNAME \
    ../monitoring-pipeline/target/monitoring-pipeline-1.0-SNAPSHOT.jar