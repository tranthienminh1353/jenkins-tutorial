pipeline {
    agent any
    stages {
        stage('Clone') {
            steps {
                git branch: 'main', url: 'https://github.com/tranthienminh1353/jenkins-tutorial.git'
            }
        }
        stage('Docker') {
            steps {
                withDockerRegistry(credentialsId: 'docker_hub', url: 'https://index.docker.io/v1/') {
                bat 'docker build -t jenkins-tutorial .'
                bat 'docker tag jenkins-tutorial tranthienminh135/jenkins-tutorial'
                bat 'docker push tranthienminh135/jenkins-tutorial'
                }
            }
        }
    }
    post{
        always{
            slackSend (channel: '#noti', color: 'good', message: 'Build successful!', tokenCredentialId: "slid")
        }
    }
    
}
