FROM mongo:latest

ADD mongod.yml /etc/mongod.conf

RUN mkdir -p /etc/ssl
ADD mongodb.pem /etc/ssl/


ENTRYPOINT ["mongod", "--config", "/etc/mongod.conf"]