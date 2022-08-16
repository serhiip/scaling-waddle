#!/bin/bash

set -eux

# gcloud auth login --update-adc --no-launch-browser
# gcloud config set project

DEFAULT_SERVICE_ACCOUNT=$(gcloud iam service-accounts list --filter=compute | awk '{print $6}' | xargs)
ZONES=["\"europe-west1-b"\"]
REGION="europe-west1"
PROJECT=$(gcloud config get-value project)
AVRO_PROJECT_NAME='avro-publisher'
DATA_PRODUCER_PROJECT_NAME='car-data-producer'
DATA_CONSUMER_PROJECT_NAME='car-data-consumer'
NOTIFIER_PROJECT_NAME='driver-notifier'

gcloud services enable \
       container.googleapis.com \
       servicenetworking.googleapis.com \
       cloudbuild.googleapis.com

pushd ../../../
DOCKERFILE_PATH=$(sbt "; project avro; docker:stage; docker:stagingDirectory" | tail -n 1 | sed 's/\[info\] //')
pushd $DOCKERFILE_PATH
gcloud builds submit --tag=gcr.io/$PROJECT/$AVRO_PROJECT_NAME --async
popd

DOCKERFILE_PATH=$(sbt "; project carDataProducer; docker:stage; docker:stagingDirectory" | tail -n 1 | sed 's/\[info\] //')
pushd $DOCKERFILE_PATH
gcloud builds submit --tag=gcr.io/$PROJECT/$DATA_PRODUCER_PROJECT_NAME --async
popd

DOCKERFILE_PATH=$(sbt "; project carDataConsumer; docker:stage; docker:stagingDirectory" | tail -n 1 | sed 's/\[info\] //')
pushd $DOCKERFILE_PATH
gcloud builds submit --tag=gcr.io/$PROJECT/$DATA_CONSUMER_PROJECT_NAME --async
popd

DOCKERFILE_PATH=$(sbt "; project driverNotifier; docker:stage; docker:stagingDirectory" | tail -n 1 | sed 's/\[info\] //')
pushd $DOCKERFILE_PATH
gcloud builds submit --tag=gcr.io/$PROJECT/$NOTIFIER_PROJECT_NAME --async
popd

popd

gcloud builds list
gcloud container images list

terraform init

terraform apply \
          -var="project_id=$PROJECT" \
          -var="region=$REGION" \
          -var="zones=$ZONES" \
          -var="service_account=$DEFAULT_SERVICE_ACCOUNT" \
          -var="avro_publisher_image_name=$AVRO_PROJECT_NAME" \
          -var="data_producer_image_name=$DATA_PRODUCER_PROJECT_NAME" \
          -var="data_consumer_image_name=$DATA_CONSUMER_PROJECT_NAME" \
          -var="notifier_image_name=$NOTIFIER_PROJECT_NAME"

gcloud container clusters get-credentials kafka-tests --region=$REGION
