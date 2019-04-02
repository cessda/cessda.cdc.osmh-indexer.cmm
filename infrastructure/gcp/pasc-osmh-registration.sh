#!/bin/bash

#############################
#          CESSDA           #
#      Cluster Setup        #
#############################

# Julien Le Hericy
# CESSDA ERIC
# j.lehericy(at)cessda.eu


### Load Env Variable and set gcloud Region/Zone/Project
source ./gcp.config > /dev/null 2>&1

echo "PRODUCT = $PRODUCT"

# Kubctl credentials setup
gcloud container clusters get-credentials development-cluster --zone=$ZONE

echo "Setup for Spring Boot Admin Registration"
sed -i "s/APPNAME/$PRODUCT-$MODULE/g; s/localhost:8087/$PRODUCT-admin:8087/g" ../../src/main/resources/application.yml

