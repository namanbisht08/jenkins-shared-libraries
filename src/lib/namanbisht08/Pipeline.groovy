package lib.namanbisht08;

def ImagePublishECR(String REGION, String repo_name, String DockerfileName, String DockerfilePath, String version ) {
	println "Building Docker Image"
	DockerImage = docker.build("${repo_name}", "-f ${DockerfileName} ${DockerfilePath}")

	println "Getting login to AWS ECR"
//	sh "$(aws ecr get-login --no-include-email --region ${REGION})"
        def login = sh (
                            script: "aws ecr get-login --no-include-email --region ${REGION} ",
                            returnStdout: true
                        )
        

        login.execute().text
	println "Pushing Image to ECR"
	DockerImage.push("${version}")
       // DockerImage.push("latest")
}

def ImageTagCheck(String repo_name, String version) {
        int status = sh(
                        script: "docker manifest inspect ${repo_name}:${version}",
                        returnStatus: true
                        )
        println status
        if (status !=0) {
                println "image do not exists"
        }
        else{
                println "image already exists with the tag ${version}"
                currentBuild.result = 'FAILURE'
                skipRemainingStages = true
                sh 'exit 1'
                return
        }
}

def EcrLogin(String REGION) {
        def login = sh (
                            script: "aws ecr get-login --no-include-email --region ${REGION} ",
                            returnStdout: true
                        )
        

        login.execute().text
}

def DockerImageTag(String version, String version1, String repo_name, String REGION) {
        EcrLogin(REGION)
        newImage = docker.image("${repo_name}:${version}")
    
        newImage.push("${version1}")
}


def DockerSonarBuild(String REGION, String repo_name, String DockerfileName, String DockerfilePath, String SONAR_PROJECT_KEY, String SONAR_HOST_URL, String SONAR_LOGIN_TOKEN ) {
	println "Building Sonar Build"
	
	
	DockerSonarImage = docker.build("${repo_name}", "--build-arg PROJECT_KEY=${SONAR_PROJECT_KEY}", "--build-arg HOST_URL=${SONAR_HOST_URL}", "--build-arg LOGIN_TOKEN=${SONAR_LOGIN_TOKEN}", "-f ${DockerfileName} ${DockerfilePath}")
}
