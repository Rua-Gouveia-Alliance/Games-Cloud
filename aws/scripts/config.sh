#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

export AWS_PATH="$DIR/../aws-cli":$PATH
export AWS_BIN_PATH="$DIR/../aws-cli-bin":$PATH
export AWS_DEFAULT_REGION="eu-west-3"

#export AWS_ACCOUNT_ID=<insert here your aws account id>
#export AWS_EC2_SSH_KEYPAR_PATH=<path to aws ssh keypair>
#export AWS_SECURITY_GROUP=<name of your security group>
#export AWS_KEYPAIR_NAME=<name of your aws keypair>
