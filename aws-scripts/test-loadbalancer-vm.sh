#!/bin/bash

source config.sh

# Requesting an instance reboot.
aws ec2 reboot-instances --instance-ids $(cat instance-loadbalancer.id)
echo "Rebooting instance to test web server auto-start."

# Letting the instance shutdown.
sleep 1

# Wait for port 8080 to become available.
while ! nc -z $(cat instance-loadbalancer.dns) 8080; do
	echo "Waiting for $(cat instance-loadbalancer.dns):8000..."
	sleep 0.5
done

# Sending a query!
echo "Sending a query!"
curl $(cat instance-loadbalancer.dns):8080/index.html
