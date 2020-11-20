 def k8slabel = "jenkins-pipeline-${UUID.randomUUID().toString()}"
  properties([
      [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false], 
      parameters([
          booleanParam(defaultValue: false, description: 'Please select to apply all changes to the environment', name: 'terraform_apply'), 
          booleanParam(defaultValue: false, description: 'Please select to destroy all changes to the environment', name: 'terraform_destroy'),
      ])
  ])

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
            image: fuchicorp/buildtools
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
        dir('aws-vpc-1/deployments/terraform') {
          withCredentials([usernamePassword(credentialsId: 'aws-access', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
              stage("Terrraform Init"){
                sh """
                  source ./setenv.sh rash-vpc.tfvars
                """
              }        
            
              if (terraform_apply.toBoolean()) {
                stage("Terraform Apply"){
                    println("AWS VPC deployment starting in virginia")
                    sh """
                    terraform apply -var-file rash-vpc.tfvars -auto-approve
                    """
                }
              }
              else if (terraform_destroy.toBoolean()) {
                stage("Terraform Destroy"){
                  println("AWS VPC to be destroyed in virginia")
                    sh """
                      terraform destroy -var-file rash-vpc.tfvars -auto-approve
                    """
                }
              }
              else {
                stage("Terraform Plan"){
                  sh """
                  terraform plan -var-file rash-vpc.tfvars
                  echo "Nothinh to do.Please choose either apply or destroy"
                    
                  """
                }
              }                                 
          }
        }
      }
    }
  }