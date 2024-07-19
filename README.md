# S3 Uploader

A small Play app to upload files to and S3 bucket and get a CDN URL in return.

## Development

Requires the [login service](https://github.com/guardian/login.gutools), so clone that repo and set it up.

You will also need media-service Janus credentials.

```
scripts/setup
sbt run
``` 

The app should now be available at https://s3-uploader.local.dev-gutools.co.uk/

### Deployment
The app is built using GitHub Actions, with CD (via Riff-Raff) enabled for changes on `main`.

The Riff-Raff project is `media-service::s3-uploader`.

### Adding a new service
If you want another service or app to access the endpoints in s3-uploader (allowing it to upload to s3), there are a 
couple of steps to follow:
* Add an new Config object for your service in `Config.scala`
* Amend the s3-uploader [BatchUploaderPolicy](https://github.com/guardian/editorial-tools-platform/blob/master/cloudformation/media-service%20account/s3-uploader.yaml#L75) (CloudFormation) to allow the s3-uploader to get objects from and put objects to the desired bucket (these changes have to be applied manually via CloudFormation console)
* Add the hostname of the new service to the 'Allowed Origins' in `application.conf`. Locally this file is in the repo.
For CODE and PROD, it is in S3 in the `media-service` account
* If required, add a new route and method to handle the new request.
* Cross account access etc can be configured at the bucket policy level in S3
  
