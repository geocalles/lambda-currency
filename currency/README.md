## FBO Multi-currency Lambdas

__Prerequisites__

- Java 11
- docker compose
- AWS 
  - [Install the IntelliJ AWS toolkit Plugin](https://aws.amazon.com/intellij/)
  - [Install the SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)


__Functions__

- Updater
  - Function that use cloudwatch event to update the redis entry with coutries and currencies based on ISO 3166 and ISO 4217.

- Getter
  - Function using APIGateway/HTTPApi to return list of countries/country by code and currencies/currencies by code.

__Starting development environment__

#### Open the Function 

- open the project function, i.e `updater` or `getter` in IntelliJ (don't use the root directory `currency`).
- change `REDIS_HOST` to your correct IP (can not use localhost because it is running inside of docker)
- on `template.yaml` click at the function name as "`UpdaterFunction`" and modify the configurations:
  - Configuration:
    - Set `from template` and select the local file `template.yaml`
    - Set Java `runtime 11`
  - Input
    - `Event template` - Select the correct event from `events` folder
- Run/Debug using the IntelliJ

__Troubleshooting__

- AWS Connection you can setup the aws credentials or use the localstack
- Remove local folder `.aws-sam` and try to run again
- Force a rebuild on docker-image
