#!/bin/bash

source config.sh

# Create load balancer and configure health check.
aws elb create-load-balancer \
	--load-balancer-name GaC-LB \
	--listeners "Protocol=HTTP,LoadBalancerPort=80,InstanceProtocol=HTTP,InstancePort=8000" \
	--availability-zones eu-west-3a

aws elb configure-health-check \
	--load-balancer-name GaC-LB \
	--health-check Target=HTTP:8000/test,Interval=30,UnhealthyThreshold=2,HealthyThreshold=10,Timeout=5

# Create Launch Template
aws ec2 create-launch-template \
	--launch-template-name GaC-launchtemp \
	--version-description "v1" \
	--launch-template-data "{
		\"ImageId\": \"$(cat image.id)\",
		\"InstanceType\": \"t2.micro\",
		\"KeyName\": \"$AWS_KEYPAIR_NAME\",
		\"SecurityGroupIds\": [\"$AWS_SECURITY_GROUP\"],
		\"Monitoring\": {\"Enabled\": true}
    }"


# Create auto scaling group.
aws autoscaling create-auto-scaling-group \
	--auto-scaling-group-name GaC-ASG \
    --launch-template LaunchTemplateName=GaC-launchtemp,Version=1 \
	--load-balancer-names GaC-LB \
	--availability-zones eu-west-3a \
	--health-check-type ELB \
	--health-check-grace-period 60 \
	--min-size 1 \
	--max-size 3 \
	--desired-capacity 1
