# S3 Uploader

A small Play app to upload files to and S3 bucket and get a CDN URL in return.

## Development

Requires the [login service](https://github.com/guardian/login.gutools), so clone that repo and set it up.

### CloudFormation
To create a DEV stack you need to first generate a json template from the yaml:

```sh
cd cloud-formation
./generate-template s3-uploader-DEV.yaml
```

This will create a file `cloud-formation/s3-uploader-DEV.json` which can be used to create a stack in the AWS console.


### Running the app
The app can be run with:

```sh
./sbt run
```

## Deploying

### CloudFormation
CloudFormation deploys are manual and not performed through riffraff.

To update a CODE or PROD stack, run:

```sh
cd cloud-formation
./update-stack.sh s3-uploader-CODE|PROD
```

### App
To deploy the app, deploy the project `media-service::teamcity::s3-uploader` in riffraff.
