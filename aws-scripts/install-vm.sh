#!/bin/bash

source config.sh

SERVER_PATH="$DIR/../../cnv25-g05"

# Install java.
cmd="sudo dnf update -y; sudo dnf install java-21-amazon-corretto-devel.x86_64 -y; sudo dnf install maven-amazon-corretto21 -y"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

# Install web server.
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH -r $SERVER_PATH ec2-user@$(cat instance.dns):~

# Build web server.
cmd="cd ~/cnv25-05; mvn clean package"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd 

# Setup web server to start on instance launch.
cmd='
sudo tee /etc/rc.local > /dev/null <<EOF
#!/bin/bash
cd ~/cnv25-05
java -cp webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
-javaagent:javassist/target/javassist-1.0.0-SNAPSHOT-jar-with-dependencies.jar=ICount:pt.ulisboa.tecnico.cnv.capturetheflag,pt.ulisboa.tecnico.cnv.fifteenpuzzle,pt.ulisboa.tecnico.cnv.gameoflife:output \
pt.ulisboa.tecnico.cnv.webserver.WebServer &
EOF
sudo chmod +x /etc/rc.local
'
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

# Create deamon and enable it
cmd='
sudo tee /etc/systemd/system/rc-local.service > /dev/null <<EOF
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
EOF
sudo systemctl enable rc-local.service
'
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

