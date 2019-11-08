
import java.io.*;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

//** Classe qui gère les commandes tapées dans la console **
// implémentation de l'interface Runnable (une des 2 méthodes pour créer un thread)
class Commandes implements Runnable {

    BufferedReader _in; // pour gestion du flux d'entrée (celui de la console)
    String _strCommande = ""; // contiendra la commande tapée
    Thread _t; // contiendra le thread
    Socket _socket; //contiendra le socket
    private static PrintWriter writer = null;
    private static BufferedInputStream reader = null;
    //** Constructeur : initialise les variables nécessaires **

    Commandes(Socket socket) {
        // le flux d'entrée de la console sera géré plus pratiquement dans un BufferedReader
        _socket = socket;
        _in = new BufferedReader(new InputStreamReader(System.in));
        _t = new Thread(this); // instanciation du thread
        _t.start(); // demarrage du thread, la fonction run() est ici lancée
    }

    //** Methode : attend les commandes dans la console et exécute l'action demandée **
    @Override
    public void run() // cette méthode doit obligatoirement être implémentée à cause de l'interface Runnable
    {
        try {
            // si aucune commande n'est tapée, on ne fait rien (bloquant sur _in.readLine())
            while ((_strCommande = _in.readLine()) != null) {
                writer = new PrintWriter(_socket.getOutputStream(), true);
                reader = new BufferedInputStream(_socket.getInputStream());
                // Commande upload avec argument
                if (_strCommande.startsWith("upload") && !_strCommande.trim().equals("upload")) {
                    writer.write(_strCommande);
                    writer.flush();
                    System.out.println("En attente du transfert...");
                    TimeUnit.SECONDS.sleep(1);
                    String file_name = _strCommande.split(" ")[1];
                    File file = new File(file_name);
                    Socket socketUp;
                    socketUp = new Socket("localhost", 6000);
                    try (
                            ObjectOutputStream oos = new ObjectOutputStream(socketUp.getOutputStream())) {
                        oos.writeObject(file.exists());

                        if (!file.exists()) {
                            System.err.println("Téléchargement impossible, fichier inexistant !");
                        } else {
                            oos.writeObject(file.isFile());
                            if (file.isDirectory()) {
                                System.err.println("Impossible de télécharger un dossier");
                            } else {
                                oos.writeObject(file.getName());
                                FileInputStream fis = new FileInputStream(file);
                                byte[] buffer = new byte[100];
                                Integer bytesRead = 0;

                                while ((bytesRead = fis.read(buffer)) > 0) {
                                    oos.writeObject(bytesRead);
                                    oos.writeObject(Arrays.copyOf(buffer, buffer.length));
                                }
                            }
                        }
                        String reponse = read();
                        System.out.println(reponse);
                    }
//                    ois.close();
                    socketUp.close();

                } else if (_strCommande.startsWith("download") && !_strCommande.trim().equals("download")) {
                    writer.write(_strCommande);
                    writer.flush();

                    try {

                        ServerSocket serverSocket = null;
                        serverSocket = new ServerSocket();
                        serverSocket.setReuseAddress(true);
                        InetAddress serveurIP = InetAddress.getByName("127.0.0.1");
                        int newPort = 6001;
                        serverSocket.bind(new InetSocketAddress(serveurIP, newPort));
                        Socket s = serverSocket.accept();
                        saveFile(s);
                        String reponse = read();
                        System.out.println(reponse);
                        try {
                            serverSocket.close();
                        } catch (IOException e) {
                            System.out.println("Couldn't close a socket, what going on ?");
                        }

//                            }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    writer.write(_strCommande);
                    writer.flush();

                    String reponse = read();
                    System.out.println(reponse);
                    if (_strCommande.equalsIgnoreCase("exit")) {
                        System.exit(0);
                    }
//        System.out.flush(); // on affiche tout ce qui est en attente dans le flux
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException ex) {
            Logger.getLogger(Commandes.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            Thread.currentThread().sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        writer.write("CLOSE");
        writer.flush();
        writer.close();

    }
    //Méthode pour lire les réponses du serveur

    private static String read() throws IOException {
        String response = "";
        int stream;
        byte[] b = new byte[4096];
        stream = reader.read(b);
        response = new String(b, 0, stream);
        return response;
    }

    private void saveFile(Socket socket) throws Exception {
        // 1. Vérifie si le fichier existe
        try (
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            // 1. Vérifie si le fichier existe
            Object fileExist = ois.readObject();
            if ((Boolean) fileExist == false) {
            } else {
                Object typeFile = ois.readObject();
                if ((Boolean) typeFile == false) {
                    
                } else {
                    FileOutputStream fos = null;
                    byte[] buffer = new byte[100];
                    // 2. Read file name.
                    Object o = ois.readObject();
                    File file = null;
                    if (o instanceof String) {
                        file = new File(Paths.get(".", "/Downloads").toAbsolutePath().normalize().toString(), o.toString());
                        fos = new FileOutputStream(file);
                    } else {
                        throwException("Something is wrong");
                    }
                    
                    // 3. Read file to the end.
                    Integer bytesRead = 0;
                    
                    do {
                        o = ois.readObject();
                        
                        if (!(o instanceof Integer)) {
                            throwException("Something is wrong");
                        }
                        
                        bytesRead = (Integer) o;
                        
                        o = ois.readObject();
                        
                        if (!(o instanceof byte[])) {
                            throwException("Something is wrong");
                        }
                        
                        buffer = (byte[]) o;
                        
                        // 4. Write data to output file.
                        fos.write(buffer, 0, bytesRead);
                        
                    } while (bytesRead == 100);
                    
                    System.out.println("File transfer success");
                    fos.close();
                }
            }
        }
    }

    public static void throwException(String message) throws Exception {
        throw new Exception(message);
    }
}
