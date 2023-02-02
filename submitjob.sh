#!/bin/bash
docker compose exec cli hz-cli submit \
    -c=hazelcast.platform.solutions.machineshop.TemperatureMonitorPipeline \
    -t=dev@hz   \
    -n=temp_monitor_`date +%H%M%S` \
    /project/monitoring-pipeline/target/monitoring-pipeline-1.0-SNAPSHOT.jar
