# GPDI Backend

Backend Spring Boot du projet GPDI (gestion de projets : équipes, projets, sprints, jalons, livrables, tâches,
partage client, gestion des risques assistée par IA).

## Socle technique

- Java 8
- Spring Boot 2.7.18
- Spring Data JPA (Hibernate)
- PostgreSQL (types ENUM natifs pour les statuts, via `hibernate-types-55`)
- Keycloak (OAuth2 / OpenID Connect, resource server)
- MapStruct (mapping Entity <-> DTO)
- springdoc-openapi (Swagger)
- Ollama + modèle `qwen3:8b` (module IA, exécuté en local, aucune donnée ne quitte le réseau)

## Architecture

Le projet suit l'architecture en couches `Controller -> Service -> Repository` imposée par le guide backend DSI MEF (`config/`, `controller/`, `dto/{request,response}/`, `entity/`, `repository/`, `service/{,impl/}`, `mapper/`, `exception/`, `security/`). 

Les entités JPA ne sont jamais exposées directement :
les DTO sont convertis par MapStruct. Toute nouvelle fonctionnalité doit suivre ce même pattern.

## Modules fonctionnels

- Role :Rôles applicatifs (référencés par `Utilisateur`)
- Utilisateur: Comptes (matricule, mot de passe haché, rôle)
- Équipe: Équipes avec chef d'équipe et membres (`membre_equipes`)
- Projet: Projets, responsable (`chef_projet`), équipe assignée
- Sprint: Sprints d'un projet, cycle de vie `PLANIFIE -> EN_COURS -> CLOTURER`
- Jalon: Jalons d'un projet, cycle de vie `A_VENIR -> EN_RETARD -> ATTEINT` / `ANNULER`
- Livrable: Livrables d'un jalon, cycle de vie `ATTENDU -> LIVRE` / `REJETE`
- Tâche: Tâches d'un projet (backlog ou sprint), avec sous-tâches, commentaires, dépendances, assignations
- Partage client: Partage de projets avec des comptes `direction_metier`, avec échéance
- Notification: Notifications personnelles (lu / non lu) générées par les événements du backend
- Risque (IA): Détection automatique des risques d'un projet par l'assistant IA, solutions proposées, application sur le projet ou rejet (voir section Risque)
- Point d'information: Points d'information publiés par le chef de projet à destination des comptes clients partagés (voir section dédiée)
- Assistant IA: Chat en langage libre sur le contexte d'un projet, réservé à son chef de projet (voir section dédiée)
- Recommandation de pilotage: Recommandations proactives (réaffectation, retrait du sprint, ajustement d'estimation) proposées par l'IA sur un projet (voir section dédiée)
- Habilitation: Matrice des droits (lecture/écriture) par rôle métier et par fonction, éditable par un administrateur et appliquée dynamiquement (voir section Sécurité)

## Configuration locale

Les variables suivantes peuvent être définies pour PostgreSQL :

- `DB_URL` (défaut : `jdbc:postgresql://localhost:5432/gpdi`)
- `DB_USERNAME` (défaut : `postgres`)
- `DB_PASSWORD`
- `SERVER_PORT` (défaut : `8081`)

Le schéma (tables + types ENUM natifs) est géré manuellement (`spring.jpa.hibernate.ddl-auto=none`), pas par
Hibernate. Voir la section Base de données ci-dessous pour la liste des types ENUM attendus.

### Ollama (module IA)

Le module IA interroge un serveur [Ollama](https://ollama.com) local. Prérequis : Ollama installé et le modèle
téléchargé (`ollama pull qwen3:8b`). 
Variables (toutes facultatives) :

- `OLLAMA_BASE_URL` - `http://localhost:11434`- Adresse du serveur Ollama
- `OLLAMA_MODEL` - `qwen3:8b` - Modèle utilisé 
- `OLLAMA_TIMEOUT_SECONDS` - `300` - Délai maximal d'un appel (le premier appel à froid dure ~2-3 min) |
- `OLLAMA_KEEP_ALIVE` - `30m` - Durée pendant laquelle Ollama garde le modèle en mémoire après un appel |
- `OLLAMA_NUM_CTX` - `8192` - Taille de la fenêtre de contexte (en jetons) |
- `IA_RISQUE_CRON` - `0 0 2 * * *` - Détection automatique des risques (cron Spring à 6 champs) ; `-` la désactive

Aucun secret n'est nécessaire (Ollama n'est pas authentifié : ne pas l'exposer hors du serveur applicatif).

### Keycloak

L'API est protégée par un serveur de ressources OAuth2 (JWT). Variables attendues :

- `KEYCLOAK_ISSUER_URI` (défaut : `http://localhost:8080/realms/gpdi`)
- `KEYCLOAK_CLIENT_ID` (défaut : `gpdi-client-id`)

Les rôles sont lus dans le claim `resource_access.<KEYCLOAK_CLIENT_ID>.roles` du jeton JWT. Rôles attendus côté
Keycloak (un par domaine, définis au niveau du client) :


- `GESTIONNAIRE_ROLE` : `/api/roles/**` et `/api/habilitations/**` 
- `GESTIONNAIRE_UTILISATEUR` : `/api/utilisateurs/**`
- `GESTIONNAIRE_EQUIPE` : `/api/equipes/**`
- `GESTIONNAIRE_PROJET` : `/api/projets/**` (hors sous-routes ci-dessous)
- `GESTIONNAIRE_SPRINT` : `/api/projets/{idProjet}/sprints/**`
- `GESTIONNAIRE_JALON` : `/api/projets/{idProjet}/jalons/**`
- `GESTIONNAIRE_LIVRABLE` : `/api/jalons/{idJalon}/livrables/**`
- `GESTIONNAIRE_TACHE` : `/api/projets/{idProjet}/taches/**` et `/api/taches/**` (sous-tâches, commentaires, dépendances)
- `GESTIONNAIRE_PARTAGE` : `/api/partages-clients/**`
- `GESTIONNAIRE_RISQUE` : `/api/projets/{idProjet}/risques/**` (lecture, détection, suggestions, application, rejet)

Rôles métier (valeurs de la table `roles`, exactement les 5 valeurs attendues par le frontend
`core/models/role.model.ts` : `administrateur`, `directeur_dsi`, `chef_projet`, `membre_equipe`,
`direction_metier`) : `chef_projet` (responsable de projet), `membre_equipe` (chef ou membre d'équipe — un
"chef d'équipe" est un `membre_equipe` désigné responsable d'une équipe donnée, pas un rôle applicatif à
part), `direction_metier` (destinataire d'un partage de projet). Ces valeurs sont indépendantes des rôles
Keycloak d'accès ci-dessus et servent à deux mécanismes de contrôle additionnels, décrits ci-dessous.

### Contrôle d'accès à trois niveaux

Une requête passe par jusqu'à trois vérifications indépendantes, chacune pouvant refuser l'accès (403) :

1. **Rôle Keycloak** (`SecurityConfig`, ci-dessus) : filtre grossier par domaine fonctionnel, appliqué au
   niveau HTTP avant d'atteindre le contrôleur.
2. **Appartenance au projet** (`AccesProjetService`) : sur Jalon, Livrable, Sprint, Tâche, Sous-tâche,
   Commentaire, Dépendance. Écriture réservée au responsable du projet, au chef d'équipe ou à un membre de
   l'équipe assignée ; lecture élargie au Directeur DSI (portefeuille complet) et, pour Jalon/Livrable
   uniquement, aux comptes `direction_metier` disposant d'un `PartageClient` `ACTIF` sur ce projet précis.
3. **Matrice d'habilitations** (`HabilitationService`, dynamique) : sur Projet (création, clôture), Sprint
   (gestion, clôture), Jalon/Livrable (gestion), Tâche (création ; en modification, vérifiée **champ par
   champ** — un `membre_equipe` peut changer le statut ou le temps passé d'une tâche sans avoir le droit de
   modifier son titre, sa priorité ou son assignation), Partage client (création). Le rôle métier de
   l'utilisateur courant doit avoir le niveau `ECRITURE` sur la fonction concernée dans la table
   `habilitations` (voir section Habilitation ci-dessous) ; sinon `HabilitationRefuseeException` (403).
   Les fonctions de gestion des comptes/rôles/habilitations elles-mêmes, le rattachement aux équipes et le
   journal des connexions restent protégées uniquement par le rôle Keycloak (niveau 1) : la matrice ne les
   couvre pas encore.

## Profils Spring

- `application.properties` : configuration commune (défaut/dev).
- `application-preprod.properties` : recette, Swagger accessible.
- `application-prod.properties` : production, Swagger désactivé.
- `application-test.properties` : profil de test (`spring.profiles.active=test`), utilisé par les tests
  d'intégration (voir `src/test/resources/schema.sql` pour les types ENUM créés avant chaque suite de tests).

## Lancement

```text
./mvnw spring-boot:run
```

## Tests

```text
./mvnw test
```

Les tests d'intégration (un par contrôleur) utilisent Testcontainers pour démarrer un PostgreSQL réel :
**Docker doit être disponible** sur la machine exécutant les tests.

Les appels au vrai Ollama ne font pas partie de la suite : les tests `*OllamaReelTest` (qualité des prompts avec
`qwen3:8b`) ne s'exécutent que si la variable d'environnement `OLLAMA_REEL=true` est définie et qu'Ollama tourne.
Dans tous les autres tests, Ollama est simulé.

Sous Windows, si Testcontainers ne trouve pas l'environnement Docker malgré un `docker ps` fonctionnel, voir
`src/test/resources/docker-java.properties` (fixe la version d'API Docker utilisée par le client).

## Documentation API

Swagger UI, disponible en dev/recette uniquement (`/swagger-ui/index.html`), désactivé en production.

## Tâches planifiées

Cinq tâches tournent tous les jours à minuit (`@Scheduled(cron = "0 0 0 * * *")`) :

- `ProjetPlanificateur` : `EN_PREPARATION` -> `EN_COURS` si `date_debut` atteinte (active aussi l'équipe)
- `JalonPlanificateur` : `A_VENIR` -> `EN_RETARD` si `date_echeance` dépassée
- `PartageClientPlanificateur` : `ACTIF` -> `EXPIRE` si `date_echeance` dépassée
- `BurndownPlanificateur` : Capture un instantané du travail restant de chaque sprint `EN_COURS` (voir section Burndown)
- `NotificationPlanificateur` : Rappels d'échéance : notifie les assignés des tâches non terminées dont l'échéance est dans 24 h au plus ou dépassée (voir section Notification)

Une sixième tâche, `RisquePlanificateur`, tourne chaque nuit à **2 h** (`IA_RISQUE_CRON`) : elle lance la détection
des risques par l'IA sur chaque projet `EN_COURS` (voir section Risque). Elle s'interrompt au premier échec
d'Ollama plutôt que d'attendre le délai d'expiration pour chaque projet restant.

## API — vue d'ensemble

Toutes les routes sont préfixées par `/api`. Pagination Spring Data standard (`?page=&size=&sort=`) sur les
listes. Un jeton JWT Keycloak valide est requis partout (401 sans jeton, 403 avec un rôle insuffisant).

### Role — `/api/roles`
`POST` / `GET` (paginé) / `GET /{id}` / `PUT /{id}` / `DELETE /{id}`

### Habilitation — `/api/habilitations`
`GET` — matrice complète, regroupée par fonction : `[{ module, fonction, acces: { <roleNom>: "ecriture"|"lecture"|"aucun" } }]`.
`PUT` (body : liste des cases modifiées `[{ fonction, roleNom, niveau }]`) — met à jour uniquement les cases
fournies et renvoie la matrice complète à jour. `roleNom` doit être l'une des 5 valeurs de rôle métier
existantes (`400` sinon) et `fonction` doit déjà exister dans la matrice (les 22 fonctions sont fixes, créées
par le script d'initialisation — voir Base de données) : cet endpoint édite des droits, il ne crée pas de
nouvelle fonction ni de nouveau rôle. Voir section Sécurité pour la façon dont cette matrice est appliquée.

### Utilisateur — `/api/utilisateurs`
`POST` / `GET` (paginé) / `GET /{id}` / `PUT /{id}` / `DELETE /{id}`
Mot de passe haché (BCrypt), optionnel à la modification (conservé si absent). `actif` (suivi de connexion,
non géré par ce CRUD) toujours `false` à la création.

### Équipe — `/api/equipes`
`POST` / `GET` (paginé) / `GET /{id}` / `PUT /{id}` / `DELETE /{id}`
Chef et membres doivent avoir le rôle `membre_equipe` (le "chef d'équipe" est une désignation, pas un rôle à
part — voir plus haut). `active` (système, devient `true` quand
un projet est affecté à l'équipe) non modifiable via ce CRUD.

### Projet — `/api/projets`
`POST` / `GET` (paginé) / `GET /{id}` / `PUT /{id}`
`POST /{id}/suspendre` — `-> SUSPENDU`.
`POST /{id}/cloturer` — `-> CLOTURE`, fixe `date_cloture`.
`POST /{id}/archiver` — `-> ARCHIVE`, remplace la suppression (jamais de perte de données).
Pas de `DELETE` sur ce CRUD (archivage uniquement). Responsable = rôle `chef_projet`. `idEquipe` optionnel.
`code_reference` (ex. `GPDI-2026-014`) et `sante` (`sain`/`sous_surveillance`/`a_risque`, dérivée de
`date_fin` et non stockée) générés côté serveur, jamais fournis par le client.

### Sprint — `/api/projets/{idProjet}/sprints`
`POST` / `GET` (paginé) / `GET /{id}` / `PUT /{id}` / `DELETE /{id}`
`POST /{id}/commencer` — `PLANIFIE -> EN_COURS`.
`POST /{id}/terminer` — `EN_COURS -> CLOTURER`.

### Jalon — `/api/projets/{idProjet}/jalons`
`POST` / `GET` (paginé) / `GET /{id}` / `PUT /{id}` / `DELETE /{id}`
`POST /{id}/terminer` — `-> ATTEINT`, fixe `date_validation`.
`POST /{id}/annuler` — `-> ANNULER`.

### Livrable — `/api/jalons/{idJalon}/livrables`
`POST` / `GET` (paginé) / `GET /{id}` / `PUT /{id}` / `DELETE /{id}`
`POST /{id}/livrer` — `ATTENDU -> LIVRE`, fixe `date_validation`.
`POST /{id}/rejeter` (body `{ motifRejet }`) — `ATTENDU -> REJETE`, fixe `date_rejet` + `motif_rejet`.
Validation : `date_livraison` ne peut pas dépasser la `date_echeance` du jalon.

### Tâche — `/api/projets/{idProjet}/taches`
`POST` / `GET` (paginé, réponse "carte") / `GET /{id}` (réponse "détail" complète) / `PUT /{id}` / `DELETE /{id}`
`code` généré automatiquement (`T-<id>`). `id_sprint` optionnel (absent = backlog du projet). `date_echeance`
optionnel. `statut` optionnel sur `PUT` (absent = conserve le statut courant ; permet de déplacer une tâche
entre sprints sans modifier son statut). Assignation limitée aux membres de l'équipe du projet.

**Suivi du temps** : le passage du `statut` à `EN_COURS` démarre automatiquement le chronomètre
(`demarree_le`, horodatage) s'il n'était pas déjà lancé ; le passage à `TERMINEE` fige le temps écoulé dans
`temps_passe` (heures), arrête le chronomètre et fixe `date_fin_reelle`. Les autres transitions de statut
(y compris un retour à `EN_COURS` ou `A_FAIRE`) ne touchent jamais un chronomètre déjà lancé. `temps_passe`
peut aussi être fourni explicitement dans le corps du `PUT` (saisie manuelle) ; absent, il est conservé.

Sous-ressources (imbriquées sous `/api/taches/{idTache}/...`, même rôle `GESTIONNAIRE_TACHE`) :
- `sous-taches` : `POST` (body `{ titre }`) / `GET` / `DELETE /{id}` / `POST /{id}/basculer` (inverse `fait`).
- `commentaires` : `POST` (body `{ contenu }`, auteur déduit du jeton connecté) / `GET` (ordre chronologique).
  Pas d'édition/suppression.
  - `POST .../avec-piece-jointe` (`multipart/form-data` : champ `fichier` obligatoire, champ `contenu` texte
    facultatif) — ajoute un commentaire avec une pièce jointe (une seule par commentaire). Taille max 10 Mo
    (`400` sinon). Le contenu binaire est stocké en base (`pieces_jointes_commentaires.contenu`, `BYTEA`), pas
    sur un stockage fichier externe.
  - `GET .../{idCommentaire}/piece-jointe` — télécharge la pièce jointe (réponse binaire, `Content-Disposition:
    attachment`). `404` si le commentaire n'a pas de pièce jointe.
  - `GET` (liste des commentaires) renvoie, pour chaque commentaire, `pieceJointe: { id, nomFichier,
    typeContenu, taille }` si une pièce jointe existe, sinon `null` (jamais le contenu binaire lui-même).
- `dependances` : `POST` (body `{ idTacheCible, typeLien }`, la tâche courante est toujours "source") /
  `GET` / `DELETE /{idAutreTache}`. Une seule relation possible entre deux tâches données (peu importe le sens).
  `typeLien` : `DEBUT_DEBUT`, `DEBUT_FIN`, `FIN_DEBUT`, `FIN_FIN`.

**Dépendances bloquantes** : le passage de statut d'une tâche "cible" est refusé (400) si une dépendance
vers sa tâche "source" n'est pas satisfaite. Tant qu'une tâche n'a jamais démarré (`A_FAIRE`/`REPORTEE`),
en sortir — vers `EN_COURS`, `EN_TEST`, `BLOQUEE` ou `TERMINEE` — est bloqué si une dépendance de démarrage
n'est pas satisfaite : `FIN_DEBUT` exige que la source soit `TERMINEE` ; `DEBUT_DEBUT` exige que la source
ait au moins démarré une fois. La tâche reste alors figée en `A_FAIRE` jusqu'à ce que la condition le soit.
Une fois démarrée, circuler entre `EN_COURS`/`EN_TEST`/`BLOQUEE` ne revalide pas cette condition. Le passage
à `TERMINEE` est en plus toujours soumis à une dépendance de clôture : `FIN_FIN` exige que la source soit
`TERMINEE` ; `DEBUT_FIN` exige que la source ait démarré.

**Champs différés (mappés en base, non exposés dans les DTO)** : `date_debut` sur `taches` (non utilisé,
distinct de `demarree_le` — voir suivi du temps ci-dessus) ; `progression` sur `sprints`/`taches` (nécessite
le calcul basé sur l'avancement des tâches, non encore implémenté).

### Burndown — `/api/projets/{idProjet}/sprints/{idSprint}/burndown`
`GET` — série jour par jour (`date`, `restant`, `ideal`) sur toute la durée du sprint, même rôle que
Sprint (`GESTIONNAIRE_SPRINT`).
`ideal` est toujours calculable (trajectoire linéaire depuis la charge estimée totale du sprint jusqu'à 0
à `date_fin`). `restant` est `null` pour un jour futur ou antérieur à la mise en place de la capture
quotidienne : `Tache.temps_passe` n'étant qu'un cumul sans historique daté, la valeur réelle jour par jour
ne peut être reconstituée qu'à partir d'un instantané capturé le jour même (voir `BurndownPlanificateur` et
la table `burndown_snapshots`).

### Notification — `/api/notifications`
Réservé à l'utilisateur connecté (authentification suffit, aucun rôle Keycloak dédié) : chacun ne voit et ne
modifie que ses propres notifications.
`GET` (paginé, plus récentes d'abord) / `POST /{id}/lire` (marque une notification lue) /
`POST /lire-toutes` (204, marque toutes les siennes lues). Une notification d'un autre utilisateur répond `404`.
Génération automatique, sans action manuelle (types `BLOCAGE`, `ECHEANCE`, `JALON` ; `IA` est prévu mais rien
ne le génère) :
- `BLOCAGE` : une tâche passe à `BLOQUEE` -> ses assignés + le responsable du projet.
- `ECHEANCE` : job quotidien, tâches non `TERMINEE` d'un projet actif dont l'échéance est dans 24 h au plus ou
  dépassée -> ses assignés, une seule fois par tâche.
- `JALON` : un jalon passe `EN_RETARD` (job existant) ou `ATTEINT` -> le responsable du projet, une seule fois.
`cle` (dédoublonnage des notifications générées par job) est protégée par un index unique en base ; supprimer un
utilisateur supprime ses notifications (`ON DELETE CASCADE`).

### Partage client — `/api/partages-clients`
`POST` (body `{ idUtilisateur, idsProjets: [...], dateEcheance }`, une ligne créée par projet) /
`GET` (paginé, tous les partages) / `POST /{id}/revoquer` (`ACTIF -> REVOQUE`, définitif).
Le destinataire doit avoir le rôle `direction_metier`. Un projet ne peut avoir qu'un partage `ACTIF` à la fois avec un
même client (un nouveau partage est possible une fois le précédent révoqué ou expiré).

### Point d'information — `/api/projets/{idProjet}/points-information`

Pas de rôle Keycloak dédié (l'endpoint demande seulement d'être authentifié) : le périmètre exact est
vérifié dans `PointInformationServiceImpl`, comme pour les notifications.
`POST` (body `{ titre, corps }`, réservé au responsable du projet) — publie un point d'information.
`GET` (paginé, plus récent d'abord) — réservé au responsable du projet **ou** à un compte `direction_metier`
disposant d'un `PartageClient` `ACTIF` sur ce projet précis. `403` (`AccesPointsInformationRefuseException`)
sinon.

### Assistant IA — `/api/projets/{idProjet}/assistant/messages`

Rôle Keycloak `GESTIONNAIRE_PROJET` ; le périmètre exact (chef de projet du projet demandé, et lui seul) est
en plus vérifié dans `AssistantIaServiceImpl` (`403` sinon). Chat en langage libre sur le contexte du projet
(même contexte que la détection de risques — voir `ContexteProjetService`), interrogeant Ollama hors
transaction ; l'échange n'est enregistré qu'une fois la réponse obtenue.
`GET` — historique des échanges du projet, du plus ancien au plus récent : `[{ id, question, reponse,
dateCreation }]`.
`POST` (body `{ question }`, peut durer plusieurs minutes) — pose une question, renvoie
`{ id, question, reponse, fondement, dateCreation }` (`fondement` : justification de la réponse par le
modèle). Erreurs : `404` projet introuvable ; `503` Ollama indisponible ; `502` réponse de l'IA inexploitable.

<<<<<<< HEAD
=======
### Assistant IA global — `/api/assistant/messages`

Même principe que l'assistant d'un projet, mais sur **tous les projets dont l'utilisateur connecté est responsable**
(sans choix de projet : onglet Assistant IA et bulle de discussion du frontend). Rôle Keycloak `GESTIONNAIRE_PROJET` ;
le rôle métier `chef_projet` est en plus vérifié dans `AssistantIaServiceImpl` (`403`,
`AccesAssistantIaGlobalRefuseException`, sinon). Le contexte envoyé au modèle est une version condensée du contexte
projet (`ContexteProjetService.construirePortefeuille`) : projets actifs détaillés (6 au plus), projets clôturés ou
archivés réduits à leur fiche. Échange enregistré dans `ia_analyses` avec la portée `GLOBAL` et sans projet
(script `init-lot14-assistant-ia-global.sql`).
`GET` — historique des échanges globaux de l'utilisateur connecté, du plus ancien au plus récent.
`POST` (body `{ question }`, peut durer plusieurs minutes) — mêmes réponse et erreurs que l'assistant d'un projet.

**Actions proposées** (`ActionAssistantService`, table `ia_actions_assistant`, script `init-lot16-actions-assistant-ia.sql`) :
quand l'utilisateur demande de *faire* quelque chose, le modèle renseigne en plus un champ `action` (un seul type parmi
`TypeActionAssistant` : création/modification/démarrage/suspension/reprise/clôture/archivage de projet ; création/
modification/démarrage/clôture/suppression de sprint ; création/modification/atteinte/annulation/suppression de jalon ;
création/modification/affectation/retrait/changement de statut/archivage/restauration/suppression de tâche), avec des
désignations libres (titre de projet, code de tâche, nom de sprint, de jalon ou de personne). Le backend les résout parmi
les projets dont l'utilisateur est responsable (personnes : responsable, chef d'équipe et membres actifs de l'équipe du
projet) ; une désignation introuvable ou ambiguë, ou un champ obligatoire manquant, ne produit aucune action : la réponse
devient la question de précision. **Rien n'est exécuté à ce stade** : la réponse contient `action: { id, type, libelle,
details, irreversible, statut: "PROPOSEE" }` (aussi renvoyée par l'historique).
- `POST /api/assistant/actions/{id}/confirmer` — exécute l'action via le service métier existant (`ProjetService`,
  `SprintService`, `JalonService`, `TacheService` : mêmes règles, même matrice d'habilitations ; une modification part de
  l'état courant relu à la confirmation) ; exige en plus le rôle Keycloak de la route équivalente (`403`,
  `RoleActionAssistantManquantException`) → `statut: "APPLIQUEE"`, `resultat`, `idProjet`. Une erreur métier est renvoyée
  telle quelle et l'action reste `PROPOSEE`.
- `POST /api/assistant/actions/{id}/annuler` — `statut: "ANNULEE"`, rien n'est modifié.
Erreurs : `404` action inconnue ou proposée à un autre utilisateur ; `409` action déjà confirmée ou annulée.

>>>>>>> nicky
### Recommandations de pilotage — `/api/projets/{idProjet}/recommandations`

Lecture ouverte au chef de projet du projet et, en lecture seule, au Directeur DSI (vue portefeuille) ;
écriture (`analyser`/`appliquer`/`rejeter`) réservée au chef de projet (`403`,
`AccesRecommandationsProjetRefuseException`, sinon). Recommandations proactives générées par l'IA à partir de
l'état courant du projet (tâches en retard, charge d'équipe déséquilibrée, estimations dépassées…), distinctes
des solutions de risque (mêmes tables `ia_analyses`/`ia_recommandations`, mais sans `id_risque`).
`GET` — recommandations `PROPOSEE` en attente, de la plus récente à la plus ancienne :
`[{ id, type, libelle, justification, preuve, statut, resultat, dateCreation }]` (`type` :
`REASSIGNER_TACHE`, `RETIRER_DU_SPRINT` ou `AJUSTER_ESTIMATION`).
`POST /analyser` (peut durer plusieurs minutes) — interroge l'IA et enregistre de nouvelles recommandations ;
une seule analyse à la fois par projet (`409` sinon, `AnalyseRecommandationsDejaEnCoursException`).
`POST /{id}/appliquer` — exécute l'action proposée sur le projet (via `TacheService`) et passe la
recommandation à `APPLIQUEE` (`resultat` renseigné).
`POST /{id}/rejeter` — passe la recommandation à `REJETEE` sans effet sur le projet.
Erreurs communes : `404` projet ou recommandation introuvable ; `409` recommandation déjà traitée
(`StatutRecommandationProjetInvalideException`).

### Risque (IA) — `/api/projets/{idProjet}/risques`

Rôle Keycloak `GESTIONNAIRE_RISQUE` pour toutes les routes. L'assistant IA (Ollama, `qwen3:8b`) détecte les
risques d'un projet ; l'utilisateur ne fait que consulter, demander des solutions, les appliquer ou rejeter le
risque. Les statuts sont renvoyés en majuscules.

- `GET /` : Tous les risques du projet (non paginé), par ordre de détection
- `POST /detection` : Lance la détection maintenant (le job de nuit fait la même chose)
- `POST /{idRisque}/suggestions` : Solutions proposées pour un risque `OUVERT` (bouton « Suggérer »)
- `POST /{idRisque}/appliquer` : Exécute les solutions cochées (bouton « Appliquer »)
- `POST /{idRisque}/rejeter` : Rejette le risque (bouton « Rejeter »)

**`GET /`** renvoie `[{ id, code, libelle, parade, analyse, probabilite, impact, niveau, statut, dateDetection,
idPorteur, porteur, solutions }]`. `code` (`R1`, `R2`…) est le rang du risque dans son projet, calculé à la lecture.
`parade` = colonne `mesure_mitigation`, `analyse` = colonne `description`. `solutions` liste les solutions en
attente de décision (`{ id, libelle, justification, gain, typeAction }`), vide sinon.

**`POST /detection`** renvoie `{ idAnalyse, nombreNouveauxRisques, nombreRisquesReevalues, nombreRisquesClos,
synthese }`. La durée va de ~10 s à ~3 min (modèle déchargé de la mémoire) : prévoir un indicateur d'attente côté
frontend, sans délai d'expiration court, puis recharger la liste.

**`POST /{idRisque}/suggestions`** renvoie la liste des solutions. Si des solutions sont déjà en attente pour ce
risque, elles sont renvoyées telles quelles, sans appel à l'IA (mise en cache) ; sinon l'IA est interrogée. Chaque
solution est soit `REASSIGNER_TACHE`, soit `CREER_TACHE`, avec un `gain` (bénéfice attendu, texte libre).

**`POST /{idRisque}/appliquer`** (body `{ idsSolutions: [ ... ] }`, les `id` des solutions cochées) renvoie
`{ idRisque, statut: "SOUS_CONTROLE", solutions: [{ id, libelle, typeAction, idTache, codeTache, suivi, remarque }] }`.

**`POST /{idRisque}/rejeter`** (body facultatif `{ motif }`) renvoie `{ idRisque, statut: "REJETE", solutions: [] }`.

**Cycle de vie d'un risque** : `OUVERT` -> (appliquer) `SOUS_CONTROLE`, ou (rejeter) `REJETE`. `CLOS` est atteint
automatiquement de deux façons : (1) quand la réévaluation d'un risque `SOUS_CONTROLE` le ramène au niveau `FAIBLE`
(voir Détection ci-dessous), ou (2) dès que toutes les tâches créées ou réassignées par ses solutions appliquées
passent à `TERMINEE` ou `REPORTEE` (`RisqueServiceImpl.cloreSiTachesTerminees`, déclenché à chaque changement de
statut de tâche par `TacheServiceImpl.modifier`) — la mitigation est alors considérée terminée indépendamment du
prochain passage de l'IA. Suggérer, appliquer et rejeter ne sont possibles que sur un risque `OUVERT` (409 sinon).

**Détection** : le contexte envoyé au modèle (projet, équipe, tâches à surveiller, sprints, jalons, risques
connus) est conservé dans `ia_analyses.contexte`. Il ne contient ni matricule, ni email, ni téléphone, ni commentaire.
La réponse du modèle est vérifiée élément par élément : notes hors de 1 à 5, libellé vide, doublon d'un risque déjà
connu (quel que soit son statut, y compris rejeté) ou identifiant inventé sont écartés ; 5 nouveaux risques au plus par
analyse. Le **niveau** est toujours calculé par le backend (`probabilite × impact` : ≥ 15 `CRITIQUE`, ≥ 10 `ELEVE`,
≥ 5 `MOYEN`, sinon `FAIBLE`), jamais fourni par l'IA. Le porteur est celui proposé par l'IA s'il est membre de
l'équipe, sinon le responsable du projet. Seuls les risques `SOUS_CONTROLE` sont réévalués ; un risque rejeté n'est
jamais recréé. Une seule analyse à la fois par projet (409 sinon).

**Application** : tout ou rien, dans une seule transaction, via le `TacheService` existant. `CREER_TACHE` crée une
tâche dans le backlog (priorité `HAUTE` si le risque est `CRITIQUE` ou `ELEVE`, sinon `NORMALE`, description
mentionnant le risque d'origine). `REASSIGNER_TACHE` ne change que l'assignation, tous les autres champs de la
tâche sont conservés. Une solution non exécutable devient une tâche de suivi (`suivi: true`, avec une `remarque`) :
tâche introuvable ou clôturée, membre proposé hors de l'équipe du projet. Un membre hors équipe sur une création
donne une tâche sans assignation (`remarque`). Les solutions cochées passent à `APPLIQUEE` (résultat conservé dans
`resultat_application`), les autres solutions en attente à `REJETEE`. Le risque est verrouillé pendant le
traitement (pas de double application).

**Erreurs** : `404` risque, solution ou projet introuvable ; `409` risque non `OUVERT`, solution déjà traitée,
analyse ou recherche de solutions déjà en cours ; `400` aucune solution cochée ; `503` Ollama indisponible ;
`502` réponse de l'IA inexploitable (réessayer).

**Limites connues** : le verrou « une analyse à la fois » est en mémoire, donc valable pour une seule instance de
l'application ; le job de nuit lit tous les projets et ne retient que ceux `EN_COURS` ; l'IA peut proposer un
responsable de projet ou un chef d'équipe sans ligne dans `membre_equipes`, que le `TacheService` refuse comme
assigné (traité comme indiqué ci-dessus) ; l'`indiceConfiance` du modèle n'est pas fiable, il est conservé
seulement s'il est compris entre 0 et 1.

## Base de données — types ENUM PostgreSQL natifs attendus

Ces types doivent exister dans la base réelle (créés manuellement, `ddl-auto=none`) :

- `priorite` : `CRITIQUE`, `HAUTE`, `NORMALE`, `BASSE`
- `statut_projet` : `EN_PREPARATION`, `EN_COURS`, `SUSPENDU`, `CLOTURE`, `ARCHIVE`
- `statut_sprint` : `PLANIFIE`, `EN_COURS`, `CLOTURER`
- `statut_jalon` : `A_VENIR`, `EN_RETARD`, `ATTEINT`, `ANNULER`
- `statut_livrable` : `ATTENDU`, `LIVRE`, `REJETE`
- `statut_tache` : `A_FAIRE`, `EN_COURS`, `EN_TEST`, `BLOQUEE`, `TERMINEE`, `REPORTEE`
- `type_lien` : `DEBUT_DEBUT`, `DEBUT_FIN`, `FIN_DEBUT`, `FIN_FIN`
- `statut_partage` : `ACTIF`, `REVOQUE`, `EXPIRE`
- `type_analyse_ia` : `ANALYSE_PROJET`, `PLANIFICATION_SPRINT`, `CHARGE_EQUIPE`, `DETECTION_RISQUE`, `SUGGESTION_RISQUE`, `SYNTHESE`
- `portee_analyse_ia` : `PROJET`, `SPRINT`, `EQUIPE`
- `statut_recommandation` : `PROPOSEE`, `APPLIQUEE`, `REJETEE`
- `niveau_risque` : `FAIBLE`, `MOYEN`, `ELEVE`, `CRITIQUE`
- `statut_risque` : `OUVERT`, `SOUS_CONTROLE`, `REJETE`, `CLOS`

La table `habilitations` (matrice des droits, voir section Sécurité) stocke son niveau (`ECRITURE`/`LECTURE`/
`AUCUN`) en `VARCHAR`, pas en type ENUM natif : table de configuration peu volumineuse, sans le même besoin
de performance que les entités métier ci-dessus.

### Tables du module IA

Créées manuellement, comme le reste du schéma. Colonnes lues et écrites par le backend :

- `ia_analyses` : `id_analyse`, `id_projet`, `id_demandeur` (NULL = analyse automatique), `type_analyse`, `portee`, `question`, `reponse`, `contexte` (jsonb), `indice_confiance` (numeric 4,3), `modele`, `date_creation`
- `ia_recommandations` : `id_recommandation`, `id_analyse`, `id_risque` (NULL possible ; NULL = recommandation de pilotage, non liée à un risque), `libelle`, `justification`, `action_proposee` (jsonb : `type`, paramètres, `gain`), `statut`, `id_validateur`, `motif_rejet`, `date_validation`, `resultat_application` (jsonb), `date_creation`
- `risques` : `id_risque`, `id_projet`, `id_analyse`, `id_porteur`, `libelle` (300), `description`, `probabilite`, `impact`, `niveau`, `statut`, `mesure_mitigation`, `date_creation`, `date_mise_a_jour`
- `points_information` : `id_point_information`, `id_projet`, `id_auteur`, `titre`, `corps`, `date_creation`
- `habilitations` : `id`, `module`, `fonction`, `role_nom`, `niveau` (`VARCHAR`) — contrainte unique (`fonction`, `role_nom`)
- `pieces_jointes_commentaires` : `id_piece_jointe`, `id_commentaire` (unique), `nom_fichier`, `type_contenu`, `taille`, `contenu` (`BYTEA`), `date_creation`
