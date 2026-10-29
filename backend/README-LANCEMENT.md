# GPDI Backend — Guide de lancement

Étapes pour obtenir un backend **fonctionnel de bout en bout** en local : base PostgreSQL, Keycloak,
module IA (Ollama + modèle **Qwen3 8B fourni en fichier GGUF**), puis démarrage de Spring Boot.

> Le détail des API, des règles métier et du contrôle d'accès se trouve dans [README.md](README.md).

---

## 0. Prérequis

| Outil | Version | Rôle |
|---|---|---|
| JDK | 8 minimum (testé aussi avec 21) | Exécution de Spring Boot 2.7.18 |
| PostgreSQL | 13+ | Base de données `GPDI` |
| Keycloak | 2x+ | Authentification (OAuth2 / JWT) |
| Ollama | récent | Serveur local du modèle IA |
| Fichier `.gguf` | Qwen3 8B (ex. `Qwen3-8B-Q4_K_M.gguf`, ~5 Go) | Modèle IA |
| Docker | — | **Uniquement** pour lancer les tests (Testcontainers) |

Maven n'est pas nécessaire : le wrapper `mvnw` / `mvnw.cmd` est fourni.

Vérification rapide :

```powershell
java -version
psql --version
ollama --version
```

---

## 1. Base de données PostgreSQL

### 1.1 Créer la base

```powershell
psql -h localhost -U postgres -c "CREATE DATABASE \"GPDI\";"
```

### 1.2 Créer le schéma

Le schéma **n'est pas généré par Hibernate** (`ddl-auto=none`) : il doit exister avant le démarrage.
Les scripts sont dans `src/main/resources/db/` et doivent être exécutés **dans cet ordre** (ils sont
tous rejouables sans risque) :

```powershell
cd backend/src/main/resources/db

psql -h localhost -U postgres -d GPDI -f init-lot1-utilisateurs.sql
psql -h localhost -U postgres -d GPDI -f init-lot2-equipes.sql
psql -h localhost -U postgres -d GPDI -f init-lot3-projets-partages.sql
psql -h localhost -U postgres -d GPDI -f init-lot4-sprints-jalons.sql
psql -h localhost -U postgres -d GPDI -f init-lot5-taches.sql
psql -h localhost -U postgres -d GPDI -f init-lot6-burndown.sql
psql -h localhost -U postgres -d GPDI -f init-lot7-notifications.sql
psql -h localhost -U postgres -d GPDI -f init-lot8-risques-ia.sql
psql -h localhost -U postgres -d GPDI -f init-lot9-points-information.sql
psql -h localhost -U postgres -d GPDI -f init-lot10-habilitations.sql
psql -h localhost -U postgres -d GPDI -f init-lot11-pieces-jointes-commentaires.sql
psql -h localhost -U postgres -d GPDI -f init-lot12-rapports-diffuses.sql
psql -h localhost -U postgres -d GPDI -f init-lot13-fix-notifications-date-creation.sql
```

> ⚠️ L'ordre `lot1 → lot13` est l'ordre **numérique**, pas l'ordre alphabétique de l'explorateur
> (qui place `lot10` avant `lot2`).

Le script `lot1` crée aussi les **5 rôles métier** et **5 comptes de test**, un par rôle :

| Matricule | Nom | Rôle métier |
|---|---|---|
| `10001` | D. Nikièma | `administrateur` |
| `10002` | S. Compaoré | `directeur_dsi` |
| `45820` | K. Traoré | `chef_projet` |
| `33110` | A. Koffi | `membre_equipe` |
| `50021` | L. Sawadogo | `direction_metier` |

---

## 2. Keycloak

Le backend vérifie les jetons JWT émis par Keycloak. Le **matricule GPDI = username Keycloak**
(claim `preferred_username`) : c'est ce lien qui permet de retrouver l'utilisateur et son rôle métier.

### 2.1 Realm et client

