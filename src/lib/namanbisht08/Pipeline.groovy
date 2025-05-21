package lib.namanbisht08;

def ImagePublishECR(String REGION, String repo_name, String DockerfileName, String DockerfilePath, String version, String accountId) {		
	println "Building Docker Image"
	DockerImage = docker.build("${repo_name}", "--no-cache -f ${DockerfileName} ${DockerfilePath}")

	println "Getting login to AWS ECR"
        def login = sh (
                            script: "aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${accountId}.dkr.ecr.${REGION}.amazonaws.com",
                            returnStdout: true
                        )
        

        //login.execute().text
	println "Pushing Image to ECR"
	DockerImage.push("${version}")
}


def Creating_updated_task_definition(String REGION, String TaskDefinitionFamily, String repoName, String version) {
    println "lib.namanbisht08: Creating an Updated Revision of Task Definition"

    sh """
        aws ecs describe-task-definition --task-definition ${TaskDefinitionFamily} --region ${REGION} > ${TaskDefinitionFamily}-task-def.json
    """

    sh """
        jq 'del(.taskDefinitionArn) | .taskDefinition.containerDefinitions[0].image = "${repoName}:${version}" | .taskDefinition.tags = [] | .taskDefinition.compatibilities = null | .taskDefinition.networkMode = "awsvpc" | .taskDefinition.executionRoleArn = "arn:aws:iam::187897899156:role/ecsTaskExecutionRole" | {family: "${TaskDefinitionFamily}", containerDefinitions: .taskDefinition.containerDefinitions, memory: .taskDefinition.memory, cpu: .taskDefinition.cpu, volumes: .taskDefinition.volumes, placementConstraints: .taskDefinition.placementConstraints, requiresCompatibilities: .taskDefinition.requiresCompatibilities, networkMode: .taskDefinition.networkMode, executionRoleArn: .taskDefinition.executionRoleArn}' ${TaskDefinitionFamily}-task-def.json > updated-${TaskDefinitionFamily}-task-def.json
    """
	
    sh """
        aws ecs register-task-definition --family ${TaskDefinitionFamily} --cli-input-json file://updated-${TaskDefinitionFamily}-task-def.json --region ${REGION}
    """

    sh """
        rm -f ${TaskDefinitionFamily}-task-def.json updated-${TaskDefinitionFamily}-task-def.json
    """

    println "====== New Task Definition Updated and Registered Successfully ======"
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
