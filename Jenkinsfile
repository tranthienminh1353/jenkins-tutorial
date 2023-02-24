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
                    bat 'docker build -t bestwork .'
                    bat 'docker tag bestwork tranthienminh135/bestwork-dev'
                    bat 'docker push tranthienminh135/jenkins-tutorial'
                }
            }
        }
    }
    post {
            success {
                slackSend (color: 'good', message: 'Build successful!')
            }
            failure {
                slackSend (color: 'danger', message: 'Build failed!')
            }
        }
}