1. Démarrer Keycloak sur `http://localhost:8080`.
2. Créer le realm **`gpdi`**.
3. Créer le client **`gpdi-client-id`** (utilisé à la fois par le frontend et le backend) :
   - *Client authentication* : **Off** (client public, le frontend Angular s'y connecte directement)
   - *Standard flow* : **On**
   - *Valid redirect URIs* : `http://localhost:4200/*`
   - *Web origins* : `http://localhost:4200`

### 2.2 Rôles du client

Dans le client `gpdi-client-id` → onglet **Roles**, créer ces rôles (ce sont eux que lit
`SecurityConfig`, dans le claim `resource_access.gpdi-client-id.roles`) :

```
GESTIONNAIRE_ROLE          GESTIONNAIRE_UTILISATEUR    GESTIONNAIRE_EQUIPE
GESTIONNAIRE_PROJET        GESTIONNAIRE_SPRINT         GESTIONNAIRE_JALON
GESTIONNAIRE_LIVRABLE      GESTIONNAIRE_TACHE          GESTIONNAIRE_PARTAGE
GESTIONNAIRE_RISQUE
```

### 2.3 Utilisateurs

Pour **chaque matricule** du tableau de l'étape 1.2 :

1. Créer un utilisateur Keycloak avec **username = le matricule** (ex. `45820`).
2. Lui définir un mot de passe (onglet *Credentials*, *Temporary* : Off).
3. Lui attribuer les rôles client `GESTIONNAIRE_*` correspondant à ce qu'il doit faire
   (onglet *Role mapping* → *Assign role* → filtrer par client `gpdi-client-id`).

Le mot de passe stocké dans la table `utilisateurs` n'est **pas** utilisé : l'authentification
passe entièrement par Keycloak.

### 2.4 (Optionnel) Création automatique des comptes Keycloak

Pour que la création/suppression d'un utilisateur depuis GPDI crée/supprime aussi le compte Keycloak,
créer un second client **confidentiel** (*Client authentication* : On, *Service accounts roles* : On)
et lui donner, dans *Service account roles*, les rôles `realm-management` → `manage-users` et
`view-users`. Renseigner ensuite son id et son secret dans `.env` (étape 4).
Sans ce client, les comptes Keycloak doivent être créés à la main.

---

## 3. Module IA : Ollama + Qwen3 8B en GGUF

Le backend interroge Ollama sur `http://localhost:11434` (endpoint `/api/chat`) avec le modèle nommé
**`qwen3:8b`**. Le modèle est ici fourni sous forme de **fichier `.gguf`** : il faut l'enregistrer
dans Ollama sous ce nom.

### 3.1 Installer et démarrer Ollama

Installer Ollama depuis <https://ollama.com/download>. Sous Windows, il démarre automatiquement en
tâche de fond (icône dans la barre des tâches). Sinon :

```powershell
ollama serve
```

Vérifier qu'il répond :

```powershell
curl http://localhost:11434/api/version
```

### 3.2 Importer le fichier GGUF

1. Placer le fichier dans un dossier dédié, par exemple `C:\ia\qwen3\Qwen3-8B.gguf`.
2. Créer dans **ce même dossier** un fichier texte nommé `Modelfile` (sans extension) :

   ```
   FROM ./Qwen3-8B.gguf

   PARAMETER num_ctx 8192
   PARAMETER temperature 0.6
   PARAMETER top_p 0.95
   PARAMETER top_k 20
   ```

   Adapter le nom après `FROM` au nom exact de votre fichier `.gguf`.

3. Créer le modèle **sous le nom attendu par le backend** :

   ```powershell
   cd C:\ia\qwen3
   ollama create qwen3:8b -f Modelfile
   ```

4. Vérifier :

   ```powershell
   ollama list
   # NAME        ID              SIZE      MODIFIED
   # qwen3:8b    xxxxxxxxxxxx    5.2 GB    ...

   ollama run qwen3:8b "Réponds juste : OK"
   ```

> **Pas de fichier GGUF sous la main ?** `ollama pull qwen3:8b` télécharge directement le modèle
> officiel sous le bon nom (les étapes 3.2.1 à 3.2.3 deviennent inutiles).

> **Autre nom de modèle ?** Si vous créez le modèle sous un autre nom, indiquez-le au backend via
> `OLLAMA_MODEL=<nom>` dans `.env`.

### 3.3 Points d'attention

- **Gabarit de conversation** : Ollama lit le gabarit de chat directement dans les métadonnées du
  GGUF. Vérifier qu'il est bien présent avec `ollama show qwen3:8b --template` (la sortie ne doit pas
  être vide). Si elle l'est, ou si les réponses de l'IA sont incohérentes, utiliser un GGUF Qwen3
  officiel (ex. dépôt `Qwen/Qwen3-8B-GGUF`) ou `ollama pull qwen3:8b`.
- **Premier appel lent** : le chargement à froid du modèle prend **2 à 3 minutes**. Le backend attend
  jusqu'à 300 s (`OLLAMA_TIMEOUT_SECONDS`) et garde ensuite le modèle en mémoire 30 min
  (`OLLAMA_KEEP_ALIVE`). Les appels suivants prennent ~10 s.
