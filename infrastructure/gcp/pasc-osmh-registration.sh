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
echo "ENVIRONMENT = $ENVIRONMENT"

# Kubctl credentials setup
gcloud container clusters get-credentials $PRODUCT-$ENVIRONMENT-cc --zone=$ZONE > /dev/null 2>&1

### Kubernetes Deployment ###
if kubectl get deployment $PRODUCT-admin-$ENVIRONMENT -n $PRODUCT-$ENVIRONMENT > /dev/null 2>&1;
  then
    echo "Admin component available, deployment will be processed"
    echo "Setup for Spring Boot Admin Registration"
    sed -i "s/APPNAME/$PRODUCT-$MODULE-$ENVIRONMENT/g; s/localhost:8087/$PRODUCT-admin-$ENVIRONMENT:8087/g" ../../src/main/resources/application.yml
  else
    echo "Admin component not available, deployment's aborted"
    exit 1
fi;
