#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

export AWS_DEFAULT_REGION="eu-west-3"
export AWS_ACCOUNT_ID="340393029847"
export AWS_ACCESS_KEY_ID=$(tail -n 1 "$DIR/rootkey.csv" | cut -d',' -f1 | tr -d '\r\n')
export AWS_SECRET_ACCESS_KEY=$(tail -n 1 "$DIR/rootkey.csv" | cut -d',' -f2 | tr -d '\r\n')
export AWS_EC2_SSH_KEYPAR_PATH="$DIR/gac-aws-key.pem"
export AWS_SECURITY_GROUP="sg-0dea709f8360caf8b"
export AWS_KEYPAIR_NAME="gac-aws-key"

#aws sts get-caller-identity
