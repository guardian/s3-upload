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

### Smoke tests

The core functionality of this app is the ability to upload files to S3.

To test:
- Open incognito tab
- Check that you are required to login to https://s3-uploader.gutools.co.uk
- Attempt to upload a small test image (that you don't mind becoming public)
- Check that the test image is accessible using the link provided after upload

### Adding a new service
If you want another service or app to access the endpoints in s3-uploader (allowing it to upload to s3), there are a 
couple of steps to follow:
* Add an new Config object for your service in `Config.scala`
* Amend the s3-uploader [BatchUploaderPolicy](https://github.com/guardian/editorial-tools-platform/blob/master/cloudformation/media-service%20account/s3-uploader.yaml#L75) (CloudFormation) to allow the s3-uploader to get objects from and put objects to the desired bucket (these changes have to be applied manually via CloudFormation console)
* Add the hostname of the new service to the 'Allowed Origins' in `application.conf`. Locally this file is in the repo.
For CODE and PROD, it is in S3 in the `media-service` account
* If required, add a new route and method to handle the new request.
* Cross account access etc can be configured at the bucket policy level in S3
  

### Uploading interactives

The authenticated interactive uploader is available at `/interactive`, including
`https://s3-uploader.code.dev-gutools.co.uk/interactive` on CODE. It uses the same
`s3_uploader_access` permission as the existing uploader.

Upload a ZIP with `index.html` at its root and its assets in their original folders.
The result includes a public entry-point URL, embed code, file links, and mobile
and desktop previews. Each upload receives a new UUID; existing uploads are not
replaced. The entry point is published after its assets. Mac ZIP metadata is
excluded. ZIPs are limited to the configured request limit (50 MB by default),
2,000 entries, 50 MB per expanded file, and 200 MB total expanded content.

Deployment properties in `/etc/gu/s3-uploader.properties`:

| Property | Default |
| --- | --- |
| `s3.interactiveBucket` | Charts bucket (`s3.chartBucket`, otherwise `gdn-cdn`) |
| `aws.interactiveRegion` | Charts region (`aws.chartRegion`, otherwise `us-east-1`) |
| `interactiveBaseUrl` | `https://interactive.guim.co.uk` for `gdn-cdn`; otherwise the regional S3 URL |
| `interactivePrefix` | `uploader/embed` on PROD; `uploader/code/embed` on CODE/DEV |

For CODE, confirm the bucket, URL and prefix before deployment. The default CODE
prefix is separate from PROD but its contents are still public. Results point
explicitly to `index.html`, so no directory-index CDN behaviour is required.

The application's IAM role and the destination bucket policy must permit
`s3:PutObject`, `s3:PutObjectAcl` (uploads use `public-read`, matching Charts), and
`s3:DeleteObject` for the interactive prefix. Deletion is used to clean up a
partially uploaded bundle after failure. Check cross-account bucket permissions
as well as CDN access to the prefix. These AWS policies are managed outside this
repository. No extra CORS origin is needed for the same-origin upload form.

Before deploying to CODE, run `sbt test` and `sbt 'Debian / packageBin'`. Smoke-test
an authenticated upload containing HTML, JS, CSS and a nested image; check the
returned public URL and both previews, and verify that an archive without a root
`index.html` is rejected.
