import {CONFIG} from './config/config';

export const CONNECTION_STRING = process.env.SOGOS_MONGO_CONNECTION || CONFIG.memory.connectionString;
export const LOGGING_LEVEL = process.env.SOGOS_LOGGING_LEVEL || 'info';
export const HMAC_KEY = process.env.SOGOS_HMAC_KEY;

if (!HMAC_KEY) {
  throw Error('Required "SOGOS_HMAC_KEY" to be set as an environment variable');
}

export const CLIENT_ID_UI = process.env.SOGOS_CLIENT_ID_UI || CONFIG.security['App-Client-Id'];
export const OPENID_PROVIDER_UI = process.env.SOGOS_OPENID_PROVIDER_UI || CONFIG.security['OpenId-Connect-Provider'];
export const PROVIDER = process.env.SOGOS_PROVIDER || CONFIG.security.provider;
export const ISSUER = process.env.SOGOS_ISSUER || CONFIG.security.issuer;
export const AUTH_URL = process.env.SOGOS_AUTH_URL || CONFIG.security['auth-url'];
export const JKWS_URL = process.env.SOGOS_JKWS_URL || CONFIG.security['jwks-url'];
export const TOKEN_URL = process.env.SOGOS_TOKEN_URL || CONFIG.security['token-url'];
export const USER_INFO_URL = process.env.SOGOS_USER_INFO_URL || CONFIG.security['user-info-url'];
export const LOGOUT_URL = process.env.SOGOS_LOGOUT_URL || CONFIG.security['logout-url'];
export const CORS_ORIGIN_URL = process.env.SOGOS_ALLOWED_ORIGIN || CONFIG.security['allowed-origin'];
