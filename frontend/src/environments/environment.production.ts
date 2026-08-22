export const environment = {
  production: true,
  apiBaseUrl: '/api',
  appVersion: '0.1.0',
  appName: 'Lease Demo',
  keycloak: {
    // TODO: replace with the production Keycloak URL before deployment
    url: 'http://localhost:8081',
    realm: 'lease-demo',
    clientId: 'lease-demo-web',
  },
};
