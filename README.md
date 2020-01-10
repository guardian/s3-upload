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


### Adding a new service
If you want another service or app to access the endpoints in s3-uploader (allowing it to upload to s3), there are a 
couple of steps to follow:
* Add an new Config object for your service in `Config.scala`. Some other configuration is in the Cloudformation stack 
in editorial-tools-platform
* Add the hostname of the new service to the 'Allowed Origins' in `application.conf`. Locally this file is in the repo.
For CODE and PROD, it is in S3 in the `media-service` account
* If required, add a new route and method to handle the new request
* Cross account access etc can be configured at the bucket policy level in S3
  
