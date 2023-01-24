#!/bin/bash
docker compose exec cli hz-cli submit \
    -c=hazelcast.platform.solutions.machineshop.TemperatureMonitorPipeline \
    -t=dev@hz   \
    -n=temperature-monitor \
    /project/monitoring-pipeline/target/monitoring-pipeline-1.0-SNAPSHOT.jar
