#!/bin/bash

# Utility to upload release to Eclipse P2 repository on Bintray

set -euo pipefail
cd "$(dirname "$0")"

BINTRAY_USER=ddekany
BINTRAY_OWNER=freemarker
BINTRAY_REPO=freemarker-ide
BINTRAY_PACKAGE=p2-repository

echo "This will upload/replace Bintray package/version independent files to ${BINTRAY_OWNER}/${BINTRAY_REPO}"
echo "Enter version:"
read version

rm -rf repo-template-out
mkdir repo-template-out
timestamp=$(date +%s)000
for f in repo-template/*; do
  cat "$f" | sed -E 's/\$\{version}/'$version'/g' | sed -E 's/\$\{timestamp}/'$timestamp'/g' > "repo-template-out/${f#*/}"
done

echo "Enter Bintray API key:"
read -s BINTRAY_API_KEY

# Upload version independent meta-data:
for f in repo-template-out/*; do
  relativeDownloadPath="${f#*/}"
  echo "Uploading $relativeDownloadPath..."
  curl -X PUT -T "$f" \
   -u "$BINTRAY_USER:$BINTRAY_API_KEY" -f \
   "https://api.bintray.com/content/$BINTRAY_OWNER/$BINTRAY_REPO/$relativeDownloadPath;publish=1;override=1"
  echo
done
rm -rf repo-template-out

# Upload version dependent files:
siteInputDir=../site/target/repository
for f in $(cd $siteInputDir; find * -type f); do
  relativeDownloadPath=releases/$version/$f
  echo "Uploading $relativeDownloadPath..."
  curl -X PUT -T "$siteInputDir/$f" \
   -u "$BINTRAY_USER:$BINTRAY_API_KEY" -f \
   "https://api.bintray.com/content/$BINTRAY_OWNER/$BINTRAY_REPO/$relativeDownloadPath;bt_package=$BINTRAY_PACKAGE;bt_version=$version;publish=1;override=1"
  echo
done
