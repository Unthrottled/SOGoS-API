#!/usr/bin/env bash
openssl genrsa -out rootCA.key 2048
openssl genrsa -des3 -out rootCA.key 2048
openssl req -x509 -new -nodes -key rootCA.key -sha256 -days 1024 -out rootCA.pem

cp rootCA.key ../mongo/
cp rootCA.pem ../mongo/
