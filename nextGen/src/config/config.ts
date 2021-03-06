export const CONFIG = {
  security: {
    'allowed-origin': 'http://172.17.0.1:3000',
    'OpenId-Connect-Provider': 'http://172.17.0.1:8080/auth/realms/master',
    'auth-url': 'http://172.17.0.1:8080/auth/realms/master/protocol/openid-connect/auth',
    'token-url': 'http://172.17.0.1:8080/auth/realms/master/protocol/openid-connect/token',
    'jwks-url': 'http://172.17.0.1:8080/auth/realms/master/protocol/openid-connect/certs',
    'user-info-url': 'http://172.17.0.1:8080/auth/realms/master/protocol/openid-connect/userinfo',
    'logout-url': 'http://172.17.0.1:8080/auth/realms/master/protocol/openid-connect/logout',
    'issuer': 'http://172.17.0.1:8080/auth/realms/master',
    'appIssuer': 'http://172.17.0.1:8080/auth/realms/master',
    'provider': 'KEYCLOAK',
    'Client-Id': 'sogos',
    'Web-App-Client-Id': 'sogos-app',
    'Native-App-Client-Id': 'sogos-app',
  },
  memory: {
    host: '127.0.0.1',
    port: 27017,
    maxPoolSize: 30,
    ssl: false,
    connectionString: 'mongodb://localhost:27017',
  },
};
