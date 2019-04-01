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

### Kubernetes configuration generation ###
sed "s#DEPLOYMENTNAME#$PRODUCT-$MODULE#g; s#NAMESPACE#$PRODUCT#g; s#PVCNAME#$PRODUCT-pvc#g; s#IMAGENAME#$image_tag#g" ../k8s/template-pasc-osmh-indexer-deployment.yaml > ../k8s/$PRODUCT-$MODULE-deployment.yaml
sed "s/SERVICENAME/$PRODUCT-$MODULE/g; s/NAMESPACE/$PRODUCT/g" ../k8s/template-pasc-$MODULE-service.yaml > ../k8s/$PRODUCT-$MODULE-service.yaml

# Kubctl credentials setup
gcloud container clusters get-credentials development-cluster --zone=$ZONE

# Deployment
kubectl apply -f ../k8s/$PRODUCT-$MODULE-deployment.yaml

# Service
kubectl apply -f ../k8s/$PRODUCT-$MODULE-service.yaml
