import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class ServerA {
    public ServerA(){

    }    
    public static void main(String[] args) {
        // create the directory if it doesn't exist
        File file = new File(Constants.SERVER_FILE_ROOT);
        if (!file.exists()) {
            if (file.mkdir()) {
                System.out.println("Directory is created!");
            } else {
                System.out.println("Failed to create directory!");
            }
        }
        //Create the server socket
        ServerSocket serverSocket;
        try{
            serverSocket = new ServerSocket(Constants.SERVER_TCP_PORT);
        }catch(Exception e){
            System.out.println("Error creating server socket");
            return;
        }
        Socket clientSocket = null;
        String algorithm = "SHA-256";
                String lastDeletedFile = "";

        do {
            try{
                //Wait for a client to connect
                clientSocket = serverSocket.accept();
                System.out.println("Client connected");

                //Create the input and output streams
                Scanner inputSocket = new Scanner(clientSocket.getInputStream());
                PrintWriter outputSocket = new PrintWriter(clientSocket.getOutputStream(), true);

                int receiverPort;
                int senderPort;
                InetAddress receiverIP;
                InetAddress senderIP;
                //Read the message
                String message = inputSocket.nextLine();
                String line = "";
                System.out.println("Received message: "+message);

                // while(!message.equals("STOP")){
                //     if (message.isEmpty()) {
                //         message = inputSocket.nextLine();
                //         continue;
                //     }
                    
                    //Process the message
                    switch(message){
                        case "FILELIST":
                            //Send the file list
                            File folder = new File(Constants.SERVER_FILE_ROOT);
                            File[] listOfFiles = folder.listFiles();
                            for (int i = 0; i < listOfFiles.length; i++) {
                                if (listOfFiles[i].isFile()) {
                                    String fileN = listOfFiles[i].getName();
                                    File tempfile = new File(Constants.SERVER_FILE_ROOT+fileN);
                                    String hashString = "";
                                    try {
                                        byte[] fileHash = calculateHash(Constants.SERVER_FILE_ROOT + fileN, algorithm);
                                        hashString = bytesToHex(fileHash);
                                        System.out.println("Hash: " + hashString);
                                    } catch (NoSuchAlgorithmException | IOException e) {
                                        e.printStackTrace();
                                    }
                                    outputSocket.println(fileN+":"+hashString +":"+tempfile.lastModified());
                                }
                            }
                            try{
                            System.out.println("Sending message: STOP after Delete");
                                                            System.out.println("Sending message: DELETE :"+lastDeletedFile);

                            if (!lastDeletedFile.equals("")) {
                                System.out.println("Sending message: DELETE :"+lastDeletedFile);
                                outputSocket.println("DELETE :"+lastDeletedFile);
                            }
                            System.out.println("Sending message: STOP after Delete");
                        }catch(Exception e){
                            System.out.println("Error sending message here");
                        }
                            outputSocket.println("STOP");

                            break;
                        case "DOWNLOAD":
                            receiverPort= 0;
                            senderPort = Constants.SERVER_UDP_PORT;
                            String fileName = "";
                            while(isPortAvailable(senderPort)==false) {
                                senderPort++;
                            }
                            boolean ready = false;
                            boolean ready2 = false;
                            //Send the file
                            while(!message.equals("STOP")){
                                System.out.println(message);
                                if (message.isEmpty()) {
                                    message = inputSocket.nextLine();
                                    continue;
                                }
                                
                                //Process the message
                                if(message.startsWith("DOWNLOAD PORT")){
                                    receiverPort = Integer.parseInt(message.split(":")[1]);
                                    System.out.println("Receiver port: "+receiverPort);
                                    ready = true;
                                    
                                }
                                if(message.startsWith("DOWNLOAD FILE")){
                                    fileName = message.split(":")[1];
                                    ready2 = true;
                                    
                                }
                                message = inputSocket.nextLine();
    
                            }
                            if (!ready || !ready2){
                                System.out.println("Invalid message");
                                break;
                            }

                            line = "DOWNLOAD PORT :"+senderPort; 
                            System.out.println("Sending message: "+line);
                            try {
                                outputSocket.println(line);
                                outputSocket.println("STOP");
                            } catch (Exception e) {
                                System.out.println("Error sending message");
                                break;
                            }


                            receiverIP=clientSocket.getInetAddress();
                            senderIP=InetAddress.getByName("localhost");
                            sendFile(fileName,senderPort,senderIP,receiverIP,receiverPort);
                            
                            break;
                        case "UPLOAD":
                            //Receive the file
                            ready = false;
                            ready2 = false;
                            senderPort = 0;
                            fileName = "";
                            receiverPort = Constants.SERVER_UDP_PORT;
                            while(isPortAvailable(receiverPort)==false) {
                                receiverPort++;
                            }
                            while(!message.equals("STOP")){
                                if (message.isEmpty()) {
                                    message = inputSocket.nextLine();
                                    continue;
                                }
                                
                                //Process the message
                                if(message.startsWith("UPLOAD PORT")){
                                    senderPort = Integer.parseInt(message.split(":")[1]);
                                    ready = true;
                                    
                                }
                                if(message.startsWith("UPLOAD FILE")){
                                    fileName = message.split(":")[1];
                                    ready2 = true;
                                    
                                }
                                message = inputSocket.nextLine();
                            }
                            if (!ready ||!ready2){
                                System.out.println("Invalid message");
                                break;
                            }
                            
                            line = "UPLOAD PORT :"+receiverPort;
                            try{
                                outputSocket.println(line);
                                outputSocket.println("STOP");
                            } catch (Exception e) {
                                System.out.println("Error sending message");
                                break;
                            }
                            senderIP=clientSocket.getInetAddress();
                            receiverIP=InetAddress.getByName("localhost");
                            receiveFile(receiverIP,receiverPort,senderIP,senderPort);
                            if (fileName.equals(lastDeletedFile)) {
                                lastDeletedFile = "";
                            }
                            break;
                        
                        case "DELETE":
                        //Receive the file
                            System.out.println("Deleting file");
                            fileName = "";
                            while(!message.equals("STOP")){
                                if (message.isEmpty()) {
                                    System.out.println(message);
                                    message = inputSocket.nextLine();
                                    continue;
                                }
                                
                                //Process the message
                                if(message.startsWith("DELETE FILENAME")){
                                    fileName = message.split(":")[1];
                                }
                                message = inputSocket.nextLine();
                            }
                            System.out.println(fileName);
                            File fileToDelete = new File(Constants.SERVER_FILE_ROOT+fileName);
                            if(fileToDelete.delete()){
                                lastDeletedFile = fileName;
                                System.out.println("File deleted successfully");
                            }else{
                                System.out.println("Failed to delete the file");
                            }
                            break;


                        default:
                            
                            break;
                    }
                //     message = inputSocket.nextLine();
                // }
            } catch(Exception e){
                System.out.println("Error communicating with client");
                // e.printStackTrace();
            } finally{
                try {
                    clientSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }while(true);
    }

    public static void sendFile(String fileName,int senderPort,InetAddress senderIp,InetAddress receiverIP,int receiverPort){
        PacketBoundedBufferMonitor bufferMonitor=new PacketBoundedBufferMonitor(Constants.MONITOR_BUFFER_SIZE);			
        PacketSender packetSender=new PacketSender(bufferMonitor,senderIp,senderPort,receiverIP,receiverPort);
        packetSender.start();
        FileReader fileReader=new FileReader(bufferMonitor,fileName, Constants.SERVER_FILE_ROOT);
        fileReader.start();
        try {
            packetSender.join();
            fileReader.join();				
        } catch (InterruptedException e) {}
    }
    public static void receiveFile(InetAddress receiverIp,int receiverPort,InetAddress senderIp,int senderPort) {
    
        PacketBoundedBufferMonitor bm=new PacketBoundedBufferMonitor(Constants.MONITOR_BUFFER_SIZE);


        PacketReceiver packetReceiver=new PacketReceiver(bm,receiverIp,receiverPort,senderIp,senderPort);
        packetReceiver.start();
        
        FileWriter fileWriter=new FileWriter(bm, Constants.SERVER_FILE_ROOT);
        fileWriter.start();	
        try {
            packetReceiver.join();
            fileWriter.join();
        } 
            catch (InterruptedException e) {e.printStackTrace();}
    }



    public static boolean isPortAvailable(int port) {
        try {
            DatagramSocket socket = new DatagramSocket(port);
            socket.close();
            return true;
        } catch (SocketException e) {
            // Port is already in use or unavailable
            return false;
        }
    }

	public static byte[] calculateHash(String filePath, String algorithm)
            throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        try (FileInputStream fis = new FileInputStream(filePath);
             DigestInputStream dis = new DigestInputStream(fis, digest)) {

            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) {
                // Read the file in chunks and update the hash calculation
                // The read method automatically updates the digest
            }

            // Calculate the final hash value
            digest = dis.getMessageDigest();
            return digest.digest();
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
