pipeline {
  agent any
  stages {
    stage('CI Build') {
      when {
        branch 'PR-*'
      }
      steps {
        checkout scm
        sh "mvn clean install"
      }
    }

    stage('Build and Push Release') {
      when {
        branch 'master'
      }
      steps {
        git "https://github.com/jenkins-x/updatebot"
        sh 'sleep 100000'
        sh './jx/scripts/release.sh'
      }
    }
  }
}
