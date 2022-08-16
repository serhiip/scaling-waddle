#!/bin/bash

set -eux

# gcloud auth login --update-adc --no-launch-browser
# gcloud config set project

DEFAULT_SERVICE_ACCOUNT=$(gcloud iam service-accounts list --filter=compute | awk '{print $6}' | xargs)
ZONES=["\"europe-west1-b"\"]
REGION="europe-west1"
PROJECT=$(gcloud config get-value project)

gcloud services enable \
       container.googleapis.com \
       servicenetworking.googleapis.com \
       cloudbuild.googleapis.com

terraform init

terraform apply \
          -var="project_id=$PROJECT" \
          -var="region=$REGION" \
          -var="zones=$ZONES" \
          -var="service_account=$DEFAULT_SERVICE_ACCOUNT"

gcloud container clusters get-credentials kafka-tests --region=$REGION

gcloud container images list

pushd ../../../
DOCKERFILE_PATH=$(sbt "; project avro; docker:stage; docker:stagingDirectory" | tail -n 1 | sed 's/\[info\] //')
pushd $DOCKERFILE_PATH
gcloud builds submit --tag=gcr.io/$PROJECT/avro-publisher --async
popd

DOCKERFILE_PATH=$(sbt "; project carDataProducer; docker:stage; docker:stagingDirectory" | tail -n 1 | sed 's/\[info\] //')
pushd $DOCKERFILE_PATH
gcloud builds submit --tag=gcr.io/$PROJECT/car-data-producer --async
popd

DOCKERFILE_PATH=$(sbt "; project carDataConsumer; docker:stage; docker:stagingDirectory" | tail -n 1 | sed 's/\[info\] //')
pushd $DOCKERFILE_PATH
gcloud builds submit --tag=gcr.io/$PROJECT/car-data-consumer --async
popd

DOCKERFILE_PATH=$(sbt "; project driverNotifier; docker:stage; docker:stagingDirectory" | tail -n 1 | sed 's/\[info\] //')
pushd $DOCKERFILE_PATH
gcloud builds submit --tag=gcr.io/$PROJECT/driver-notifier --async
popd

popd

gcloud builds list
