#!/bin/sh
set -e

echo "Waiting for Nextcloud to initialize..."
until docker exec --user www-data nextcloud php occ status 2>/dev/null | grep -q "installed: true"; do
  printf "."
  sleep 5
done
echo " ready."

echo "Installing OnlyOffice connector..."
docker exec --user www-data nextcloud php occ app:install onlyoffice || true

echo "Configuring OnlyOffice connector..."
docker exec --user www-data nextcloud php occ config:app:set onlyoffice DocumentServerUrl \
  --value="http://localhost/office/"

docker exec --user www-data nextcloud php occ config:app:set onlyoffice DocumentServerInternalUrl \
  --value="http://onlyoffice/"

docker exec --user www-data nextcloud php occ config:app:set onlyoffice StorageUrl \
  --value="http://nginx/cloud/"

docker exec --user www-data nextcloud php occ config:app:set onlyoffice verify_peer_off \
  --value="true"

echo "Enabling CSV format for OnlyOffice..."
docker exec --user www-data nextcloud php occ config:app:set onlyoffice defFormats \
  --value='{"csv":true}'
docker exec --user www-data nextcloud php occ config:app:set onlyoffice editFormats \
  --value='{"csv":true}'

echo "Done. Open http://localhost/cloud/ and log in as admin/admin."
