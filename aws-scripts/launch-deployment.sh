#!/bin/bash

source config.sh

# Create load balancer and configure health check.
aws elb create-load-balancer \
	--load-balancer-name GaC-LB \
	--listeners "Protocol=HTTP,LoadBalancerPort=80,InstanceProtocol=HTTP,InstancePort=8000" \
	--availability-zones us-east-2a

aws elb configure-health-check \
	--load-balancer-name GaC-LB \
	--health-check Target=HTTP:8000/test,Interval=30,UnhealthyThreshold=2,HealthyThreshold=10,Timeout=5

# Create launch configuration.
aws autoscaling create-launch-configuration \
	--launch-configuration-name GaC-launchconfig \
	--image-id $(cat image.id) \
	--instance-type t2.micro \
	--security-groups $AWS_SECURITY_GROUP \
	--key-name $AWS_KEYPAIR_NAME \
	--instance-monitoring Enabled=true

# Create auto scaling group.
aws autoscaling create-auto-scaling-group \
	--auto-scaling-group-name GaC-ASG \
	--launch-configuration-name GaC-launchconfig \
	--load-balancer-names GaC-LB \
	--availability-zones us-east-2a \
	--health-check-type ELB \
	--health-check-grace-period 60 \
	--min-size 1 \
	--max-size 3 \
	--desired-capacity 1
