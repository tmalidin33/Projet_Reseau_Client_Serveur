import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {

    private static Socket socket = null;

        
    public static void main(String[] args) throws Exception {
        try {
            //Pattern d'une adresse IP 
            String IPADDRESS_PATTERN
                    = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                    + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                    + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                    + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
            Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);

            //Adresse du serveur
            Matcher matcher;
            String serveurAdresse;
            do {
                Scanner sa = new Scanner(System.in);
                System.out.println("Veuillez entrer l'IP du serveur :");
                serveurAdresse = sa.nextLine();
                matcher = pattern.matcher(serveurAdresse);
            } while (!matcher.find());
            InetAddress serveurIP = InetAddress.getByName(serveurAdresse);

            //Demande du port du serveur
            //Pattern d'une adresse IP 
            String serveurPort;
            String port = "\\b(50[0-4][0-9]|5050)\\b";
            Pattern patternPort = Pattern.compile(port);

            //Adresse du serveur
            Matcher matcherPort;
            Scanner sc = new Scanner(System.in);
            do {
                System.out.println("Veuillez entrer le port d'écoute (entre 5000 et 5050) :");
                serveurPort = sc.nextLine();
                matcherPort = patternPort.matcher(serveurPort);
            } while (!matcherPort.find());
            int newPort = Integer.parseInt(serveurPort);
//            int newPort = Integer.parseInt("5000");
            socket = new Socket(serveurIP, newPort);
//            socket = new Socket("127.0.0.1", newPort);
            System.out.format("The serveur is running on %s:%d%n", serveurAdresse, newPort);
//            System.out.format("The serveur is running on %s:%d%n", "127.0.0.1", newPort);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
            new Commandes(socket);
    }

    
}
