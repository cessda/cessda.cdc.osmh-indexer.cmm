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
sed "s/DEPLOYMENTNAME/$PRODUCT-$MODULE-$ENVIRONMENT/g; s/NAMESPACE/$PRODUCT-$ENVIRONMENT/g; s/PVCNAME/$PRODUCT-$ENVIRONMENT-pvc/g" ../k8s/template-pasc-$MODULE-deployment.yaml > ../k8s/$PRODUCT-$MODULE-$ENVIRONMENT-deployment.yaml
sed "s/SERVICENAME/$PRODUCT-$MODULE-$ENVIRONMENT/g; s/NAMESPACE/$PRODUCT-$ENVIRONMENT/g" ../k8s/template-pasc-$MODULE-service.yaml > ../k8s/$PRODUCT-$MODULE-$ENVIRONMENT-service.yaml

# Kubctl credentials setup
gcloud container clusters get-credentials $PRODUCT-$ENVIRONMENT-cc --zone=$ZONE ### > /dev/null 2>&1

# Deployment
if kubectl get deployment $PRODUCT-$MODULE-$ENVIRONMENT -n $PRODUCT-$ENVIRONMENT; ### > /dev/null 2>&1;
  then
    echo "Deployment already exists, it will be destroyed to perform the new deployment"
    kubectl delete deployment $PRODUCT-$MODULE-$ENVIRONMENT -n $PRODUCT-$ENVIRONMENT ### > /dev/null 2>&1
    kubectl create -f ../k8s/$PRODUCT-$MODULE-$ENVIRONMENT-deployment.yaml
    echo "Deployment created"
  else
    kubectl create -f ../k8s/$PRODUCT-$MODULE-$ENVIRONMENT-deployment.yaml
    echo "Deployment created"
fi;

# Service
if kubectl get service $PRODUCT-$MODULE-$ENVIRONMENT -n $PRODUCT-$ENVIRONMENT; ### > /dev/null 2>&1;
  then
    echo "Service already exists"
  else
    kubectl create -f ../k8s/$PRODUCT-$MODULE-$ENVIRONMENT-service.yaml ### > /dev/null 2>&1
    echo "Service created"
fi;
