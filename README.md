# projet_reseau

Réalisé par Thomas Malidin-Delabrière et Lucas Tasseaux
Ce projet est réalisé dans le cadre de nos études à l'école Polytechnique de Montréal.  
La consigne était de créer une architecture client serveur afin de réaliser un modèle de drive (upload/download de fichier)

### Fonctionnalités
Connexion Client-Serveur multithread avec gestion des erreurs et pattern (adresse IP et port).

##### Connexion
Le service fonctionne avec un serveur et un client qu'il faut lancer respectivement dans cet ordre. Du côté du serveur est demandé de renseigné le port d'écoute (5000-5050).
Du côté client, on renseigne alors l'ip du serveur (en local : 127.0.0.1) et le port d'écoute. 
Il est possible de lancer plusieurs clients qui vont se connecter séparement sur plusieurs thread. 

#### Commande
Pour gérér les commandes du drive, nous avons renseigné plusieurs commandes possibles permettant d'accéder à un dossier et de naviguer dans l'arborescence du serveur.
Pour plus de lisibilité, nous avons créer un drive séparé par chaque client. 
Commandes possibles : 
  * cd <Nom d’un répertoire sur le serveur>
  * ls
  * mkdir <Nom du nouveau dossier>
  * upload <Nom du fichier>
  * download <Nom du fichier>
  * exit
  
Chaque commande peut être exécutée à n'importe quel moment sur n'importe quel client


### Fontionnalités secondaires

Débuggage côté serveur. 
A chaque fois qu'un client execute une commande, une ligne contenant l'adresse IP, le port utilisé et la commande exécutée est affichée côté serveur. 

-----------

## Déployer en local
Pour rédployer ce projet, il faut créer deux environnements JAVA distincts. Un pour le serveur et un pour le client. 

## Utiliser sur la console
Des fichiers .jar ont été générés afin de pouvoir utiliser directement le serveur et le/les client(s) depuis une console. 
Naviguer vers le dossier dans lequel se trouvent les .jar et taper : java -jar serveur.jar / java -jar client.jar
