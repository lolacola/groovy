template = '''
apiVersion: v1
kind: Pod
metadata:
  labels:
    run: terraform
  name: terraform
spec:
  containers:
  - command:
    - sleep
    - "3600"
    image: hashicorp/terraform
    name: terraform
    '''

tfvars = """
   ami_id = "${params.ami_id}"
   instance_type = "t2.micro"
   az1 = "${params.az}"
   key_pair = "${params.key_pair}"
   region = "${params.region}"
"""

properties([
    parameters([
        choice(choices: ['apply', 'destroy'], description: 'Pick the action: ', name: 'action'),
        choice(choices: ['us-east-1', 'us-east-2', 'us-west-1', 'us-west-2'], description: 'Pick the region',  name: 'region'), 
        string(description: 'Enter AMI ID', name: 'ami_id', trim: true), 
        string(description: 'Enter availability zone', name: 'az', trim: true), 
        string(description: 'Enter your key_pair', name: 'key_pair', trim: true)
        ])
        ])

podTemplate(cloud: 'kubernetes', label: 'terraform', yaml: template) {
    node("terraform"){
        container("terraform"){
           stage("Clone repo"){
        git branch: 'main', url: 'https://github.com/lolacola/jenkins-terraform.git'
    }



    withCredentials([usernamePassword(credentialsId: 'aws-creds', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
        stage("Init"){
        sh "terraform init -backend-config='key=${params.region}/terraform.tfstate'"
    }
    
    if(params.action == "apply"){
        stage("Apply"){
        writeFile file: 'hello.tfvars', text: tfvars
        sh 'terraform apply -var-file hello.tfvars --auto-approve'
    }
    }
    else{
        stage("Destroy"){
        writeFile file: 'hello.tfvars', text: tfvars
        sh 'terraform destroy -var-file hello.tfvars --auto-approve'
    }
    }  
}
        }   
}
}
