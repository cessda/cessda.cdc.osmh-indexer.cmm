/*
 * Copyright © 2017-2023 CESSDA ERIC (support@cessda.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
pipeline {
	options {
		buildDiscarder logRotator(artifactNumToKeepStr: '5', numToKeepStr: '20')
	}

	environment {
		product_name = "cdc"
		module_name = "osmh-indexer"
		image_tag = "${docker_repo}/${product_name}-${module_name}:${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
	}

    agent {
        label 'jnlp-himem'
    }

	stages {
		// Building on main
		stage('Pull SDK Docker Image') {
		    agent {
		        docker {
                    image 'openjdk:17'
                    reuseNode true
                }
            }
		    stages {
                stage('Build Project') {
                    steps {
                        withMaven {
                            sh "./mvnw clean install -DbuildNumber=${env.BUILD_NUMBER}"
                        }
                    }
                    when { branch 'main' }
                }
                // Not running on main - test only (for PRs and integration branches)
                stage('Test Project') {
                    steps {
                        withMaven {
                            sh './mvnw clean verify'
                        }
                    }
                    when { not { branch 'main' } }
                }
                stage('Record Issues') {
                    steps {
                        recordIssues aggregatingResults: true, tools: [errorProne(), java()]
                    }
                }
                stage('Run Sonar Scan') {
                    steps {
                        withSonarQubeEnv('cessda-sonar') {
                            withMaven {
                                sh "./mvnw sonar:sonar -DbuildNumber=${env.BUILD_NUMBER}"
                            }
                        }
						timeout(time: 1, unit: 'HOURS') {
							waitForQualityGate abortPipeline: true
						}
                    }
                    when { branch 'main' }
                }
            }
        }
		stage('Build and Push Docker image') {
            steps {
                sh 'gcloud auth configure-docker'
                withMaven {
                    sh "./mvnw jib:build -Dimage=${image_tag}"
                }
                sh "gcloud container images add-tag ${image_tag} ${docker_repo}/${product_name}-${module_name}:${env.BRANCH_NAME}-latest"
            }
            when { branch 'main' }
		}
		stage('Check Requirements and Deployments') {
			steps {
				dir('./infrastructure/gcp/') {
					build job: 'cessda.cdc.deploy/main', parameters: [string(name: 'osmh_indexer_image_tag', value: "${env.BRANCH_NAME}-${env.BUILD_NUMBER}")], wait: false
				}
			}
            when { branch 'main' }
		}
	}
}
