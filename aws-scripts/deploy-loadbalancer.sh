#!/bin/bash

source config.sh

# Step 1: launch a vm instance.
$DIR/launch-loadbalancer-vm.sh

# Step 2: install software in the VM instance.
$DIR/install-loadbalancer-vm.sh

# Step 3: test VM instance.
$DIR/test-loadbalancer-vm.sh
