#!/bin/bash

source config.sh

# Step 1: update and delete Auto Scaling group.
aws autoscaling update-auto-scaling-group \
	--auto-scaling-group-name GaC-ASG \
  	--min-size 0 \
  	--max-size 0 \
  	--desired-capacity 0

# Optional: wait for instance termination before deletion
echo "Waiting 60 seconds for instance termination..."
sleep 60

aws autoscaling delete-auto-scaling-group \
	--auto-scaling-group-name GaC-ASG \
	--force-delete

# Step 2: delete Launch Template.
aws ec2 delete-launch-template \
  	--launch-template-name GaC-launchtemp

# Step 3: delete load balancer.
aws elb delete-load-balancer \
	--load-balancer-name GaC-LB

# Step 4: delete cloudwatch alarms
aws cloudwatch delete-alarms \
	--alarm-names GaC-CPU-High GaC-CPU-Low

# Step 5: delete image and snapshots
IMAGE_ID=$(cat image.id)

# Get and delete associated snapshots
SNAPSHOT_IDS=$(aws ec2 describe-images \
    --image-ids "$IMAGE_ID" \
    --query "Images[].BlockDeviceMappings[].Ebs.SnapshotId" \
    --output text)

# Deregister the AMI
aws ec2 deregister-image --image-id "$IMAGE_ID"
sleep 1

for SNAPSHOT_ID in $SNAPSHOT_IDS; do
    aws ec2 delete-snapshot --snapshot-id "$SNAPSHOT_ID"
done

# Step 6: delete files
rm image.id instance.dns instance.id

echo "Done."
