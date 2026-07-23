/*
 * Copyright © 2017-2025 CESSDA ERIC (support@cessda.eu)
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

	environment {
		product_name = "cdc"
		module_name = "osmh-indexer"
		image_tag = "${DOCKER_ARTIFACT_REGISTRY}/${product_name}-${module_name}:${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
	}

    agent {
        label 'jnlp-himem'
    }

	stages {
		// Building on main
		stage('Pull SDK Docker Image') {
            agent {
                docker {
                    image 'eclipse-temurin:25'
                    reuseNode true
                }
            }
            environment {
                HOME = "${WORKSPACE_TMP}"
            }
		    stages {
                stage('Build Project') {
                    steps {
                        withMaven {
                            sh "./mvnw -Pnative clean verify -DbuildNumber=${env.BUILD_NUMBER}"
                        }
                    }
                }
                stage('Record Issues') {
                    steps {
                        discoverGitReferenceBuild()
                        recordCoverage(tools: [[parser: 'JACOCO']])
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
        stage('Build Docker Image') {
            steps {
                withMaven {
                    sh "./mvnw -Pnative spring-boot:build-image-no-fork -Dspring-boot.build-image.imageName=${image_tag}"
                }
            }
            //when { branch 'main' }
        }
		stage('Build and Push Docker image') {
            steps {
                sh "gcloud auth configure-docker ${ARTIFACT_REGISTRY_HOST}"
                sh "docker push ${IMAGE_TAG}"
                sh "gcloud artifacts docker tags add ${image_tag} ${DOCKER_ARTIFACT_REGISTRY}/${product_name}-${module_name}:${env.BRANCH_NAME}-latest"
            }
            when { branch 'main' }
		}
		stage('Check Requirements and Deployments') {
			steps {
                build job: 'cessda.cdc.deploy/main', parameters: [string(name: 'osmh_indexer_image_tag', value: "${env.BRANCH_NAME}-${env.BUILD_NUMBER}")], wait: false
			}
            when { branch 'main' }
		}
	}
}
