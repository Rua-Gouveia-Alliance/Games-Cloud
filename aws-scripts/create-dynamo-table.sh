#!/bin/bash

source config.sh

# Create dynamoDB table with keys: GameName and MethodName
TABLE_NAME="MetricStorageSystem"

# Create the DynamoDB table in Pay-Per-Request (on-demand) mode
aws dynamodb create-table \
    --table-name $TABLE_NAME \
    --attribute-definitions \
        AttributeName=GameName,AttributeType=S \
        AttributeName=Timestamp,AttributeType=N \
    --key-schema \
        AttributeName=GameName,KeyType=HASH \
        AttributeName=Timestamp,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --region $AWS_DEFAULT_REGION \
    --output json

echo "Waiting for table $TABLE_NAME to be created..."
aws dynamodb wait table-exists \
    --table-name $TABLE_NAME \
    --region $AWS_DEFAULT_REGION

echo "Table $TABLE_NAME created successfully in $AWS_DEFAULT_REGION using pay-per-request mode."

