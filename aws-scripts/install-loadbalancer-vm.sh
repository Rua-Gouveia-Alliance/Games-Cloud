#!/bin/bash

source config.sh

SERVER_PATH="$DIR/../../cnv25-g05"

# Install java.
cmd="sudo dnf update -y; sudo dnf install java-21-amazon-corretto-devel.x86_64 -y; sudo dnf install maven-amazon-corretto21 -y"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance-loadbalancer.dns) $cmd

# Install web server.
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH -r $SERVER_PATH ec2-user@$(cat instance-loadbalancer.dns):~

# Build web server.
cmd="cd ~/cnv25-g05; mvn clean package"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance-loadbalancer.dns) $cmd

# Start web server at boot using rc.local
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance-loadbalancer.dns) 'bash -s' <<'EOF'
sudo tee /etc/rc.local > /dev/null <<EOL
#!/bin/bash
cd /home/ec2-user/cnv25-g05
source aws-scripts/config.sh
java -cp loadbalancer/target/loadbalancer-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.loadbalancer.LoadBalancer
EOL
sudo chmod +x /etc/rc.local
EOF

# Create and enable rc-local.service
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance-loadbalancer.dns) 'bash -s' <<'EOF'
sudo tee /etc/systemd/system/rc-local.service > /dev/null <<EOL
[Unit]
Description=/etc/rc.local Compatibility
ConditionPathExists=/etc/rc.local

[Service]
Type=forking
ExecStart=/etc/rc.local start
TimeoutSec=0
StandardOutput=tty
RemainAfterExit=yes
SysVStartPriority=99

[Install]
WantedBy=multi-user.target
EOL
sudo systemctl enable rc-local.service
EOF
