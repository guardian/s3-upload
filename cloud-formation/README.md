# Cloud Formation

To generate a JSON template, run:

```sh
./generate-template <FILE>
```

For example, to create a template for DEV, run:

```sh
./generate-template s3-uploader-DEV.yaml
```

This will create a file `s3-uploader-DEV.json` which can then be used to create a Cloud Formation Stack.
