#!/usr/bin/env bash

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
    clean compile riffRaffNotifyTeamcity
popd
