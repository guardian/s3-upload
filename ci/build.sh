#!/usr/bin/env bash

# NB we use `riffRaffUpload` over `riffRaffNotifyTeamcity` because we want to specify the project name

pushd ../public/js
rm -rf node_modules
npm install
popd

pushd ..
java -Xmx1024m \
    -XX:MaxPermSize=256m \
    -XX:ReservedCodeCacheSize=128m \
    -XX:+CMSClassUnloadingEnabled \
    -Dsbt.log.noformat=true \
    -jar sbt-launch.jar \
    clean compile riffRaffUpload
popd
