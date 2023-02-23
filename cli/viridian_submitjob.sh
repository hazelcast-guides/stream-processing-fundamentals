#!/bin/bash
set -e
JOBNAME=temp_monitor_`date +%H%M%S`
hz-cli -f=/project/cli/viridian.client.yaml   \
    submit \
    -v \
    -c=hazelcast.platform.labs.machineshop.TemperatureMonitorPipeline \
    -n=$JOBNAME \
    /project/monitoring-pipeline/target/monitoring-pipeline-1.0-SNAPSHOT.jar
echo $JOBNAME > /project/cli/viridian.jobname.txt
