pipeline {
	options {
		buildDiscarder logRotator(artifactNumToKeepStr: '5', numToKeepStr: '10')
	}

	environment {
		product_name = "cdc"
		module_name = "osmh-repo"
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
				sh("./cdc-osmh-registration.sh")
			}
		}
		// Building on master
		stage('Build Project') {
            agent {
                docker {
                    image 'maven:3-jdk-11'
                    reuseNode true
                }
            }
			steps {
				withMaven {
                    sh 'export PATH=$MVN_CMD_DIR:$PATH && mvn clean deploy'				
				}
			}
			when { branch 'master' }
		}
        // Not running on master - test only (for PRs and integration branches)
		stage('Test Project') {
            agent {
                docker {
                    image 'maven:3-jdk-11'
                    reuseNode true
                }
            }
			steps {
				withMaven {
					sh 'export PATH=$MVN_CMD_DIR:$PATH && mvn clean test'					
				}
			}
			when { not { branch 'master' } }
		}
		stage('Run Sonar Scan') {
            agent {
                docker {
                    image 'maven:3-jdk-11'
                    reuseNode true
                }
            }
			steps {
				withSonarQubeEnv('cessda-sonar') {
                    withMaven {
                        sh 'export PATH=$MVN_CMD_DIR:$PATH && mvn sonar:sonar'
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
					build job: 'cessda.cdc.deploy/master', parameters: [string(name: 'osmh_repo_image_tag', value: "${image_tag}"), string(name: 'module', value: 'osmh-repo')], wait: false
				}
			}
            when { branch 'master' }
		}
	}
}
