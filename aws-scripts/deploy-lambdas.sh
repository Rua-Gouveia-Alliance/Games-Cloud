#!/bin/bash

# Load AWS credentials
source config.sh

# Common values
ROLE_ARN="arn:aws:iam::$AWS_ACCOUNT_ID:role/gac-lambda-role"
RUNTIME="java11"
REGION="$AWS_DEFAULT_REGION"
TIMEOUT=900
MEMORY=2048

# Declare your games and handlers
declare -A GAMES=(
  ["capturetheflag"]="pt.ulisboa.tecnico.cnv.capturetheflag.CaptureTheFlagHandler::handleRequest"
  ["fifteenpuzzle"]="pt.ulisboa.tecnico.cnv.fifteenpuzzle.FifteenPuzzleHandler::handleRequest"
  ["gameoflife"]="pt.ulisboa.tecnico.cnv.gameoflife.GameOfLifeHandler::handleRequest"
)

# Deploy each game
for GAME in "${!GAMES[@]}"; do
  echo "Deploying Lambda: $GAME"

  PROJECT_DIR="$DIR/../$GAME"
  HANDLER="${GAMES[$GAME]}"
  FUNCTION_NAME="${GAME}Handler"

  # Build JAR with dependencies
  (cd "$PROJECT_DIR" && mvn clean package -DskipTests)

  # Get the fat JAR path
  JAR_FILE=$(find "$PROJECT_DIR/target" -name "*-jar-with-dependencies.jar" | head -n 1)
  if [ ! -f "$JAR_FILE" ]; then
    echo "Fat JAR not found for $GAME. Skipping."
    continue
  fi

  # Create or update Lambda
  if aws lambda get-function --function-name "$FUNCTION_NAME" --region "$REGION" > /dev/null 2>&1; then
    echo "Updating existing Lambda function $FUNCTION_NAME..."
    aws lambda update-function-code \
      --function-name "$FUNCTION_NAME" \
      --zip-file "fileb://$JAR_FILE" \
      --region "$REGION"
  else
    echo "Creating new Lambda function $FUNCTION_NAME..."
    aws lambda create-function \
      --function-name "$FUNCTION_NAME" \
      --runtime "$RUNTIME" \
      --role "$ROLE_ARN" \
      --handler "$HANDLER" \
      --zip-file "fileb://$JAR_FILE" \
      --timeout "$TIMEOUT" \
      --memory-size "$MEMORY" \
      --region "$REGION"
  fi
done

