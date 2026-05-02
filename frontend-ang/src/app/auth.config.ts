import { AuthConfig } from 'angular-oauth2-oidc';

export const authConfig: AuthConfig = {

  // Url of the keycloak server
  issuer: 'http://localhost:8081/realms/test',

  // Url for the redirect after successful login
  redirectUri: window.location.origin + '/experiments',

  // ID of the client for keycloak
  clientId: 'angular-frontend-client',

  responseType: 'code',

  // The data we want to get form keycloak (openid for login, profile for names, offline_access for JWT token)
  //TODO: Do we need email?
  scope: 'openid profile email offline_access',

  //TODO: Set to false in Production
  showDebugInformation: true,
};
