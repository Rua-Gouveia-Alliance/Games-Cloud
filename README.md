## Games@Cloud

This project contains three sub-projects:

1. `capturetheflag` - the Capture the Flag workload
2. `fifteenpuzzle` - the 15-Puzzle Solver workload
3. `gameoflife` - the Conway's Game of Life workload
4. `webserver` - the web server exposing the functionality of the workloads

Refer to the `README.md` files of the sub-projects to get more details about each specific sub-project.

### How to build everything

1. Make sure your `JAVA_HOME` environment variable is set to Java 11+ distribution
2. Run `mvn clean package`

### Running the instrumented webserver

From the project's root:
```
java -cp webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar -javaagent:javassist/target/javassist-1.0.0-SNAPSHOT-jar-with-dependencies.jar=ICount:pt.ulisboa.tecnico.cnv.capturetheflag,pt.ulisboa.tecnico.cnv.fifteenpuzzle,pt.ulisboa.tecnico.cnv.gameoflife:output pt.ulisboa.tecnico.cnv.webserver.WebServer
```

### System Architecture and AWS Configuration

The architecture follows a **Load Balanced Auto-Scaling Group** model to host and scale the web server that runs the workloads. Here's an overview:

#### Components

* **Amazon EC2 Instances**
  Used to run the workloads (`capturetheflag`, `fifteenpuzzle`, `gameoflife`) exposed by the instrumented `webserver`.

* **Amazon Machine Image (AMI)**
  Created from a manually configured EC2 instance, containing the web server and startup scripts.

* **Elastic Load Balancer (ELB)**
  Distributes incoming HTTP requests across multiple EC2 instances running in a single Availability Zone.

* **Auto Scaling Group (ASG)**
  Ensures availability and elasticity of the application by scaling the number of EC2 instances based on CPU usage.

* **CloudWatch Alarms**
  Monitor EC2 CPU utilization and trigger scaling policies accordingly.


#### Load Balancer Configuration

* **Type**: Classic Load Balancer (ELB)
* **Name**: `GaC-LB`
* **Listener**:

  * Protocol: HTTP
  * LoadBalancerPort: 80
  * InstanceProtocol: HTTP
  * InstancePort: 8000
* **Availability Zone**: `eu-west-3a`
* **Health Check**:

  * Target: `HTTP:8000/test`
  * Interval: 30 seconds
  * Timeout: 5 seconds
  * Healthy Threshold: 10
  * Unhealthy Threshold: 2


#### Auto Scaling Group Configuration

* **Name**: `GaC-ASG`
* **Launch Template**: `GaC-launchtemp`
* **Instance Type**: `t2.micro`
* **AMI ID**: dynamically generated and stored in `image.id`
* **Key Name**: defined in `config.sh`
* **Security Group**: defined in `config.sh`
* **Monitoring**: Enabled
* **Min Size**: 1
* **Max Size**: 3
* **Desired Capacity**: 1
* **Availability Zone**: `eu-west-3a`
* **Health Check Type**: ELB
* **Health Check Grace Period**: 60 seconds


#### Auto Scaling Policies

* **Scale Out (Add Instance)**:

  * Policy Type: Step Scaling
  * Trigger: CPU Utilization > 50%
  * Adjustment: +1 instance
  * Cooldown: 60 seconds

* **Scale In (Remove Instance)**:

  * Policy Type: Step Scaling
  * Trigger: CPU Utilization < 20%
  * Adjustment: -1 instance
  * Cooldown: 60 seconds

### Deploying on AWS

1. Ensure the parameters in `aws-scripts/config.sh` are correct, and that your AWS access keys are stored in `aws-scripts/rootkey.csv`.
2. Make sure the SSH keypair file `gac-aws-key.pem` is present in the `aws-scripts` directory.

From the `aws-scripts` directory:

3. Create the AMI from the local version by running `./create-img.sh`
4. Deploy the Load Balancer, Auto Scaling Group, and configure CloudWatch alarms by running `./launch-deployment.sh`
5. To stop the server and terminate all AWS resources, run `./terminate-deployment.sh`

