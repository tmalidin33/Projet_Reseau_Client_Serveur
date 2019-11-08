
import java.io.BufferedInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//ClientHandler hérite d'un Thread pour gérer plusieurs clients en même temps
public class ClientHandler extends Thread {

    private final Socket socket;
    private final int clientNumber;
    private PrintWriter writer = null;
    private BufferedInputStream reader = null;
    private Path origine;
    private Path currentDir;

    public ClientHandler(Socket socket, int clientNumber) {
        this.socket = socket;
        this.clientNumber = clientNumber;
        System.out.println("Nouvelle connexion avec client#" + clientNumber + " avec " + socket);
    }

    @Override
    public void run() {
        //Verifie si la connexion au socket est fermée
        boolean closeConnexion = false;

        //On crée un chemin vers le dossier de chaque client 
        origine = Paths.get("./Drive/client" + clientNumber).toAbsolutePath().normalize();

        try {
            Path newDir = Files.createDirectory(origine);
            currentDir = newDir;
            System.out.format("Création du dossier client%d\n", clientNumber);
        } catch (FileAlreadyExistsException e) {
            // Le dossier existe déjà.
            System.out.format("Le dossier client%d existe déjà\n", clientNumber);
            currentDir = Paths.get("./Drive/client" + clientNumber + "/");
        } catch (IOException e) {
            //Quelque chose ne va pas 
            e.printStackTrace();
        }

        // Tant que la connexion n'est pas fermée 
        while (!closeConnexion) {
            try {
                writer = new PrintWriter(socket.getOutputStream());
                reader = new BufferedInputStream(socket.getInputStream());
                //on attend la réponse du client
                String reponse = read();
                String commande = null;
                String args = null;
                //Commande qui devrait recevoir un argument, on renvoi une commande inconnue
                if (reponse.trim().equals("mkdir") || reponse.trim().equals("upload") || reponse.trim().equals("download") || reponse.trim().equals("cd")) {
                    commande = "";
                } //Commande avec argument, on sépare la commande et l'argument de la commande
                else if (reponse.startsWith("mkdir ") || reponse.startsWith("upload ") || reponse.startsWith("download ") || reponse.startsWith("cd ")) {
                    String[] parts = reponse.split(" ", 2);
                    commande = parts[0];
                    args = parts[1];
                } //Commande sans argument
                else {
                    commande = reponse;
                }

                log(reponse);
                //On traite la demande du client en fonction de la commande envoyée
                String toSend = "";

                switch (commande) {
                    case "cd":
                        String chemin = args;
                        Path previousPath = currentDir;
                        currentDir = cd(chemin);
                        System.out.println(currentDir.toString());
                        if (!Files.exists(currentDir) || !Files.isDirectory(currentDir)) {
                            currentDir = previousPath;
                            String[] partPath = currentDir.toString().split("/");
                            toSend = "Le dossier " + args + " n'existe pas\n" + "Vous êtes dans le dossier " + partPath[partPath.length - 1];
                        } else {
                            String[] partPath = currentDir.toString().split("/");
                            toSend = "Vous êtes dans le dossier " + partPath[partPath.length - 1];
                        }
                        break;
                    case "ls":

                        try (Stream<Path> walk = Files.walk(currentDir, 1)) {
                            List<String> result = walk.map(x -> x.toString()).collect(Collectors.toList());
                            if (result.size() == 1) {
                                toSend = "Dossier vide";
                            } else {
                                result.remove(0);
                                for (String item : result) {
                                    File file = new File(item);
                                    //N'affiche pas les fichiers cachés
                                    if (!file.isHidden()) {
                                        String[] parts = item.split("/");
                                        if (file.isDirectory()) {
                                            toSend += "[Folder]" + parts[parts.length - 1] + "\n";
                                        } else if (file.isFile()) {
                                            toSend += "[File]" + parts[parts.length - 1] + "\n";
                                        }
                                    }
                                }
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "mkdir":
                        Path test_path = Paths.get(currentDir.toString(), args);
                        try {
                            Files.createDirectory(test_path);
                            toSend = "Le dossier " + args;
                            toSend += " a été créé.";
                        } catch (FileAlreadyExistsException e) {
                            // the directory already exists.
                            toSend = "Le dossier " + args;
                            toSend += " existe déjà !";
                        } catch (IOException e) {
                            //something else went wrong
                            e.printStackTrace();
                        }

                        break;
                    case "download":
                        File file = new File(currentDir.toString() + "/" + args);
                        Socket socketDo = new Socket("localhost", 6001);
                        ObjectOutputStream oos = new ObjectOutputStream(socketDo.getOutputStream());

                        // Vérifie et envoi true si c'est un fichier transférable
                        oos.writeObject(file.exists());

                        if (!file.exists()) {
                            toSend = "Téléchargement impossible, fichier inexistant !";
                        } else {

                            oos.writeObject(file.isFile());
                            if (file.isDirectory()) {
                                toSend = "Impossible de télécharger un dossier";
                            } else {
                                oos.writeObject(file.getName());
                                FileInputStream fis = new FileInputStream(file);
                                byte[] buffer = new byte[100];
                                Integer bytesRead = 0;

                                while ((bytesRead = fis.read(buffer)) > 0) {
                                    oos.writeObject(bytesRead);
                                    oos.writeObject(Arrays.copyOf(buffer, buffer.length));
                                }
                                toSend = "Download terminé";
                            }
                        }

                        oos.close();
                        socketDo.close();

                        break;
                    case "upload":

                        try {
                            ServerSocket serverSocket = null;
                            serverSocket = new ServerSocket();
                            serverSocket.setReuseAddress(true);
                            InetAddress serveurIP = InetAddress.getByName("127.0.0.1");
                            int newPort = 6000;
                            serverSocket.bind(new InetSocketAddress(serveurIP, newPort));
                            Socket s = serverSocket.accept();
                            saveFile(s);
                            try {
                                serverSocket.close();
                            } catch (IOException e) {
                                System.err.println("Couldn't close a socket, what going on ?");
                            }

//                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        toSend = "Upload terminé";
                        break;

                    case "exit":
                        toSend = "Communication terminée";
                        closeConnexion = true;
                        break;
                    case "":
                        toSend = "--------\n";
                        toSend += "Cette commande sans argument n'est pas supportee\n";
                        toSend += "Créer un dossier : \"mkdir <Nom du dossier>\"\n";
                        toSend += "Naviguer dans l'arborescence : \"cd <Nom du chemin>\"\n";
                        toSend += "Lister les fichiers : \"ls\"";
                        toSend += "Downloader un fichier : \"download <Nom du fichier>\"\n";
                        toSend += "Upload un fichier local : \"upload <Nom du fichier>\"\n";
                        toSend += "Fermer serveur : \"quit\"\n";
                        toSend += "--------";

                        break;
                    default:
                        toSend = "--------\n";
                        toSend += "Cette commande est inconnue\n";
                        toSend += "Créer un dossier : \"mkdir <Nom du dossier>\"\n";
                        toSend += "Naviguer dans l'arborescence : \"cd <Nom du chemin>\"\n";
                        toSend += "Lister les fichiers : \"ls\"";
                        toSend += "Downloader un fichier : \"download <Nom du fichier>\"\n";
                        toSend += "Upload un fichier local : \"upload <Nom du fichier>\"\n";
                        toSend += "Fermer serveur : \"quit\"\n";
                        toSend += "--------";
                        break;
                }
                writer.write(toSend);
                writer.flush();
                reader = null;

                //Vérifie si on ferme la connexion
                if (closeConnexion) {
                    System.err.println("Connection with client#" + clientNumber + " closed");
                    writer = null;
                    reader = null;
                    try {
                        socket.close();
                    } catch (IOException e) {
                        System.err.println("Couldn't close a socket, what going on ?");
                    }
                    break;
                }
            } catch (IOException e) {
                System.err.println("Error handling client#" + clientNumber + ": " + e);
            }
        }
    }

//Envoi le log au serveur 
    private void log(String reponse) {
        //Formatage de l'objet date
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        SimpleDateFormat hf = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        String dateF = df.format(date);
        String heureF = hf.format(date);
        InetSocketAddress remote = (InetSocketAddress) socket.getRemoteSocketAddress();

        //On affiche les informations du client et du message
        System.out.println("[" + remote.getAddress().getHostAddress() + ":" + remote.getPort() + " - " + dateF + "@" + heureF + "]" + ":" + reponse);
    }

    //La méthode que nous utilisons pour lire les réponses
    private String read() throws IOException {
        String response = "";
        int stream;
        byte[] b = new byte[4096];
        stream = reader.read(b);
        response = new String(b, 0, stream);
        return response;
    }

    public Path cd(String newPath) {
        currentDir = currentDir.toAbsolutePath().normalize();

        if ("".equals(currentDir.toString()) || "".equals(newPath) || !currentDir.startsWith("/")) {
            return currentDir;
        } else if (newPath.startsWith("/")) {
            currentDir = origine;
            return this.cd(newPath.length() > 1 ? newPath.substring(1) : "");
        } else {
            if (newPath.startsWith("...")) {
                return currentDir;
            } else if (newPath.startsWith("../")) {
                this.cdup1();
                return cd(newPath.length() > 3 ? newPath.substring(3) : "");
            } else if (newPath.startsWith("./")) {
                return this.cd(newPath.length() > 2 ? newPath.substring(2) : "");
            } else if (newPath.startsWith("..")) {
                this.cdup1();
                return this.cd(newPath.length() > 2 ? newPath.substring(2) : "");
            } else if (newPath.startsWith(".")) {
                return this.cd(newPath.length() > 1 ? newPath.substring(1) : "");
            } else {
                String[] strarr = newPath.split("/");
                if (currentDir.endsWith("/")) {
                    currentDir = Paths.get(currentDir.toString() + strarr[0]);
                } else {
                    currentDir = Paths.get(currentDir.toString() + "/" + strarr[0]);
                }
                String newPath2 = "";
                for (int i = 1; i < strarr.length; i++) {
                    String s = strarr[i];
                    newPath2 = newPath2 + s + "/";
                }
                return this.cd(newPath2);
            }
        }
    }

    private void cdup1() {
        if ("/".equals(currentDir.toString())) {
            return;
        }
        String[] patharr;
        patharr = currentDir.toString().split("/");
        StringBuilder s = new StringBuilder("/");
        for (int i = 0; i < patharr.length - 1; i++) {
            if (!"".equals(patharr[i])) {
                s.append(patharr[i]).append("/");
            }
        }
        String endpath = s.toString();
        if (!"/".endsWith(endpath) && endpath.endsWith("/")) {
            endpath = endpath.substring(0, endpath.length() - 1);
        }
        currentDir = Paths.get(endpath).normalize();
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
                        file = new File(currentDir.toString(), o.toString());
                        fos = new FileOutputStream(file);
                    } else {
                        throwException("Something is wrong");
                    }

                    // 2. Read file to the end.
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

                        // 3. Write data to output file.
                        fos.write(buffer, 0, bytesRead);

                    } while (bytesRead == 100);

                    System.out.println("File transfer success");

                    fos.close();
                }
            }
//        oos.close();
        }
    }

    public static void throwException(String message) throws Exception {
        throw new Exception(message);
    }
}
