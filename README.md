# Decision Board

Comité de 4 agents (Carrière, Finances perso, Élections France + Arbitre) consultable depuis une app Android qui lit tes données FinVault et Maillage.

## Déroulé d'un débat
1. **Ouverture** : chaque expert expose sa thèse, ses arguments (notés 1-5) et pose des questions aux autres.
2. **Débat croisé** : chacun répond aux questions reçues, note la pertinence (0-10) des arguments des autres, révise sa position.
3. **Arbitrage** : l'arbitre tranche les désaccords et donne une recommandation, un niveau de confiance, les risques, ce qui ferait changer d'avis et les prochaines étapes.

## Architecture
```
App Android (Compose) ──ContentProvider──> FinVault / Maillage
        │  HTTPS + jeton, flux NDJSON
        ▼
Backend perso (Node) ──> API Claude (clé uniquement côté serveur)
```

## Backend
```
cd backend
export ANTHROPIC_API_KEY=...   # clé Anthropic
export APP_TOKEN=...           # secret long, identique dans l'app
node server.js                 # Node >= 18, aucune dépendance
```
Édite `profile.json` (priorités, objectifs, tolérance au risque ; renseigne notamment `priorites_pour_les_elections`, l'agent Élections ne déduit jamais ton vote de ton profil). À héberger derrière HTTPS (Caddy, nginx, Cloud Run...). Rien n'est journalisé.

## Compiler l'APK avec GitHub (sans ordinateur de développement)
1. Crée un dépôt **privé** sur GitHub et envoie-y le contenu de ce dossier (`.github/`, `android/`, `backend/`).
2. Dépôt > Settings > Secrets and variables > Actions > New repository secret :
   - `BACKEND_URL` (https://...) et `APP_TOKEN` (même valeur que le backend)
   - optionnel mais **nécessaire pour la permission « signature »** : `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. C'est un même keystore pour les 3 apps (Decision Board, FinVault, Maillage). Sans lui, chaque compilation utilise une clé différente et la lecture des données sera refusée.
3. Onglet Actions > « Build APK » > Run workflow (il se lance aussi à chaque push sur `main`).
4. Quand c'est vert, ouvre l'exécution, télécharge l'artefact `decision-board-apk` (zip contenant `app-debug.apk`) depuis ton téléphone, décompresse-le et installe l'APK (autorise « sources inconnues » pour ton navigateur / gestionnaire de fichiers).

## App Android (compilation locale, optionnelle)
1. Ouvre `android/` dans Android Studio (il génère le wrapper Gradle).
2. Ajoute dans `android/local.properties` :
   ```
   BACKEND_URL=https://ton-serveur.example
   APP_TOKEN=le_même_secret
   ```
3. Dans `AppBridge.kt`, remplace les URI `content://...` par celles de tes apps ; dans `AndroidManifest.xml`, vérifie les noms de package (`fr.jovida.finvault`, `fr.jovida.maillage` sont des placeholders).

## Côté FinVault / Maillage (à faire dans ces deux apps)
Elles doivent exposer un `ContentProvider` en lecture seule, protégé par une permission signature :
```xml
<permission android:name="fr.jovida.permission.READ_DECISION_DATA"
            android:protectionLevel="signature" />
<provider android:name=".DecisionDataProvider"
          android:authorities="fr.jovida.finvault.provider"
          android:exported="true"
          android:readPermission="fr.jovida.permission.READ_DECISION_DATA" />
```
`signature` impose que les trois apps soient signées avec la même clé : seules tes apps peuvent lire. Expose des **vues résumées** (soldes agrégés, répartition, échéances ; nombre et catégories de contacts...) plutôt que les tables brutes.

## Confidentialité
- Les données partent vers ton backend puis vers l'API Claude. `AppBridge` retire les colonnes dont le nom évoque IBAN, numéro, email, téléphone, mot de passe, etc. et limite à 200 lignes, mais le vrai filtrage doit se faire dans les providers.
- Désactive FinVault / Maillage d'une requête avec les pastilles de l'écran principal.

## Limites à connaître
- Le projet n'a pas été compilé ici (pas d'Android SDK) : attends-toi à de petits ajustements à la première synchro Gradle.
- Les agents n'ont pas d'accès web : pour l'agent Élections, dates, lieux de vote et programmes sont à vérifier sur service-public.fr et les sources officielles. Les agents ne remplacent pas un conseil agréé pour les décisions financières ou fiscales importantes.
