#!/bin/sh
set -e
python3 /project/performance-test/retrieve_sns.py
/home/hazelcast/.local/bin/locust -f /project/performance-test/perftest.py