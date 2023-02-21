#!/bin/bash
docker compose exec cli hz-cli submit \
    -v \
    -c=hazelcast.platform.labs.machineshop.solutions.TemperatureMonitorPipelineSolution \
    -t=dev@hz   \
    -n=temp_monitor_`date +%H%M%S` \
    /project/monitoring-pipeline/target/monitoring-pipeline-1.0-SNAPSHOT.jar
