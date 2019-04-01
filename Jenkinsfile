pipeline {
  environment {
    project_name = "cessda-dev"
    module_name = "pasc-oci"
    image_tag = "eu.gcr.io/${project_name}/${module_name}:${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
  }

  agent any

  stages {
    stage('Check environment') {
      steps {
	      echo "Check environment"
        echo "project_name = ${project_name}"
        echo "module_name = ${module_name}"
        echo "image_tag = ${image_tag}"
      }
    }
    stage('Prepare Application for registration with Spring Boot Admin') {
      steps {
        dir('./infrastructure/gcp/') {
          sh("./pasc-osmh-registration.sh")
        }
      }
    }
    stage('Build Project and Run Sonar Scan') {
		  steps {
        withSonarQubeEnv('cessda-sonar') {
          withMaven(options: [junitPublisher(healthScaleFactor: 1.0)], tempBinDir: '') {
            sh 'mvn clean install sonar:sonar'
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
        sh("gcloud container images add-tag ${image_tag} eu.gcr.io/${project_name}/${env.BRANCH_NAME}-${module_name}:latest")
      }
    }
    stage('Check Requirements and Deployments') {
      steps {
        dir('./infrastructure/gcp/') {
          sh("./pasc-osmh-creation.sh")
        }
      }
    }
  }
}
