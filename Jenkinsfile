pipeline {
  options {
    buildDiscarder logRotator(artifactNumToKeepStr: '5', numToKeepStr: '10')
  }

  environment {
    product_name = "cdc"
    module_name = "osmh-indexer"
    image_tag = "${docker_repo}/${product_name}-${module_name}:${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
  }

  agent any

  stages {
    stage('Check environment') {
      steps {
	      echo "Check environment"
        echo "product_name = ${product_name}"
        echo "module_name = ${module_name}"
        echo "image_tag = ${image_tag}"
      }
    }
    stage('Prepare Application for registration with Spring Boot Admin') {
      steps {
          sh("./osmh-gcp-configuration.sh")
      }
    }
    stage('Build Project and Run Sonar Scan') {
      agent {
        docker {
          image 'maven:3-jdk-11'
            reuseNode true
        }
      }
		  steps {
        withSonarQubeEnv('cessda-sonar') {
          withMaven(options: [junitPublisher(healthScaleFactor: 1.0)], tempBinDir: '') {
            sh 'export PATH=$MVN_CMD_DIR:$PATH && mvn clean install sonar:sonar'
          }
        }
      }
    }
    stage('Get Quality Gate Status') {
      steps {
        timeout(time: 1, unit: 'HOURS') {
          waitForQualityGate abortPipeline: true
        }
      }
    }
	  stage('Build Docker image') {
   		steps {
        sh("docker build -t ${image_tag} .")
      }
    }
    stage('Push Docker image') {
      steps {
        sh("gcloud auth configure-docker")
        sh("docker push ${image_tag}")
        sh("gcloud container images add-tag ${image_tag} ${docker_repo}/${product_name}-${module_name}:${env.BRANCH_NAME}-latest")
      }
    }
    stage('Check Requirements and Deployments') {
      steps {
        dir('./infrastructure/gcp/') {
          build job: 'cessda.cdc.deploy/master', parameters: [string(name: 'osmh_indexer_image_tag', value: "${image_tag}"), string(name: 'module', value: 'osmh-indexer')], wait: false
        }
      }
    }
  }
}
