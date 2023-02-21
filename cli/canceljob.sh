#!/bin/bash
set -e
hz-cli cancel \
    -t=dev@hz   \
    `cat /project/cli/jobname.txt`
