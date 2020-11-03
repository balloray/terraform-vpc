
  def k8slabel = "jenkins-pipeline-${UUID.randomUUID().toString()}"
  properties([
      [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false], 
      parameters([
          booleanParam(defaultValue: false, description: 'Please select to apply all changes to the environment', name: 'terraform_apply'), 
          booleanParam(defaultValue: false, description: 'Please select to destroy all changes to the environment', name: 'terraform_destroy'),  
          choice(choices: ['dev', 'qa', 'stage', 'prod'], description: 'Please provide the environment to deploy ', name: 'environment')
      ])
  ])

  def aws_region_var = ''
  if(params.environment == "dev"){
      aws_region_var = "us-east-1"
  }
  else if(params.environment == "qa"){
      aws_region_var = "us-east-2"
  }
  else if(params.environment == "prod"){
      aws_region_var = "us-west-2"
  }

  def slavePodTemplate = """
        metadata:
          labels:
            k8s-label: ${k8slabel}
          annotations:
            jenkinsjoblabel: ${env.JOB_NAME}-${env.BUILD_NUMBER}
        spec:
          affinity:
            podAntiAffinity:
              requiredDuringSchedulingIgnoredDuringExecution:
              - labelSelector:
                  matchExpressions:
                  - key: component
                    operator: In
                    values:
                    - jenkins-jenkins-master
                topologyKey: "kubernetes.io/hostname"
          containers: 
          - name: fuchicorptools
            image: hashicorp/terraform
            imagePullPolicy: Always
            command:
            - cat
            tty: true
          serviceAccountName: default
          securityContext:
            runAsUser: 0
            fsGroup: 0
          volumes:
            - name: docker-sock
              hostPath:
                path: /var/run/docker.sock
  
      """
  podTemplate(name: k8slabel, label: k8slabel, yaml: slavePodTemplate, showRawYaml: false) {
      node(k8slabel){       
          stage("Pull Repo"){
              git url: 'https://github.com/balogunrash/jenkins-aws-vpc.git'
          }
        

        
        container("fuchicorptools") {
          
            withCredentials([usernamePassword(credentialsId: 'aws-access', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
              withEnv(["AWS_REGION=${aws_region_var}"]) {
                  stage("Terrraform Init"){
                    sh """
                        source setenv.sh ${environment}.tfvars
                        terraform init
                    """
                  }        
                
                  if (terraform_apply.toBoolean()) {
                    stage("Terraform Apply"){
                        println("AWS VPC deployment starting in ${aws_region_var}")
                        sh """
                        terraform apply -var-file ${environment}.tfvars -auto-approve
                        """
                    }
                  }
                  else if (terraform_destroy.toBoolean()) {
                    stage("Terraform Destroy"){
                      println("AWS VPC to be destroyed in ${aws_region_var}")
                        sh """
                          terraform destroy -var-file ${environment}.tfvars -auto-approve
                        """
                    }
                  }
                  else {
                    stage("Terraform Plan"){
                      sh """
                        terraform plan -var-file ${environment}.tfvars
                        echo "Nothinh to do in ${environment}.Choose either apply or destroy"
                        """
                    }
                  }            
                
              }
            }
          
        }
      }
  }
