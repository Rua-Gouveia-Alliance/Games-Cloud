#!/bin/bash

systemctl stop rc-local.service
source config.sh
java -cp ../loadbalancer/target/loadbalancer-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.loadbalancer.LoadBalancer
