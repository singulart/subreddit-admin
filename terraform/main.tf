provider "aws" {
  region = "us-east-1"
}


module "vpc_mod_lex" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "5.8.1"

  name = "lex-vpc"
  cidr = "10.0.0.0/16"

  azs             = ["us-east-1a"]
  private_subnets = ["10.0.1.0/24"]
  public_subnets  = ["10.0.101.0/24"]

  enable_nat_gateway = true
 #  reuse_nat_ips = true 
  
  tags = {
    Terraform   = "true"
    Environment = "lab"
    Owner       = "lex"
  }
}

module "lex_log_group" {
  source = "terraform-aws-modules/cloudwatch/aws//modules/log-group"
  version = "5.3.1"  

  name = "/ec2/lex-application-logs"
}


resource "aws_iam_role" "ec2_cw_logs_role" {
  name = "ec2_cw_logs_role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      },
    ]
  })
}

resource "aws_iam_role_policy" "ec2_cw_logs_policy" {
  name   = "EC2CloudWatchLogsPolicy"
  role   = aws_iam_role.ec2_cw_logs_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "logs:CreateLogGroup"
        ],
        Effect   = "Allow",
        Resource = "arn:aws:logs:*:*:*"
      },
    ]
  })
}

module "iam_assumable_role" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-assumable-role"
  version = "5.39.0"

  create_role = true
  role_name   = "EC2CloudWatchLogsRole"
  trusted_role_arns = ["arn:aws:iam::${module.vpc_mod_lex.vpc_owner_id}:root"]

  custom_role_policy_arns = [
    "arn:aws:iam::aws:policy/CloudWatchLogsFullAccess"
  ]
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "EC2CloudWatchLogsProfile"
  role = module.iam_assumable_role.iam_role_name
}

data "aws_ami" "selected" {
  owners      = ["amazon"] 
  most_recent = true

  filter {
    name   = "name"
    values = ["al2023-ami-*-arm64"] 
  }
}



module "ec2_instance" {
  source  = "terraform-aws-modules/ec2-instance/aws"
  version = "5.6.1"

  name           = "LexMonolith"
  instance_type  = "t4g.2xlarge"
  ami            = data.aws_ami.selected.id
  subnet_id      = module.vpc_mod_lex.private_subnets[0]
  iam_instance_profile = aws_iam_instance_profile.ec2_profile.name

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


  tags = {
    "Name" = "LexMonolith"
  }
}

