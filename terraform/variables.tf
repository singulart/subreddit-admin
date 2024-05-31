variable "region" {
  description = "AWS region"
  type        = string
}

variable "aws_profile" {
  description = "AWS CLI profile"
  type        = string
}

variable "app_name" {
  description = "Name of the application"
  type        = string
}

variable "dns_name" {
  description = "DNS name"
  type        = string
}

variable "azs" {
  description = "Availability zones"
  type = list(string)
}

variable "ec2_type" {
  description = "Class of EC2 intance used"
  type        = string
}

variable "ec2_key" {
  description = "EC2 public key location"
  type        = string
}