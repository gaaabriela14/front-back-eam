pipeline {

    environment {
        BASE_GIT_URL = 'https://github.com/gaaabriela14'
        APP_REPO_URL = "${env.BASE_GIT_URL}/${nombre_repositorio}.git"
        INFRA_REPO_URL = "${env.BASE_GIT_URL}/practica-seminario.git"
        DOCKER_IMAGE = "gachu/${nombre_repositorio}"
        DEPLOY_FOLDER = "deploy/practicaK8/${nombre_repositorio}"
    }

    agent any
    stages {
        stage("Checkout app-code") {
            steps {
            //se esta crando una carpeta....
               dir('app') {
                    git url:"${env.APP_REPO_URL}" , branch: "${tag}"
                } 
            }
        }
         
         stage("Checkout deploy-code") {
            steps {
               dir('deploy') {
                    git url:"${env.INFRA_REPO_URL}" , branch: "master"
                } 
            }
        }
        
         stage("Build image") {
            steps {
                dir('app') {
                    script {
                        dockerImage = docker.build("${env.DOCKER_IMAGE}:${tag}")
                    }
                }
            }
        }
        
        stage("Push image") {
            steps {
                script {
                    docker.withRegistry('', 'gabrieladocker') {
                    dockerImage.push()
                    }
                }
            }
        }
        
        stage('Deploy') {
            steps{
                sh "sed -i 's:DOCKER_IMAGE:${env.DOCKER_IMAGE}:g' ${DEPLOY_FOLDER}/deployment.yaml"
                sh "sed -i 's:TAG:${tag}:g' ${DEPLOY_FOLDER}/deployment.yaml"
                
                step([$class: 'KubernetesEngineBuilder', 
                        projectId: "nice-root-288300",
                        clusterName: "cluster-gabriela",
                        zone: "us-west1-a",
                        manifestPattern: "${DEPLOY_FOLDER}/",
                        credentialsId: "seminario",
                        verifyDeployments: true])
            }
        }
    }
}