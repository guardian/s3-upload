# S3 Uploader

A small Play app to upload files to and S3 bucket and get a CDN URL in return.

## Development

Requires the [login service](https://github.com/guardian/login.gutools), so clone that repo and set it up.

You will also need media-service Janus credentials.

```
sbt run
``` 

### App
To deploy the app, deploy the project `media-service::teamcity::s3-uploader` in riffraff.
