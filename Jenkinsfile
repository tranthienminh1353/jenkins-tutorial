pipeline {
    agent any
    stages {
        stage('Clone') {
            steps {
                git branch: 'main', url: 'https://github.com/tranthienminh1353/jenkins-tutorial.git'
            }
        }
        stage('Build') {
            steps {
                echo 'Hello world'
            }
        }
        // add more stages if needed
    }
}
