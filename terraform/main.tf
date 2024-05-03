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
 #  reuse_nat_ips = true 
  
  tags = {
    Terraform   = "true"
    Environment = "lab"
    Owner       = "lex"
  }
}

# resource "aws_ec2_instance_connect_endpoint" "lex_ec2_instance_connect_endpoint" {
#   subnet_id = module.vpc.private_subnets[0]
# }

# module "vpc_endpoints" {
#   source = "terraform-aws-modules/vpc/aws//modules/vpc-endpoints"
#   version = "~> 5.8"
  
#   vpc_id = module.vpc.vpc_id
#   endpoints = {
#     ec2 = {
#         service             = "ec2"
#         private_dns_enabled = true
#         security_group_ids  = [module.vpc.default_security_group_id]
#         subnet_ids          = []
#         tags                = { Name = "lex-ec2-vpc-endpoint" }
#       }
#   }  
# }

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
  iam_role_description        = "Role that enables sending logs from this instance to CloudWatch"
  iam_role_policies = {
    AmazonSSMManagedInstanceCore = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore",
    CloudWatchAgentServerPolicy  = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
  }

  user_data = <<-EOF
                #!/bin/bash
                yum update -y
                yum install -y awslogs
                sudo amazon-linux-extras install docker -y
                sudo service docker start
                sudo usermod -a -G docker ec2-user
                systemctl start awslogsd
                systemctl enable awslogsd.service
                EOF
  user_data_replace_on_change = true

  tags = {
    "Name" = "LexMonolith"
  }
}

