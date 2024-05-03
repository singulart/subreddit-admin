# AWS Deployment

To save on AWS monthly bills, the app is deployed as a monolithic EC2 instance where all components coexist: PostgreSQL, Elastisearch and the Java app itself.
This approach is definitely NOT recommended for production!

## Connecting to EC2 monolith

Session Manager is used to connect to the EC2. No private keys, bastions, open SSH ports are required.

However, Session manager logs in to EC2 under its own user `ssm-user`. In order to execute commands under standard `ec2-user`, do

```bash
sudo -u ec2-user docker ps
```
