Parfait 👍
Voici le README complet, harmonisé et prêt à rendre, avec un niveau TP, clair, logique, et cohérent avec ce que ton prof attend.
Tu peux le mettre tel quel dans ton dépôt Git.

⸻

📘 TP v2 – Haute Disponibilité & WatchDog

Duplication du Core Operational et Supervision

⸻

🎯 Objectif du TP

Mettre en place une architecture tolérante aux pannes autour du service Core Operational Backend en :
	•	déployant plusieurs instances du service
	•	surveillant leur disponibilité
	•	relançant automatiquement une instance en cas de défaillance
	•	démontrant le mécanisme lors d’une démo

L’architecture repose sur des services stateless, des conteneurs Docker, et un WatchDog applicatif.

⸻

🧱 Étape 1 – Duplication du service Core Operational

1.1 Principe

Le service core-operational est stateless :
il ne stocke aucun état local critique et ne dépend pas d’une instance spécifique.

Grâce à cette propriété, il peut être déployé plusieurs fois à l’identique, sans modification du code métier.

La duplication repose donc sur le déploiement de la même image Docker sur plusieurs machines virtuelles.

⸻

1.2 Duplication logique du service

La duplication du service ne consiste pas à dupliquer le code source.

On conserve :
	•	un seul dépôt Git
	•	un seul code source
	•	une seule image Docker

On déploie :
	•	deux instances identiques du service
	•	sur deux machines virtuelles distinctes

Nommage des instances :
	•	core-one
	•	core-two

⸻

1.3 Construction de l’image Docker

Une unique image Docker est construite à partir du projet :

docker build -t core-operational:latest .

Cette image est utilisée sans modification pour tous les déploiements.

⸻

1.4 Déploiement sur les machines virtuelles

L’image Docker est déployée :
	•	sur la VM vm-core-one
	•	sur la VM vm-core-two

Exemple de lancement :

docker run -d \
  --name core-one \
  -e INSTANCE_NAME=core-one \
  -p 8080:8080 \
  core-operational:latest

Même commande sur la seconde VM avec :

--name core-two
-e INSTANCE_NAME=core-two


⸻

1.5 Différences entre les instances

Les deux instances utilisent exactement la même image Docker.
Les seules différences concernent le contexte d’exécution.

Élément	core-one	core-two
Machine virtuelle	vm-core-one	vm-core-two
Nom du conteneur	core-one	core-two
Variable d’environnement	INSTANCE_NAME=core-one	INSTANCE_NAME=core-two
Adresse réseau	différente	différente

Il n’existe aucune différence :
	•	dans le code
	•	dans le binaire
	•	dans la logique métier

⸻

1.6 Justification du choix

Cette approche permet :
	•	une duplication simple et propre
	•	une haute disponibilité
	•	une meilleure tolérance aux pannes
	•	une démonstration claire du fonctionnement du WatchDog

Elle est rendue possible par :
	•	le caractère stateless du service
	•	l’utilisation de services externes partagés (cache Redis, base managée)

⸻

🖥️ Étape 2 – Création des machines virtuelles

2.1 Machines nécessaires

VM	Rôle
vm-core-one	Instance 1 du Core Operational
vm-core-two	Instance 2 du Core Operational
vm-watchdog	Supervision et relance


⸻

2.2 Pré-requis sur chaque VM
	•	Docker installé
	•	Accès SSH actif
	•	Connectivité réseau entre les VMs

⸻

❤️ Étape 3 – API de surveillance (/health)

3.1 Objectif

Permettre au WatchDog de vérifier que :
	•	le service est actif
	•	le processus applicatif fonctionne correctement

⸻

3.2 Implémentation

Le service Core expose une API simple :

GET /health

Réponse attendue :

{
  "status": "UP"
}


⸻

🐶 Étape 4 – Mise en place du WatchDog

4.1 Rôle du WatchDog

Le WatchDog est un service indépendant chargé de :
	1.	vérifier l’accessibilité des machines via SSH
	2.	vérifier l’état des services via l’API /health
	3.	relancer un service défaillant via Docker

⸻

4.2 Principe de fonctionnement

À intervalle régulier :
	•	test SSH sur chaque machine
	•	appel HTTP /health
	•	si une anomalie est détectée :
	•	exécution d’une commande de relance Docker
	•	journalisation de l’action

⸻

⚙️ Étape 5 – Configuration du WatchDog

5.1 Fichier de configuration

Un fichier de configuration unique, partagé et versionné dans Git :

watchdog:
  intervalSeconds: 10

coreInstances:
  - name: core-one
    healthUrl: http://core-one:8080/health
    sshHost: vm-core-one
    sshUser: toto
    restartCommand: docker restart core-one

  - name: core-two
    healthUrl: http://core-two:8080/health
    sshHost: vm-core-two
    sshUser: toto
    restartCommand: docker restart core-two


⸻

🎬 Étape 6 – Mode Démo

6.1 Objectif

Simuler une panne afin de visualiser la détection et la relance automatique.

⸻

6.2 Implémentation côté Core

Ajout d’un mode démo :

if (demoMode) {
    Thread.sleep(30000);
}


⸻

6.3 Démonstration attendue
	•	une instance cesse de répondre
	•	le WatchDog détecte l’anomalie
	•	le WatchDog relance le conteneur
	•	le service redevient disponible

⸻

✅ Conclusion

Ce TP met en œuvre :
	•	une duplication propre par image Docker
	•	une architecture stateless
	•	un mécanisme de supervision applicatif
	•	une relance automatique démontrable

Le choix d’un WatchDog développé en interne permet de maîtriser la logique de surveillance, la relance et la démonstration pédagogique.

⸻

Si tu veux, prochaine étape possible :
	•	🔹 checklist de démo orale (quoi arrêter, quoi montrer)
	•	🔹 diagramme d’architecture annoté
	•	🔹 pseudo-code exact du WatchDogScheduler

Dis-moi 👍