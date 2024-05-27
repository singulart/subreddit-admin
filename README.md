# Building and publishing the Docker image for AWS

JHipster-generated README.md has a lot to improve when production deployment matters are concerned.

This project was created with AWS containerized deployment in mind. The Docker image needs to be built for ARM64 architecture:
`npm run java:docker:arm64`. Should a different architecture be required, use appropriate EC2 instance type that supports it.

Pushing the built image to the AWS Elastic Container Repository (make sure to create one beforehand!) is done using docker CLI:

```sh
docker tag subredditsadmin <AWS ACCOUNT NUMERIC ID>.dkr.ecr.<AWS REGION>.amazonaws.com/subredditsadmin:latest`
docker push <AWS ACCOUNT NUMERIC ID>.dkr.ecr.<AWS REGION>.amazonaws.com/subredditsadmin:latest
```

If Docker errors out credentials-related errors, you probably need to login first:

```sh
aws ecr get-login-password --region <AWS REGION>  | docker login --username AWS --password-stdin <AWS ACCOUNT NUMERIC ID>.dkr.ecr.<AWS REGION>.amazonaws.com
```

# (Almost) automated AWS infra

Most of the AWS infrastructure provisioning is automated using Terraform. To save on AWS costs, this project uses only one EC2 virtual instance to run both the Java app and its dependencies: PostgreSQL and ElasticSearch. Granted, this is not a recommended way to deal with production systems, but it does the job as a proof of concept.

AWS environment provisioning is as simple as:

```sh
cd terraform
terraform init
terraform plan
terraform apply
```

This will take a while to execute, but in the end you'll get the ready-to-use AWS environment. Both PostgreSQL and Elastic will be up and running! PostgreSQL user and application database will also be created for you. All that's left to do is to deploy the Java application.

## Name Servers

The resolution of a website's DNS name to the IP of EC2 instance it runs on is not easy to automate. The main difficulty is that whenever Terraform creates a Route53 Hosted Zone, AWS assignes it a random set of 3-4 name servers, and there is no control over this from the client's side. Therefore, you can't set, for example, `ns-1397.awsdns-46.org.` in your domain registrar's admin panel and expect this name server to be assigned to the Hosted Zone when you destroy and recreate the AWS infra next time.

This means, that after Terraform finishes its job, you need to add name server assigned to a Hosted Zone to your domain settings in the domain registrar's admin panel. Be aware that these changes will take time to propagate. After couple of hours you will be able to execute nslookup on your domain name and resolve it to the proper EC2 IP.

# SSL Certificates

Technically, this application can be used without any SSL certificates. If this works for your use-case, simply run it using the default HTTP port 8080 and whitelist incoming traffic to this port in the EC2 security group.

However, if we don't want web browsers to complain that the website is not secure, SSL needs to be properly set up.

Requesting SSL certificate from the free Let's Encrypt service (don't forget to donate them some dollars!) is done using command line tool certbot:

```sh
certbot certonly --dns-route53 --work-dir=~/certbot --config-dir=~/certbot --logs-dir=~/certbot -d subreddits.xyz --agree-tos --no-eff-email --email <your email>
```

This command, if successful, produces two files that you need to stitch together and convert to a different format.

```sh
cat /home/ec2-user/=~/certbot/live/subreddits.xyz/fullchain.pem /home/ec2-user/=~/certbot/live/subreddits.xyz/privkey.pem > keycert.pem

openssl pkcs12 -export -in keycert.pem -out keystore.p12 -name myalias
```

Note: `openssl` will ask to create a keystore passsword. The resulting `keystore.p12` is the Java keystore that the application needs to enable HTTPS and SSL.

# Manual Deployment to EC2

Deployment of the Java app to EC2 was not automated with Terraform, so you need to ssh to the EC2 and spin it up manually. If you wonder why: it's much faster to hop on a EC2 box and run one `docker run` command manually than re-create the entire instance from scratch with Terraform.

The easiest way to access the EC2 is using AWS Session Manager. It will log in under the different user, so the first thing you'd want to do is to switch back to ec2-user: `sudo -i -u ec2-user`.

Then simply run the Java app:

## No SSL

```sh
docker run --name tidder \
  --network=reddit-network \
  --log-driver=awslogs \
  --log-opt awslogs-group=/ec2/app \
  --log-opt awslogs-create-group=true \
  -p 8080:8080 \
  -e SPRING_ELASTICSEARCH_URIS=http://reddit-elasticsearch:9200 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://reddit-postgresql:5432/SubredditsAdmin \
  -e SPRING_DATASOURCE_PASSWORD=TODO_REPLACE_WITH_AWS_SECRET_MANAGER \
  -d <AWS ACCOUNT NUMERIC ID>.dkr.ecr.<AWS REGION>.amazonaws.com/subredditsadmin:latest
