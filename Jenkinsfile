pipeline {
    agent any
    stages {
        stage('Clone') {
            steps {
                git branch: 'main', url: 'https://github.com/tranthienminh1353/jenkins-tutorial.git'
            }
        }
        stage('Docker') {
            withDockerRegistry(credentialsId: 'docker_id', url: 'https://index.docker.io/v1') {
                sh 'docker build -t tranthienminh135/jenkins-tutorial:v10'
                sh 'docker push tranthienminh135/jenkins-tutorial:v10'
            }
        }
    }
}
