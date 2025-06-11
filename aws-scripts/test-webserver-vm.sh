#!/bin/bash

source config.sh

# Requesting an instance reboot.
aws ec2 reboot-instances --instance-ids $(cat instance-webserver.id)
echo "Rebooting instance to test web server auto-start."

# Letting the instance shutdown.
sleep 1

# Wait for port 8000 to become available.
while ! nc -z $(cat instance-webserver.dns) 8000; do
	echo "Waiting for $(cat instance-webserver.dns):8000..."
	sleep 0.5
done

# Sending a query!
echo "Sending a query!"
curl $(cat instance-webserver.dns):8000/index.html
