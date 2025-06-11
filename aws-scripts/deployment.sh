#!/bin/bash

source config.sh

source create-webserver-image.sh

# Ask the user to update the AMI ID manually before continuing
echo ""
echo "Please copy the new AMI ID above and update it in the Java source code (EC2InstanceManager.java)."
echo "   Replace the value of AMI_ID with the newly created image ID."
echo ""
read -p "Once you've done that and recompiled, press [Enter] to continue..."

source create-dynamo-table.sh
source deploy-lambdas.sh
source deploy-loadbalancer.sh
