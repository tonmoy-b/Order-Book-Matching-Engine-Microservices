#!/bin/bash
set -e


DB_EXISTS=$(psql -U "$POSTGRES_USER" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='keycloak'")

if [ "$DB_EXISTS" != "1" ]; then
    echo "Creating keycloak database..."
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
        CREATE DATABASE keycloak;
        GRANT ALL PRIVILEGES ON DATABASE keycloak TO "$POSTGRES_USER";
EOSQL
    echo "Keycloak database created successfully."
else
    echo "Database 'keycloak' already exists. Skipping creation."
fi