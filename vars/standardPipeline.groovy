def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
pipeline {
    agent any
    stages {
        stage("Initialize Pipeline") {
            steps {
                script {
                    echo "Ansible Playbook Pipeline....."
                    sh "#!/usr/bin/sh \necho 'Setting up ansible configuration'"
                    sh "#!/usr/bin/sh \necho '${Hosts}' > ${WORKSPACE}/iac/ansible/inventory"
                    sh "#!/usr/bin/sh \necho 'AnsiblePassword: ${env.AnsiblePassword}' > '${WORKSPACE}/iac/ansible/passdata.yml'"
                    sh "export ANSIBLE_HOST_KEY_CHECKING=False"
                }
            }
        }
        stage("Ansible Execution") {
            steps{
                script {
                    echo 'Running Ansible Playbook'
                    command = getAnsibleCommand('')
                    echo "NODE_NAME: ${env.NODE_NAME}"
                    if ("${env.NODE_NAME}" == "master"){
                        sh "#!/usr/bin/sh \n${command}"
                    } else {
                        echo "Cleaning Directory"
                        sshPublisher (
                            continueOnError: false, failOnError: true,
                            publishers: [ sshPublisherDesc(
                                configName: 'Ansible server ',
                                transfers: [
                                    sshTransfer(
                                        sourceFiles: "",
                                        removePrefix: "",
                                        remoteDirectory: "",
                                        cleanRemote: true,
                                        execCommand: "rm -rf roles/${directory_name}/*"
                                    )
                                ]
                            )]
                        )
                        echo "Sending the Ansible Playbook command using ssh."
                        command = getAnsibleCommand('')

                        sshPublisher (
                            continueOnError: false, failOnError: true,
                            publishers: [ sshPublisherDesc(
                                configName: 'Ansible server ',
                                transfers: [
                                    sshTransfer(
                                        sourceFiles: "roles/${directory_name}/**",
                                        removePrefix: "",
                                        remoteDirectory: "",
                                        cleanRemote: true,
                                        execCommand: "${command}",
                                        execTimeout: 120000000
                                    )
                                ]
                            )]
                        )
                    }
                }
            }
        }
    }

}

def getAnsibleCommand(rolename){
    command = "ansible-playbook -i iac/ansible/inventory "
    def variableList = ["-e AnsibleUsername=${env.AnsibleUsername}", "-e ansible_password=${env.AnsiblePassword}", "-e state=${env.state}", "-e daemon=${env.daemon}"]
    variableList.each { item ->
        command += "${item} "
    }
    command += "${WORKSPACE}/iac/ansible/main.yml -vvv"
    return command
}
}
