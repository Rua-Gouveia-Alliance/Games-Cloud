#!/bin/bash

source config.sh

echo "Terminating all EC2 instances..."
INSTANCE_IDS=$(aws ec2 describe-instances \
    --query "Reservations[].Instances[].InstanceId" \
    --output text)

if [[ -z "$INSTANCE_IDS" ]]; then
    echo "No EC2 instances found."
else
    aws ec2 terminate-instances --instance-ids $INSTANCE_IDS
    echo "Waiting for instance termination (60s)..."
    sleep 60
fi

if [ -f image-webserver.id ]; then
    IMAGE_ID=$(cat image-webserver.id)
    echo "Deregistering AMI: $IMAGE_ID"

    SNAPSHOT_IDS=$(aws ec2 describe-images \
        --image-ids "$IMAGE_ID" \
        --query "Images[].BlockDeviceMappings[].Ebs.SnapshotId" \
        --output text)

    aws ec2 deregister-image --image-id "$IMAGE_ID"

    echo "Deleting associated snapshots..."
    for SNAPSHOT_ID in $SNAPSHOT_IDS; do
        aws ec2 delete-snapshot --snapshot-id "$SNAPSHOT_ID"
    done

    rm -f image-webserver.id
else
    echo "image-webserver.id not found — skipping AMI cleanup."
fi

echo "Deleting Lambda functions..."
aws lambda delete-function --function-name capturetheflag || echo "capturetheflag not found"
aws lambda delete-function --function-name fifteenpuzzle || echo "fifteenpuzzle not found"
aws lambda delete-function --function-name gameoflife || echo "gameoflife not found"

echo "Deleting DynamoDB table 'metrics'..."
aws dynamodb delete-table --table-name metrics || echo "metrics table not found"

echo "Termination complete. All AWS resources cleaned up."
