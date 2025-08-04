#!/usr/bin/env bash
#
# Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
# This product includes software developed at Datadog (https://www.datadoghq.com/).
# Copyright 2016-Present Datadog, Inc.
#

set -o pipefail

tag_name=$(curl -s -H "Authorization: Bearer $GITHUB_TOKEN" \
     -H "Accept: application/vnd.github+json" \
     https://api.github.com/repos/DataDog/dd-sdk-android-gradle-plugin/releases/latest | jq -r .tag_name)

check_release_existence() {
  local url=$1
  local repo_name=$2

  status_code=$(curl -s -o /dev/null -w "%{http_code}" "$url")

  if [ "$status_code" -eq 200 ]; then
    echo "Release $tag_name exists in $repo_name"
  else
    echo "Release $tag_name doesn't exist in $repo_name"
  fi
}

check_release_existence "https://plugins.gradle.org/plugin/com.datadoghq.dd-sdk-android-gradle-plugin-local/$tag_name" "Gradle Portal"
check_release_existence "https://repo1.maven.org/maven2/com/datadoghq/dd-sdk-android-gradle-plugin/$tag_name/dd-sdk-android-gradle-plugin-$tag_name.jar" "Maven Central"
