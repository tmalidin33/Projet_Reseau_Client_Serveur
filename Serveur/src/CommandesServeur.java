import java.io.*;

//** Classe qui gère les commandes tapées dans la console **
// implémentation de l'interface Runnable (une des 2 méthodes pour créer un thread)
class Commandes implements Runnable
{
  Serveur _serveur; // pour utilisation des méthodes de la classe principale
  BufferedReader _in; // pour gestion du flux d'entrée (celui de la console)
  String _strCommande=""; // contiendra la commande tapée
  Thread _t; // contiendra le thread

  //** Constructeur : initialise les variables nécessaires **
  Commandes(Serveur Serveur)
  {
    _serveur=Serveur; // passage de local en global
    // le flux d'entrée de la console sera géré plus pratiquement dans un BufferedReader
    _in = new BufferedReader(new InputStreamReader(System.in));
    _t = new Thread(this); // instanciation du thread
    _t.start(); // demarrage du thread, la fonction run() est ici lancée
  }

  //** Methode : attend les commandes dans la console et exécute l'action demandée **
  @Override
  public void run() // cette méthode doit obligatoirement être implémentée à cause de l'interface Runnable
  {
    try
    {
      // si aucune commande n'est tapée, on ne fait rien (bloquant sur _in.readLine())
      while ((_strCommande=_in.readLine())!=null)
      {
        if (_strCommande.equalsIgnoreCase("exit")) { // commande "quit" detectée ...
          System.err.println("Fermeture du serveur...");
          //TODO : fermer le client quand on ferme le serveur
          System.exit(0); // ... on ferme alors le serveur
        }else
        {
          // si la commande n'est pas "quit", on informe l'utilisateur et on lui donne une aide
          System.err.println("--------");
          System.err.println("Cette commande n'est pas supportee");
          System.err.println("Quitter : \"exit\"");
          System.err.println("--------");
        }
        System.out.flush(); // on affiche tout ce qui est en attente dans le flux
      }
    }
    catch (IOException e) {}
  }
}