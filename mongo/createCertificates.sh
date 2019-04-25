#!/usr/bin/env bash
openssl genrsa -out mongodb.key 2048
openssl req -new -key mongodb.key -out mongodb.csr


# To get the trusted root certificate just run the createRootCertificate.sh
# in the scripts directory and copy it into here
openssl x509 -req -in mongodb.csr -CA rootCA.pem -CAkey rootCA.key -CAcreateserial -out mongodb.crt -days 500 -sha256

cat mongodb.key mongodb.crt > mongodb.pem
