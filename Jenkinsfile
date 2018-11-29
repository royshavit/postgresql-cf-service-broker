pipeline {
    agent none

    stages {

        stage('mvn -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn deploy') {
            when {
                anyOf { branch 'dev'; branch 'master' }
            }
            agent {
                docker {
                    image 'maven:3.5.3-jdk-8'
                    label 'dind'
                    args '-v /root/.m2:/root/.m2'
                }
            }
            environment {
                REPO = credentials('Repository_Credential')
                PREDIX_IO_REPO = credentials('Predix_IO_Repository_Credential')
            }
            steps {
                sh 'mvn -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn clean deploy -B -s jenkins_settings.xml -Drepo.username=$REPO_USR -Drepo.password=$REPO_PSW -Dpredixio.username=$PREDIX_IO_REPO_USR -Dpredixio.password=$PREDIX_IO_REPO_PSW'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    step([$class: 'JacocoPublisher'])
                }
            }
        }
    }
}