- **Mémoire** : prévoir ~6 Go de RAM/VRAM libres pour un Q4_K_M.
- **Sécurité** : Ollama n'a pas d'authentification. Ne pas l'exposer hors de la machine.
- **Alternative en ligne** : `IA_FOURNISSEUR=gemini` + `GEMINI_API_KEY` utilise Gemini au lieu
  d'Ollama. Le contexte des projets est alors **envoyé à Google**.

---

## 4. Fichier de configuration `.env`

Le backend lit automatiquement `backend/.env` au démarrage. Copier le modèle puis le remplir :

```powershell
cd backend
copy .env.example .env
```

Exemple de contenu pour un poste local :

```properties
SERVER_PORT=8081

DB_URL=jdbc:postgresql://localhost:5432/GPDI
DB_USERNAME=postgres
DB_PASSWORD=votre_mot_de_passe

KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/gpdi
KEYCLOAK_CLIENT_ID=gpdi-client-id

# Optionnel (étape 2.4)
KEYCLOAK_ADMIN_BASE_URL=http://localhost:8080
KEYCLOAK_REALM=gpdi
KEYCLOAK_ADMIN_CLIENT_ID=
KEYCLOAK_ADMIN_CLIENT_SECRET=

# IA (valeurs par défaut si absentes : ollama / http://localhost:11434 / qwen3:8b)
IA_FOURNISSEUR=ollama
# OLLAMA_BASE_URL=http://localhost:11434
# OLLAMA_MODEL=qwen3:8b
```

`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `KEYCLOAK_ISSUER_URI` et `KEYCLOAK_CLIENT_ID` sont
**obligatoires** : sans eux, l'application refuse de démarrer.

> `.env` contient des secrets : il est exclu de git (`.gitignore`). Ne jamais le committer.

---

## 5. Démarrer le backend

Depuis le dossier `backend/` :

```powershell
.\mvnw.cmd spring-boot:run
```

(Linux / macOS : `./mvnw spring-boot:run`)

Le backend est prêt lorsque la console affiche `Started GpdiApplication`. Il écoute sur
**`http://localhost:8081`**.

---

## 6. Vérifier que tout fonctionne

| Vérification | Comment | Résultat attendu |
|---|---|---|
| Backend démarré | <http://localhost:8081/swagger-ui/index.html> | Swagger UI s'affiche |
| Sécurité active | `curl -i http://localhost:8081/api/projets` | `401` (jeton requis) |
| Connexion | Lancer le frontend (`ng serve`), se connecter avec un matricule de l'étape 1.2 | Le tableau de bord du rôle s'affiche |
| IA | En tant que chef de projet, ouvrir un projet → *Assistant IA* ou *Risques* → lancer une détection | Réponse après 10 s à 3 min |

---

## 7. Problèmes fréquents

| Symptôme | Cause probable | Solution |
|---|---|---|
| L'application ne démarre pas : `Could not resolve placeholder 'DB_URL'` | `.env` absent ou incomplet | Étape 4 |
| `relation "..." does not exist` | Un script SQL n'a pas été exécuté | Rejouer les scripts de l'étape 1.2 dans l'ordre |
| `401` alors que l'utilisateur est connecté | `KEYCLOAK_ISSUER_URI` différent de l'émetteur réel du jeton | Vérifier realm et port Keycloak |
| `403` sur un écran | Rôle client `GESTIONNAIRE_*` manquant, ou droit absent de la matrice d'habilitations | Étape 2.3, ou page *Rôles et habilitations* |
| Utilisateur connecté mais « introuvable » | Username Keycloak ≠ matricule GPDI | Étape 2.3 |
| IA : `503` | Ollama non démarré | Étape 3.1 |
| IA : `model "qwen3:8b" not found` | Modèle GGUF importé sous un autre nom | `ollama list`, puis étape 3.2 ou `OLLAMA_MODEL` |
| IA : `502` (réponse inexploitable) | Gabarit absent dans le GGUF, ou réponse ponctuellement mal formée | Étape 3.3 ; sinon réessayer |
| IA : délai dépassé au premier appel | Chargement à froid trop lent sur la machine | Augmenter `OLLAMA_TIMEOUT_SECONDS` |

---

## 8. (Optionnel) Lancer les tests

Les tests d'intégration démarrent un PostgreSQL éphémère via Testcontainers : **Docker Desktop doit
être lancé**. Ollama est simulé dans les tests, il n'a pas besoin de tourner.

```powershell
docker ps
.\mvnw.cmd clean test
```

Utiliser `clean test` plutôt que `test` seul : cela évite des erreurs `NoClassDefFoundError`
dues à des classes compilées obsolètes dans `target/`.
