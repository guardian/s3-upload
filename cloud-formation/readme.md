# Cloud Formation

We do not need this folder. It was removed on this PR:
https://github.com/guardian/s3-upload/pull/18/changes#diff-c9e974fec616d15af7431eacfe54d844c2a397b86c916852e835b58b54e26186

However, the old requirements.txt contained a package that not has a critical Python vulnerability. Because dependabot can no longer find the file, it seems to assume the last copy it has still applies and flags the project has having the vulnerability, despite no longer using Python:
https://github.com/guardian/s3-upload/actions/runs/28558339606/job/84670584829

Adding a blank file to in the hope that this will stop the false flag resurfacing.

