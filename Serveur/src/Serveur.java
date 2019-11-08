
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Serveur {

    //On initialise les valeurs par défauts
    private String serverPort = "5000";
    private String serverAddress = "127.0.0.1";
    private static ServerSocket listener = null;

    public Serveur() {
        try {
            // Pattern d'une adresse IP 
            String port = "\\b(50[0-4][0-9]|5050)\\b";
            Pattern pattern = Pattern.compile(port);
            Matcher matcher;
            Scanner sc = new Scanner(System.in);

            // Vérification du pattern du port
            do {
                System.out.println("Veuillez entrer le port d'écoute (entre 5000 et 5050) :");
                serverPort = sc.nextLine();
                matcher = pattern.matcher(serverPort);
            } while (!matcher.find());

            // Création du socket avec la bonne adresse et le bon port
            listener = new ServerSocket();
            listener.setReuseAddress(true);
            InetAddress serveurIP = InetAddress.getByName(serverAddress);
            int newPort = Integer.parseInt(serverPort);
            listener.bind(new InetSocketAddress(serveurIP, newPort));
            System.out.format("Serveur est lancé sur %s:%d%n", serverAddress, newPort);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {

        int clientNumber = 0;
        Serveur Serveur = new Serveur();
        new Commandes(Serveur);
        try {
            while (true) {
                //Créer un nouveau thread pour chaque client connecté
                new ClientHandler(listener.accept(), clientNumber++).start();
            }
        } finally {
            listener.close();
        }
    }
}
