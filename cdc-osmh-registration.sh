#!/bin/bash

#############################
#          CESSDA           #
#      Cluster Setup        #
#############################

# Julien Le Hericy
# CESSDA ERIC
# j.lehericy(at)cessda.eu


echo "Setup for Spring Boot Admin Registration"
sed -i "s/APPNAME/$product_name-$module_name/g; s/localhost:8087/$product_name-admin:8087/g" ./src/main/resources/application.yml

