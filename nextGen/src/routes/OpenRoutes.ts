import {Router} from 'express';
import {
  AUTH_URL,
  CLIENT_ID_UI, ISSUER,
  LOGOUT_URL,
  OPENID_PROVIDER_UI,
  PROVIDER,
  TOKEN_URL,
  USER_INFO_URL
} from '../ConfigurationENV';

const openRoutes = Router();

export const APPLICATION_JSON = 'application/json';

openRoutes.get('/time', (_, res) => {
  res.contentType(APPLICATION_JSON)
    .send({
      currentTime: new Date().valueOf(),
    });
});

openRoutes.get('/configurations', (_, res) => {
  res.contentType(APPLICATION_JSON)
    .send({
      clientID: CLIENT_ID_UI,
      authorizationEndpoint: AUTH_URL,
      logoutEndpoint: LOGOUT_URL,
      userInfoEndpoint: USER_INFO_URL,
      tokenEndpoint: TOKEN_URL,
      openIDConnectURI: OPENID_PROVIDER_UI,
      provider: PROVIDER,
      issuer: ISSUER,
    });
});

export default openRoutes;
