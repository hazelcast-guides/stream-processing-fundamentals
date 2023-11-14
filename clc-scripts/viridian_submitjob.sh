#!/bin/bash
set -e
JOBNAME=temp_monitor_`date +%H%M%S`
hz-cli -f=/project/cli/viridian.client.yaml   \
    submit \
    -v \
    -c=hazelcast.platform.labs.machineshop.TemperatureMonitorPipeline \
    -n=$JOBNAME \
    /project/monitoring-pipeline/target/monitoring-pipeline-1.0-SNAPSHOT.jar
#
# To submit the solution, comment out the command above and use the command below instead
#
#hz-cli -f=/project/cli/viridian.client.yaml   \
#    submit \
#    -v \
#    -c=hazelcast.platform.labs.machineshop.solutions.TemperatureMonitorPipelineSolution \
#    -n=$JOBNAME \
#    /project/monitoring-pipeline/target/monitoring-pipeline-1.0-SNAPSHOT.jar
echo $JOBNAME > /project/job/viridian.jobname.txt
