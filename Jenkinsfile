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
		// Building on master
		stage('Build Project') {
			steps {
				withMaven {
				    sh 'mvn clean deploy -Pdocker-compose -Dmaven.test.failure.ignore=true'					
				}
			}
			when { branch 'master' }
		}
        // Not running on master - test only (for PRs and integration branches)
		stage('Test Project') {
			steps {
				withMaven {
					sh 'mvn clean test -Pdocker-compose'					
				}
			}
			when { not { branch 'master' } }
		}
		stage('Run Sonar Scan') {
			steps {
				withSonarQubeEnv('cessda-sonar') {
					nodejs('node') {
						withMaven {
							sh 'mvn sonar:sonar -Pdocker-compose'
						}
					}
				}
			}
			when { branch 'master' }
		}
		stage('Get Sonar Quality Gate') {
			steps {
				timeout(time: 1, unit: 'HOURS') {
					waitForQualityGate abortPipeline: true
				}
			}
            when { branch 'master' }
		}
		stage('Build Docker image') {
			 steps {
				sh("docker build -t ${image_tag} .")
			}
			when { branch 'master' }
		}
		stage('Push Docker image') {
			steps {
				sh("gcloud auth configure-docker")
				sh("docker push ${image_tag}")
				sh("gcloud container images add-tag ${image_tag} ${docker_repo}/${product_name}-${module_name}:${env.BRANCH_NAME}-latest")
			}
			when { branch 'master' }
		}
		stage('Check Requirements and Deployments') {
			steps {
				dir('./infrastructure/gcp/') {
					build job: 'cessda.cdc.deploy/master', parameters: [string(name: 'osmh_indexer_image_tag', value: "${image_tag}"), string(name: 'module', value: 'osmh-indexer')], wait: false
				}
			}
            when { branch 'master' }
		}
	}
}
