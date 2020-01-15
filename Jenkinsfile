/**
# Copyright CESSDA ERIC 2017-2019
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
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
		// Building on master
		stage('Pull SDK Docker Image') {
		    agent {
		        docker {
                    image 'maven:3-jdk-11'
                    reuseNode true
                }
            }
		    stages {
                stage('Build Project') {
                    steps {
                        withMaven {
                            sh "export PATH=$MVN_CMD_DIR:$PATH && mvn clean install -DbuildNumber=${env.BUILD_NUMBER}"
                        }
                    }
                    when { branch 'master' }
                }
                // Not running on master - test only (for PRs and integration branches)
                stage('Test Project') {
                    steps {
                        withMaven {
                            sh 'export PATH=$MVN_CMD_DIR:$PATH && mvn clean test'
                        }
                    }
                    when { not { branch 'master' } }
                }
                stage('Record Issues') {
                    steps {
                        recordIssues(tools: [java()])
                    }
                }
                stage('Run Sonar Scan') {
                    steps {
                        withSonarQubeEnv('cessda-sonar') {
                            withMaven {
                                sh "export PATH=$MVN_CMD_DIR:$PATH && mvn sonar:sonar -DbuildNumber=${env.BUILD_NUMBER}"
                            }
                        }
						timeout(time: 1, unit: 'HOURS') {
							waitForQualityGate abortPipeline: true
						}
                    }
                    when { branch 'master' }
                }
            }
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
					build job: 'cessda.cdc.deploy/master', parameters: [string(name: 'osmh_repo_image_tag', value: "${env.BRANCH_NAME}-${env.BUILD_NUMBER}")], wait: false
				}
			}
            when { branch 'master' }
		}
	}
}
