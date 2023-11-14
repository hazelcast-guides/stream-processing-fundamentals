#!/bin/bash
set -e
clc -c Docker job cancel \
    `cat ../job/jobname.txt`