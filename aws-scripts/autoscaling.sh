#!/bin/bash

source config.sh

# Create scaling policies
SCALE_OUT_POLICY_ARN=$(aws autoscaling put-scaling-policy \
	--auto-scaling-group-name GaC-ASG \
	--policy-name GaC-ScaleOut \
	--policy-type StepScaling \
	--adjustment-type ChangeInCapacity \
	--step-adjustments MetricIntervalLowerBound=0.0,ScalingAdjustment=1 \
	--cooldown 60 \
	--query "PolicyARN" \
	--output text)

SCALE_IN_POLICY_ARN=$(aws autoscaling put-scaling-policy \
	--auto-scaling-group-name GaC-ASG \
	--policy-name GaC-ScaleIn \
	--policy-type StepScaling \
	--adjustment-type ChangeInCapacity \
	--step-adjustments MetricIntervalUpperBound=0.0,ScalingAdjustment=-1 \
	--cooldown 60 \
	--query "PolicyARN" \
	--output text)

# Create CloudWatch alarms
aws cloudwatch put-metric-alarm \
	--alarm-name GaC-CPU-High \
	--metric-name CPUUtilization \
	--namespace AWS/EC2 \
	--statistic Average \
	--period 60 \
	--threshold 50 \
	--comparison-operator GreaterThanThreshold \
	--dimensions Name=AutoScalingGroupName,Value=GaC-ASG \
	--evaluation-periods 1 \
	--alarm-actions "$SCALE_OUT_POLICY_ARN" \
	--unit Percent

aws cloudwatch put-metric-alarm \
	--alarm-name GaC-CPU-Low \
	--metric-name CPUUtilization \
	--namespace AWS/EC2 \
	--statistic Average \
	--period 60 \
	--threshold 20 \
	--comparison-operator LessThanThreshold \
	--dimensions Name=AutoScalingGroupName,Value=GaC-ASG \
	--evaluation-periods 1 \
	--alarm-actions "$SCALE_IN_POLICY_ARN" \
	--unit Percent
