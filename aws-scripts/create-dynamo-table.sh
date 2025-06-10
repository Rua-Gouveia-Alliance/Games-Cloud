#!/bin/bash

source config.sh

# Create dynamoDB table
aws dynamodb create-table \
  --table-name MetricStorageSystem \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --tags Key=Environment,Value=Dev Key=Project,Value=GamesCloud

