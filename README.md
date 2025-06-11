## Games\@Cloud

This project contains four sub-projects:

1. `capturetheflag` - the Capture the Flag workload
2. `fifteenpuzzle` - the 15-Puzzle Solver workload
3. `gameoflife` - the Conway's Game of Life workload
4. `webserver` - the web server exposing the functionality of the workloads

Refer to the `README.md` files of the sub-projects to get more details about each specific sub-project.

### How to build everything

1. Make sure your `JAVA_HOME` environment variable is set to a Java 11+ distribution
2. Run `mvn clean package` from the root directory

### Running the instrumented webserver (for local testing only)

```
java -cp webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar -javaagent:javassist/target/javassist-1.0.0-SNAPSHOT-jar-with-dependencies.jar=ICount:pt.ulisboa.tecnico.cnv.capturetheflag,pt.ulisboa.tecnico.cnv.fifteenpuzzle,pt.ulisboa.tecnico.cnv.gameoflife:output pt.ulisboa.tecnico.cnv.webserver.WebServer
```

### System Architecture and AWS Configuration

The architecture now uses a **custom Load Balancer with Auto Scaling and Lambda support** to handle and scale requests dynamically. Here's an updated overview:

#### Components

* **Amazon EC2 Instances**

  * Run the instrumented `webserver` hosting the game workloads.

* **Amazon Lambda Functions**

  * Each game is deployed as a separate Lambda function for cost-effective, burstable request handling.
  * **IAM Role Required**: Ensure your Lambda functions use the following execution role:

    ```
    arn:aws:iam::$AWS_ACCOUNT_ID:role/gac-lambda-role
    ```

* **Amazon DynamoDB**

  * Stores instrumentation metrics extracted by the webservers.

* **Amazon CloudWatch**

  * Provides CPU metrics for EC2-based autoscaling decisions.

* **Custom Java Load Balancer**

  * Routes requests to EC2 instances or Lambda based on real-time metrics and worker availability.
  * Uses a shared-memory model to interact with the Autoscaler.

* **Java-based AutoScaler**

  * Monitors total system load (using metrics and queues) to dynamically scale EC2 workers.
  * Avoids waste and bootstrapping penalties.

#### Load Balancer Details

* **Port**: 8080 (external), 8000 (internal worker HTTP server)
* **Dispatch Strategy**:

  * Forwards requests to the least-loaded EC2 instance if available.
  * Falls back to Lambda execution if all instances are overloaded or booting.
  * Detects failed VMs and reassigns queued requests.

#### AutoScaler Policies

* Monitors a custom `lambda` metric derived from current queue lengths, in-flight workloads, and CloudWatch CPU utilization.
* **Scale Out**: Launches a new instance when estimated lambda exceeds a dynamic threshold.
* **Scale In**: Terminates underutilized instances after a cool-down period.

### Deploying on AWS

1. Ensure AWS credentials are configured in `aws-scripts/config.sh`, including:

   * `AWS_ACCESS_KEY_ID`
   * `AWS_SECRET_ACCESS_KEY`
   * `AWS_ACCOUNT_ID`
   * `AWS_DEFAULT_REGION`
   * `AWS_KEYPAIR_NAME`
   * `AWS_SECURITY_GROUP`
   * `AWS_EC2_SSH_KEYPAR_PATH`

2. Create the instrumented AMI by running:

   ```bash
   ./aws-scripts/create-image.sh
   ```

3. Once the image is created, update the `AMI_ID` value in the code (`EC2InstanceManager.java`), then:

   ```bash
   read -p "Once you've done that and recompiled, press [Enter] to continue..."
   ```

4. Deploy all infrastructure:

   ```bash
   ./aws-scripts/launch-deployment.sh
   ```

5. This script will:

   * Create DynamoDB table
   * Deploy 3 Lambda functions: `capturetheflag`, `fifteenpuzzle`, and `gameoflife`
   * Deploy and start the custom Java-based load balancer

6. To terminate all infrastructure:

   ```bash
   ./aws-scripts/terminate-deployment.sh
   ```
