/** Environnement de production. Valeurs à ajuster selon le déploiement DSI (à ne jamais committer avec des secrets). */
export const environment = {
  production: true,
  apiUrl: '/api',
  keycloak: {
    issuer: 'https://TODO-keycloak-prod.mef.gov.mg/realms/gpdi',
    clientId: 'gpdi-client-id',
  },
};
