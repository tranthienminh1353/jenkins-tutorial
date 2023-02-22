pipeline {
    agent any
    stages {
        states('Clone') {
            step {
                git branch: 'main', credentialsId: 'account_id', url: 'https://github.com/tranthienminh1353/jenkins-tutorial.git'
            }
        }
    }
}