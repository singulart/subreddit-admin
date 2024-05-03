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
  iam_role_description        = "Role that enables sending logs from this instance to CloudWatch"
  iam_role_policies = {
    AmazonSSMManagedInstanceCore = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore",
    CloudWatchAgentServerPolicy  = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
  }

  user_data = <<-EOF
                #!/bin/bash
                # Installing additional packages
                yum update -y && yum install -y awslogs && yum install -y postgresql15
                systemctl start awslogsd
                systemctl enable awslogsd.service
                
                # Installing Docker
                sudo amazon-linux-extras install docker -y
                sudo service docker start
                sudo usermod -a -G docker ec2-user

                # Create persistent location for PostgreSQL data
                sudo mkdir -p /opt/postgresql/data

                # Start PostgreSQL 
                docker run --name reddit-postgresql \
                  --log-driver=awslogs \
                  --log-opt awslogs-group=/ec2/lex-monolith/postgres \
                  --log-opt awslogs-create-group=true \
                  -p 5432:5432 \
                  -v /opt/postgresql/data:/var/lib/postgresql/data \
                  -e POSTGRES_PASSWORD=TODO_REPLACE_WITH_AWS_SECRET_MANAGER \
                  -d postgres 

                # Start Elastic 
                docker run --name reddit-elasticsearch \
                  --log-driver=awslogs \
                  --log-opt awslogs-group=/ec2/lex-monolith/elastic \
                  --log-opt awslogs-create-group=true \
                  -p 9200:9200 \
                  -p 9300:9300 \
                  -e "discovery.type=single-node" \
                  -e"xpack.security.enabled=false" \
                  -d docker.elastic.co/elasticsearch/elasticsearch:8.13.3
                EOF
  user_data_replace_on_change = true

  tags = {
    "Name" = "LexMonolith"
  }
}

