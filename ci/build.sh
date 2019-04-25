#!/usr/bin/env bash
set -e

pushd ../public/js
rm -rf node_modules
npm install
popd

pushd ..
sbt -J-Xmx1024m \
    -J-XX:MaxPermSize=256m \
    -J-XX:ReservedCodeCacheSize=128m \
    -J-XX:+CMSClassUnloadingEnabled \
    -J-Dsbt.log.noformat=true \
    clean riffRaffUpload
popd
