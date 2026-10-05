/** Environnement de développement (`ng serve`). Remplacé par `environment.prod.ts` en build de production. */
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081/api',
  keycloak: {
    issuer: 'http://localhost:8080/realms/gpdi',
    clientId: 'gpdi-client-id',
  },
};
