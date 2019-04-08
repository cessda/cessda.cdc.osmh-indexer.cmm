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
echo "PRODUCT = $product_name"

### Kubernetes Deployment ###
sed -i "s/localhost:8087/$product_name-admin:8087/g" ./src/main/resources/application.yml

