provider "aws" {
  region = var.region
  profile = var.aws_profile
}

data "http" "myip" {
  url = "https://ipv4.icanhazip.com"
}

# data "aws_ip_ranges" "instance_connect" {
#   regions  = [var.region]
#   services = ["ec2_instance_connect"]
# }

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.8"

  name = "${var.app_name}-vpc"
  cidr = "10.0.0.0/16"

  azs             = var.azs
  private_subnets = ["10.0.1.0/24"]
  public_subnets  = ["10.0.101.0/24"]

  default_security_group_ingress = [
    {
      description = "HTTPS ingress"
      from_port     = 443
      to_port     = 443
      protocol    = "TCP"
      cidr_blocks = "0.0.0.0/0"
    }, 
    {
      from_port = 5432
      to_port = 5432
      protocol = "tcp"
      cidr_blocks = "${chomp(data.http.myip.response_body)}/32"
    }, 
    {
      description = "SSH port is necessary for EC2 Instance Connect"
      from_port = 22
      to_port = 22
      protocol = "tcp"
      cidr_blocks = "${chomp(data.http.myip.response_body)}/32" # data.aws_ip_ranges.instance_connect.cidr_blocks[0]
    }
  ]

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
    Owner       = var.app_name
  }
}

data "aws_ami" "selected" {
  owners      = ["amazon"] 
  most_recent = true

  filter {
    name   = "name"
    values = ["al2023-ami-*-arm64"] 
  }
}

# resource aws_iam_group instance_connect {
#   name = "ec2_instance_connect"
# }

# # --------------------------------------------------------------------------------
# # policy to allow use of instance connect to the instance(s)
# # derived from arn:aws:iam::aws:policy/EC2InstanceConnect
# # --------------------------------------------------------------------------------
# resource aws_iam_policy instance_connect {
#   name = "ec2_instance_connect"
#   description = "Allows use of Instance Connect to the constructed instance(s)"
#   policy      = <<-EOF
#         {
#           "Version": "2012-10-17",
#           "Statement": [
#               {
#                   "Sid": "EC2InstanceConnect",
#                   "Action": [
#                       "ec2:DescribeInstances",
#                       "ec2-instance-connect:SendSSHPublicKey"
#                   ],
#                   "Effect": "Allow",
#                   "Resource": "*",
#                   "Condition": {
#                     "StringEquals": {
#                       "ec2:osuser": "ec2-user"
#                     }
#                   }
#               }
#           ]
#         }
#         EOF
# }

# resource aws_iam_group_policy_attachment instance_connect {
#   group      = aws_iam_group.instance_connect.name
#   policy_arn = aws_iam_policy.instance_connect.arn
# }

module "key_pair" {
  source = "terraform-aws-modules/key-pair/aws"

  key_name   = "${var.app_name}_key"
  public_key = file("${var.ec2_key}")
}

module "ec2_instance" {
  source  = "terraform-aws-modules/ec2-instance/aws"
  version = "~> 5.6"

  name           = "${var.app_name}_box"
  instance_type  = var.ec2_type
  ami            = data.aws_ami.selected.id
  subnet_id      = module.vpc.public_subnets[0]
  monitoring     = true
  create_iam_instance_profile = true
  iam_role_name               = "${var.app_name}_ec2_role"
  iam_role_path               = "/ec2/"
  iam_role_description        = "Minimal set of permissions for the EC2 VM"
  iam_role_policies = {
    AmazonSSMManagedInstanceCore       = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore",
    CloudWatchAgentServerPolicy        = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy",
    AmazonEC2ContainerRegistryReadOnly = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly",
    AmazonRoute53ReadOnlyAccess        = "arn:aws:iam::aws:policy/AmazonRoute53ReadOnlyAccess",
    AmazonRoute53AutoNamingFullAccess  = "arn:aws:iam::aws:policy/AmazonRoute53AutoNamingFullAccess"
  }
  enable_volume_tags = false
  root_block_device = [
    {
      encrypted   = true
      volume_type = "gp3"
      throughput  = 200
      volume_size = 50
      tags = {
        Name = "my-root-block"
      }
    },
  ]
  associate_public_ip_address          = true
  key_name = module.key_pair.key_pair_name

  user_data = <<-EOF
                #!/bin/bash

                POSTGRESQL_VERSION=postgresql16
                ELASTIC_VERSION=8.17.2

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
                yum install -y amazon-ssm-agent
                yum install -y pip
                yum install -y python3-certbot-dns-route53
                yum install -y certbot
                yum install -y $POSTGRESQL_VERSION

                systemctl enable amazon-ssm-agent
                systemctl start amazon-ssm-agent

                # Install Docker and configure the service
                yum install -y docker
                systemctl start docker
                usermod -a -G docker ec2-user

                # Create persistent location for PostgreSQL data
                mkdir -p $POSTGRES_DATA_DIR

                # Create Docker network
                docker network create $DOCKER_NETWORK

                # Start PostgreSQL container
                docker run --name $POSTGRES_CONTAINER \
                  --network=$DOCKER_NETWORK \
                  --log-driver awslogs \
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
                  --log-driver awslogs \
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
    "Name" = "${var.app_name}"
  }
}

# Create a Route 53 public hosted zone
resource "aws_route53_zone" "main" {
  name = "${var.dns_name}"
}

# Create an A-record pointing to the EC2 instance public IP
resource "aws_route53_record" "a_record" {
  zone_id = aws_route53_zone.main.zone_id
  name    = "${var.dns_name}"
  type    = "A"
  ttl     = "300"
  records = [module.ec2_instance.public_ip]
}

