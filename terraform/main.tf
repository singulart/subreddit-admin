provider "aws" {
  region = "us-east-1"
}


module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.8"

  name = "lex-vpc"
  cidr = "10.0.0.0/16"

  azs             = ["us-east-1a"]
  private_subnets = ["10.0.1.0/24"]
  public_subnets  = ["10.0.101.0/24"]


  #### ATTENTION! COST IMPACT!
  #### NAT Gateways are billed hourly and they are not super cheap. 
  #### However, in this particular case the WWW connectivity they provide is only needed at the time "user data" script runs.
  #### With Terraform it's not possible to provision temporary, disposable, resources which get deleted in the end of `apply` phase. 
  #### With all that said, after `apply` finishes, NAT Gateway needs to be deleted manually, or else there would be monthly cost associated with it. 
  enable_nat_gateway = true

  default_security_group_egress = [
    {
      description = "HTTPS egress"
      from_port   = 443
      to_port     = 443
      protocol    = "TCP"
      cidr_blocks = "0.0.0.0/0"
    }
  ]
  
  tags = {
    Terraform   = "true"
    Environment = "lab"
    Owner       = "lex"
  }
}

module "log_group" {
  source = "terraform-aws-modules/cloudwatch/aws//modules/log-group"
  version = "~> 5.3"
  name = "/ec2/lex-application-logs"
}


data "aws_ami" "selected" {
  owners      = ["amazon"] 
  most_recent = true

  filter {
    name   = "name"
    values = ["al2023-ami-*-arm64"] 
  }
}

# resource "aws_key_pair" "deployer" {
#   key_name   = "my-ssh-key"
#   public_key = file("~/.ssh/lex-aws-ec2.pub") 
# }

module "ec2_instance" {
  source  = "terraform-aws-modules/ec2-instance/aws"
  version = "~> 5.6"

  name           = "LexMonolith"
  instance_type  = "t4g.2xlarge"
  ami            = data.aws_ami.selected.id
  subnet_id      = module.vpc.private_subnets[0]
  monitoring     = true
  create_iam_instance_profile = true
  iam_role_name               = "lex_monolith_ec2_role"
  iam_role_path               = "/ec2/"
  iam_role_description        = "Enables sending logs to CloudWatch, pulling from ECR and connecting using Session Manager"
  iam_role_policies = {
    AmazonSSMManagedInstanceCore       = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore",
    CloudWatchAgentServerPolicy        = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy",
    AmazonEC2ContainerRegistryReadOnly = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
  }

  user_data = <<-EOF
                #!/bin/bash

                POSTGRESQL_VERSION=postgresql15
                ELASTIC_VERSION=8.13.3

                DOCKER_NETWORK=reddit-network
                POSTGRES_CONTAINER=reddit-postgresql
                POSTGRES_DATA_DIR=/opt/postgresql/data
                POSTGRES_DB=SubredditsAdmin
                POSTGRES_USER=SubredditsAdmin
                ES_CONTAINER=reddit-elasticsearch
                POSTGRES_PASSWORD=TODO_REPLACE_WITH_AWS_SECRET_MANAGER
                AWSLOGS_GROUP_PG=/ec2/lex-monolith/postgres
                AWSLOGS_GROUP_ES=/ec2/lex-monolith/elastic

                # Update and install packages
                yum update -y
                yum install -y awslogs 
                yum install -y $POSTGRESQL_VERSION 
                yum install -y docker

                # Configure and start AWS Logs
                systemctl start awslogsd.service
                systemctl enable awslogsd.service

                # Install Docker and configure the service
                amazon-linux-extras install docker -y
                service docker start
                usermod -a -G docker ec2-user

                # Create persistent location for PostgreSQL data
                sudo mkdir -p $POSTGRES_DATA_DIR

                # Create Docker network
                docker network create $DOCKER_NETWORK

                # Start PostgreSQL container
                docker run --name $POSTGRES_CONTAINER \
                  --network=$DOCKER_NETWORK \
                  --log-driver=awslogs \
                  --log-opt awslogs-group=$AWSLOGS_GROUP_PG \
                  --log-opt awslogs-create-group=true \
                  -p 5432:5432 \
                  -v $POSTGRES_DATA_DIR:/var/lib/postgresql/data \
                  -e POSTGRES_PASSWORD=$POSTGRES_PASSWORD \
                  -d postgres

                echo 'PostgreSQL container running'
                sleep 10
                echo 'Wait complete'

                # Create PostgreSQL user
                echo 'Creating user'
                printf "$POSTGRES_PASSWORD\n$POSTGRES_PASSWORD\n" | \
                  docker exec -i $POSTGRES_CONTAINER createuser $POSTGRES_USER -P -s -U postgres

                # Create PostgreSQL database
                echo 'Creating DB'
                docker exec -i $POSTGRES_CONTAINER createdb $POSTGRES_DB -U $POSTGRES_USER

                # Start Elasticsearch container
                docker run --name $ES_CONTAINER \
                  --network=$DOCKER_NETWORK \
                  --log-driver=awslogs \
                  --log-opt awslogs-group=$AWSLOGS_GROUP_ES \
                  --log-opt awslogs-create-group=true \
                  -p 9200:9200 \
                  -p 9300:9300 \
                  -e "discovery.type=single-node" \
                  -e "xpack.security.enabled=false" \
                  -d docker.elastic.co/elasticsearch/elasticsearch:$ELASTIC_VERSION
                EOF
  user_data_replace_on_change = true

  tags = {
    "Name" = "LexMonolith"
  }
}

