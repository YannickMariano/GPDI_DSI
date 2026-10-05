# Directives de Modification du Code

## Principes Fondamentaux

### 1. Périmètre des Modifications
- **Implémenter uniquement** ce qui est explicitement demandé dans le prompt
- **Ne pas ajouter** de fonctionnalités supplémentaires non mentionnées
- **Ne pas modifier** le code au-delà du strict nécessaire pour la demande

### 2. Protection des Fonctionnalités Existantes
- **Interdiction absolue** de supprimer ou modifier une fonctionnalité déjà présente
- Si une modification risque d'affecter une fonctionnalité existante, signaler immédiatement
- Préserver la compatibilité avec le code existant

### 3. Gestion des Ambiguïtés
- **En cas de doute**, toujours informer l'utilisateur explicitement
- **Proposer des suggestions claires** avec les différentes interprétations possibles
- **Attendre la validation** de l'utilisateur avant de procéder
- **Ne jamais décider unilatéralement**, particulièrement pour :
  - Les modifications de fonctionnalités existantes
  - Les changements d'architecture
  - Les suppressions de code

---

## Architecture Frontend / Backend

### 4. Séparation des Couches
- **Frontend** : Ne pas modifier, préserver intégralement
- **Backend** : Adapter selon les exigences de données du frontend
- Le backend doit être conforme aux contrats de données attendus par le frontend

### 5. Gestion des Incompatibilités
- **En cas de problème ou incompatibilité** entre frontend et backend :
  - Proposer des suggestions explicites à l'utilisateur
  - Détailler chaque option et ses implications
  - **Attendre la validation** avant de modifier le code
  - L'utilisateur décide si une modification existante doit être affectée

### 6. Modifications Non Demandées
- **Aucune modification** au-delà de ce qui est demandé
- Si une autre modification s'avère **nécessaire ou importante** :
  - **Informer l'utilisateur immédiatement**
  - **Justifier pourquoi** cette modification est requise
  - **Attendre la validation** avant de procéder

---

## Design Patterns

### 7. Conformité aux Patterns
- **Pattern Frontend** : Obligatoire (à respecter scrupuleusement)
- **Pattern Backend** : Obligatoire (à respecter scrupuleusement)
- Toute modification doit **respecter les patterns définis**
- Ne pas déroger aux patterns sans validation explicite

---

## Résumé
Seules les modifications explicitement validées doivent être implémentées. Le backend s'adapte au frontend, jamais l'inverse. Les design patterns sont non-négociables. La clarification et la validation de l'utilisateur sont prioritaires.