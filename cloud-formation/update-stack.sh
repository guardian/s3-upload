#!/usr/bin/env bash

if [ $# -lt 1 ]
then
    echo 'usage: update-stack <STACK_NAME>'
    exit 1
fi

STACK_NAME=$1

aws cloudformation list-stack-resources --stack-name $STACK_NAME > /dev/null 2>&1

if [ $? -ne 0 ]
then
    echo "Stack $STACK_NAME doesn't exist. Aborting."
    exit 1
fi

if [[ $STACK_NAME == *"DEV"* ]]
then
    ./generate-template s3-uploader-DEV.yaml > /dev/null 2>&1

    TEMPLATE_FILE=s3-uploader-DEV.json

    aws cloudformation update-stack \
        --capabilities CAPABILITY_IAM \
        --stack-name $STACK_NAME \
        --template-body file://$TEMPLATE_FILE

    rm $TEMPLATE_FILE

else
    ./generate-template s3-uploader.yaml > /dev/null 2>&1

    TEMPLATE_FILE=s3-uploader.json

    aws cloudformation update-stack \
        --capabilities CAPABILITY_IAM \
        --stack-name $STACK_NAME \
        --template-body file://$TEMPLATE_FILE \
        --parameters ParameterKey=SSLCertificateId,UsePreviousValue=true \
                     ParameterKey=Stage,UsePreviousValue=true \
                     ParameterKey=KeyName,UsePreviousValue=true \
                     ParameterKey=DomainRoot,UsePreviousValue=true \
                     ParameterKey=PlayApplicationSecret,UsePreviousValue=true

    rm $TEMPLATE_FILE
fi