```

## With SSL

```sh
docker run --name tidder \
	--network=reddit-network \
	--log-driver=awslogs \
	--log-opt awslogs-group=/ec2/app \
	--log-opt awslogs-create-group=true \
	-p 443:443 \
	-v /home/ec2-user:/opt/java/ssl \
	-e SPRING_ELASTICSEARCH_URIS=http://reddit-elasticsearch:9200 \
	-e SPRING_DATASOURCE_URL=jdbc:postgresql://reddit-postgresql:5432/SubredditsAdmin \
	-e SPRING_DATASOURCE_PASSWORD=TODO_REPLACE_WITH_AWS_SECRET_MANAGER \
	-e SERVER_PORT=443 \
	-e SERVER_SSL_KEYSTORE=/opt/java/ssl/keystore.p12 \
	-e SERVER_SSL_KEYSTOREPASSWORD=<PASSWORD FOR YOUR KEYSTORE> \
	-e SERVER_SSL_KEYSTORETYPE=PKCS12 \
	-d <AWS ACCOUNT NUMERIC ID>.dkr.ecr.<AWS REGION>.amazonaws.com/subredditsadmin:latest
```

# Note on CloudWatch

EC2 logs from all three infrastructure pieces (the application itself, the DB and the search index) are sent to AWS CloudWatch groups "/ec2/XYZ". This should give enough insight into what's going on.

# Reddit Subs Admin UI

This application was generated using JHipster 8.4.0, you can find documentation and help at [https://www.jhipster.tech/documentation-archive/v8.4.0](https://www.jhipster.tech/documentation-archive/v8.4.0).

## Project Structure

Node is required for generation and recommended for development. `package.json` is always generated for a better development experience with prettier, commit hooks, scripts and so on.

In the project root, JHipster generates configuration files for tools like git, prettier, eslint, husky, and others that are well known and you can find references in the web.

`/src/*` structure follows default Java structure.

- `.yo-rc.json` - Yeoman configuration file
  JHipster configuration is stored in this file at `generator-jhipster` key. You may find `generator-jhipster-*` for specific blueprints configuration.
- `.yo-resolve` (optional) - Yeoman conflict resolver
  Allows to use a specific action when conflicts are found skipping prompts for files that matches a pattern. Each line should match `[pattern] [action]` with pattern been a [Minimatch](https://github.com/isaacs/minimatch#minimatch) pattern and action been one of skip (default if omitted) or force. Lines starting with `#` are considered comments and are ignored.
- `.jhipster/*.json` - JHipster entity configuration files

- `npmw` - wrapper to use locally installed npm.
  JHipster installs Node and npm locally using the build tool by default. This wrapper makes sure npm is installed locally and uses it avoiding some differences different versions can cause. By using `./npmw` instead of the traditional `npm` you can configure a Node-less environment to develop or test your application.
- `/src/main/docker` - Docker configurations for the application and services that the application depends on

## Development

Before you can build this project, you must install and configure the following dependencies on your machine:

1. [Node.js](https://nodejs.org/): We use Node to run a development web server and build the project.
   Depending on your system, you can install Node either from source or as a pre-packaged bundle.

After installing Node, you should be able to run the following command to install development tools.
You will only need to run this command when dependencies change in [package.json](package.json).

```
npm install
```

We use npm scripts and [Webpack][] as our build system.

Run the following commands in two separate terminals to create a blissful development experience where your browser
auto-refreshes when files change on your hard drive.

```
./gradlew -x webapp
npm start
```

Npm is also used to manage CSS and JavaScript dependencies used in this application. You can upgrade dependencies by
specifying a newer version in [package.json](package.json). You can also run `npm update` and `npm install` to manage dependencies.
Add the `help` flag on any command to see how you can use it. For example, `npm help update`.

The `npm run` command will list all of the scripts available to run for this project.

### PWA Support

JHipster ships with PWA (Progressive Web App) support, and it's turned off by default. One of the main components of a PWA is a service worker.

The service worker initialization code is commented out by default. To enable it, uncomment the following code in `src/main/webapp/index.html`:

```html
<script>
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./service-worker.js').then(function () {
      console.log('Service Worker Registered');
    });
  }
</script>
```

Note: [Workbox](https://developers.google.com/web/tools/workbox/) powers JHipster's service worker. It dynamically generates the `service-worker.js` file.

### Managing dependencies

For example, to add [Leaflet][] library as a runtime dependency of your application, you would run following command:

```
npm install --save --save-exact leaflet
```

To benefit from TypeScript type definitions from [DefinitelyTyped][] repository in development, you would run following command:

```
npm install --save-dev --save-exact @types/leaflet
```

Then you would import the JS and CSS files specified in library's installation instructions so that [Webpack][] knows about them:
Note: There are still a few other things remaining to do for Leaflet that we won't detail here.

For further instructions on how to develop with JHipster, have a look at [Using JHipster in development][].

### Doing API-First development using openapi-generator-cli

[OpenAPI-Generator]() is configured for this application. You can generate API code from the `src/main/resources/swagger/api.yml` definition file by running:

```bash
./gradlew openApiGenerate
```

Then implements the generated delegate classes with `@Service` classes.

To edit the `api.yml` definition file, you can use a tool such as [Swagger-Editor](). Start a local instance of the swagger-editor using docker by running: `docker compose -f src/main/docker/swagger-editor.yml up -d`. The editor will then be reachable at [http://localhost:7742](http://localhost:7742).

Refer to [Doing API-First development][] for more details.

## Building for production

### Packaging as jar

To build the final jar and optimize the SubredditsAdmin application for production, run:

```
./gradlew -Pprod clean bootJar
```

This will concatenate and minify the client CSS and JavaScript files. It will also modify `index.html` so it references these new files.
To ensure everything worked, run:

```
java -jar build/libs/*.jar
```

Then navigate to [http://localhost:8080](http://localhost:8080) in your browser.

Refer to [Using JHipster in production][] for more details.

### Packaging as war

To package your application as a war in order to deploy it to an application server, run:

```
./gradlew -Pprod -Pwar clean bootWar
```

### JHipster Control Center

JHipster Control Center can help you manage and control your application(s). You can start a local control center server (accessible on http://localhost:7419) with:

```
docker compose -f src/main/docker/jhipster-control-center.yml up
```

## Testing

### Client tests

Unit tests are run by [Jest][]. They're located in [src/test/javascript/](src/test/javascript/) and can be run with:

```
npm test
```

### Spring Boot tests

To launch your application's tests, run:

```
./gradlew test integrationTest jacocoTestReport
```

## Others

### Code quality using Sonar

Sonar is used to analyse code quality. You can start a local Sonar server (accessible on http://localhost:9001) with:

```
docker compose -f src/main/docker/sonar.yml up -d
```

Note: we have turned off forced authentication redirect for UI in [src/main/docker/sonar.yml](src/main/docker/sonar.yml) for out of the box experience while trying out SonarQube, for real use cases turn it back on.

You can run a Sonar analysis with using the [sonar-scanner](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner) or by using the gradle plugin.

Then, run a Sonar analysis:

```
./gradlew -Pprod clean check jacocoTestReport sonarqube -Dsonar.login=admin -Dsonar.password=admin
```

Additionally, Instead of passing `sonar.password` and `sonar.login` as CLI arguments, these parameters can be configured from [sonar-project.properties](sonar-project.properties) as shown below:

```
sonar.login=admin
sonar.password=admin
```

For more information, refer to the [Code quality page][].

### Using Docker to simplify development (optional)

You can use Docker to improve your JHipster development experience. A number of docker-compose configuration are available in the [src/main/docker](src/main/docker) folder to launch required third party services.

For example, to start a postgresql database in a docker container, run:

```
docker compose -f src/main/docker/postgresql.yml up -d
```

To stop it and remove the container, run:

```
docker compose -f src/main/docker/postgresql.yml down
```

You can also fully dockerize your application and all the services that it depends on.
To achieve this, first build a docker image of your app by running:

```
npm run java:docker
```

Or build a arm64 docker image when using an arm64 processor os like MacOS with M1 processor family running:

```
npm run java:docker:arm64
```

Then run:

```
docker compose -f src/main/docker/app.yml up -d
```

When running Docker Desktop on MacOS Big Sur or later, consider enabling experimental `Use the new Virtualization framework` for better processing performance ([disk access performance is worse](https://github.com/docker/roadmap/issues/7)).

For more information refer to [Using Docker and Docker-Compose][], this page also contains information on the docker-compose sub-generator (`jhipster docker-compose`), which is able to generate docker configurations for one or several JHipster applications.

## Continuous Integration (optional)

To configure CI for your project, run the ci-cd sub-generator (`jhipster ci-cd`), this will let you generate configuration files for a number of Continuous Integration systems. Consult the [Setting up Continuous Integration][] page for more information.

[JHipster Homepage and latest documentation]: https://www.jhipster.tech
[JHipster 8.4.0 archive]: https://www.jhipster.tech/documentation-archive/v8.4.0
[Using JHipster in development]: https://www.jhipster.tech/documentation-archive/v8.4.0/development/
[Using Docker and Docker-Compose]: https://www.jhipster.tech/documentation-archive/v8.4.0/docker-compose
[Using JHipster in production]: https://www.jhipster.tech/documentation-archive/v8.4.0/production/
[Running tests page]: https://www.jhipster.tech/documentation-archive/v8.4.0/running-tests/
[Code quality page]: https://www.jhipster.tech/documentation-archive/v8.4.0/code-quality/
[Setting up Continuous Integration]: https://www.jhipster.tech/documentation-archive/v8.4.0/setting-up-ci/
[Node.js]: https://nodejs.org/
[NPM]: https://www.npmjs.com/
[Webpack]: https://webpack.github.io/
[BrowserSync]: https://www.browsersync.io/
[Jest]: https://facebook.github.io/jest/
[Leaflet]: https://leafletjs.com/
[DefinitelyTyped]: https://definitelytyped.org/
[OpenAPI-Generator]: https://openapi-generator.tech
[Swagger-Editor]: https://editor.swagger.io
[Doing API-First development]: https://www.jhipster.tech/documentation-archive/v8.4.0/doing-api-first-development/
