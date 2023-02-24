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
                    bat 'docker tag bestwork tranthienminh135/bestwork'
                    bat 'docker push tranthienminh135/bestwork'
                }
            }
        }
        stage('Test') {
            steps {
                 junit 'path/to/test/reports/*.xml'
            }
            post {
                success {
                    echo 'All tests passed. Proceeding with build.'
                    currentBuild.result = "SUCCESS"
                }
                failure {
                    echo 'Tests failed. Skipping build.'
                    currentBuild.result = "FAILURE"
                }
            }
            when {
                expression { currentBuild.result == 'SUCCESS' }
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
