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

### Deploying on AWS

First, ensure the parameters in `aws-scripts/config.sh` are correct, and that your AWS access key is on `aws-scripts/rootkey.csv`.

Also ensure that the ssh keypair is present in `aws-scripts/gac-aws-key.pem`

From the `aws-scripts` directory:

1. Create the AMI with the local version by running `./create-img.sh`
2. Deploy the LoadBalancer, AutoScalar, and configure alarms with `./launch-deployment.sh`
3. To stop the server and terminate the AWS resources run `./terminate-deployment.sh`

### Deploying on AWS

1. Ensure the parameters in `aws-scripts/config.sh` are correct, and that your AWS access keys are stored in `aws-scripts/rootkey.csv`.
2. Make sure the SSH keypair file `gac-aws-key.pem` is present in the `aws-scripts` directory.

From the `aws-scripts` directory:

3. Create the AMI from the local version by running `./create-img.sh`
4. Deploy the Load Balancer, Auto Scaling Group, and configure CloudWatch alarms by running `./launch-deployment.sh`
5. To stop the server and terminate all AWS resources, run `./terminate-deployment.sh`
