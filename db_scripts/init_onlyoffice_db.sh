#!/bin/sh

psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "create user office_user with password 'onlyoffice'"
psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "create database onlyoffice"
psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "grant all privileges on database onlyoffice to office_user"

psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "create user nextcloud_user with password 'nextcloud'"
psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "create database nextcloud"
psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "grant all privileges on database nextcloud to nextcloud_user"

psql -U "$POSTGRES_USER" -d onlyoffice -c "grant all on schema public to office_user"
psql -U "$POSTGRES_USER" -d nextcloud -c "grant all on schema public to nextcloud_user"