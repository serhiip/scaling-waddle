#!/bin/bash

set -eux

DEFAULT_SERVICE_ACCOUNT=$(gcloud iam service-accounts list --filter=compute | awk '{print $6}' | xargs)
ZONES=["\"europe-west1-b"\"]
REGION="europe-west1"
PROJECT=$(gcloud config get-value project)

terraform init

terraform apply -target=./kafka-services.tf \
          -var="project_id=$PROJECT" \
          -var="region=$REGION" \
          -var="zones=$ZONES" \
          -var="service_account=$DEFAULT_SERVICE_ACCOUNT" \
