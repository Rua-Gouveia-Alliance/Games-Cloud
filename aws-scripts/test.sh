#!/bin/bash

BASE_URL="http://ec2-35-180-58-116.eu-west-3.compute.amazonaws.com:8080/fifteenpuzzle"

for ((i=5; i<=80; i+=5)); do
    echo "Requesting size=$i and shuffles=$i"
    curl "${BASE_URL}?size=${i}&shuffles=${i}"
    echo -e "\n"
done

