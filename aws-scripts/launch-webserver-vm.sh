#!/bin/bash

source config.sh

# Run new instance.
aws ec2 run-instances \
	--image-id resolve:ssm:/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 \
	--instance-type t3.medium \
	--key-name $AWS_KEYPAIR_NAME \
	--security-group-ids $AWS_SECURITY_GROUP \
	--monitoring Enabled=true | jq -r ".Instances[0].InstanceId" > instance-webserver.id
echo "New instance with id $(cat instance-webserver.id)."

# Wait for instance to be running.
aws ec2 wait instance-running --instance-ids $(cat instance-webserver.id)
echo "New instance with id $(cat instance-webserver.id) is now running."

# Extract DNS name.
aws ec2 describe-instances \
	--instance-ids $(cat instance-webserver.id) | jq -r ".Reservations[0].Instances[0].NetworkInterfaces[0].PrivateIpAddresses[0].Association.PublicDnsName" > instance-webserver.dns
echo "New instance with id $(cat instance-webserver.id) has address $(cat instance-webserver.dns)."

# Wait for instance to have SSH ready.
while ! nc -z $(cat instance-webserver.dns) 22; do
	echo "Waiting for $(cat instance-webserver.dns):22 (SSH)..."
	sleep 0.5
done
echo "New instance with id $(cat instance-webserver.id) is ready for SSH access."
