#!/bin/bash

source config.sh

# Step 1: delete auto scaling group.
aws autoscaling delete-auto-scaling-group \
	--auto-scaling-group-name GaC-ASG \
	--force-delete

# Step 2: delete launch configuration.
aws autoscaling delete-launch-configuration \
	--launch-configuration-name GaC-launchconfig

# Step 3: delete load balancer.
aws elb delete-load-balancer \
	--load-balancer-name GaC-LB
