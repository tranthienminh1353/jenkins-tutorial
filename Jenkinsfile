pipeline {
    agent any

    environment {
            SLACK_CHANNEL = '#noti'
            SLACK_CREDENTIAL = 'tg3ocU7Wc2EEJxe7Pl6PdjqK'
            SLACK_WORKSPACE = 'jenkinsnotifi-jy12673'
        }

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
        post {
            success {
                slackSend (channel: color: 'good', message: 'Build successful!')
            }
            failure {
                slackSend (channel: color: 'danger', message: 'Build failed!')
            }
        }
    }
}
