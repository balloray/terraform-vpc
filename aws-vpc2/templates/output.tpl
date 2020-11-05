

You were able to successfully deploy the VPC deployment. If you are facing some issues and if you change something in the code. Feel free to submit the code into projects. 



Submit issue to VPC  Module
https://github.com/fuchicorp/terraform-aws-vpc/issues/new


Submit issue to cluster infrastructure
https://github.com/fuchicorp/cluster-infrastructure/issues/new



########################   OUTPUTS   ###########################


Your VPC: ${vpc_id}

1. Public Subnet ID: [
    "${public_subnet_1}",
    "${public_subnet_2}",
    "${public_subnet_3}",
    "${public_subnet_4}",
    ]


1. Private Subnet ID: [
    "${private_subnet_1}",
    "${private_subnet_2}",
    "${private_subnet_3}",
    "${private_subnet_4}",
    ]



NAT GateWay:          ${nat_gateway}
Internet GateWay:     ${internet_gateway} 



Security Groups: [
    "${second_security_group}",
    "${first_security_group}",
]
