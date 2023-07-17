import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class ClientC {
    
    public static void main(String[] args) {

        String directoryPath = Constants.CLIENT3_FILE_ROOT;
        String serverHost = "localhost";
        int serverPort = Constants.SERVER_TCP_PORT;
        // create the directory if it doesn't exist
        File directory = new File(directoryPath);
		if (!directory.exists()) {
			if (directory.mkdir()) {
				System.out.println("Directory is created!");
			} else {
				System.out.println("Failed to create directory!");
			}
		}
        
        String action = "LOCALCHECK";
        String suggestedFileName = "";
        String[] localFiles = directory.list();
        String algorithm = "SHA-256";

        do{
            

            InetAddress serverIp;
            Socket tcpSocket;
            Scanner inputSocket;
            PrintWriter outputSocket;
            String message;
            String line;
            InetAddress receiverIp;
            InetAddress senderIp;
            int receiverPort;
            int senderPort;
            //Connect to the server
            try{
                serverIp=InetAddress.getByName(serverHost);			
				tcpSocket = new Socket(serverIp, serverPort);

                inputSocket = new Scanner(tcpSocket.getInputStream());
                outputSocket = new PrintWriter(tcpSocket.getOutputStream(), true);

            }catch(Exception e){
                System.out.println("Error connecting to server");
                continue;
            }

            //Send the message
            switch (action) {
                case "FILELIST":
                    message = "FILELIST";
                    

                    //Senc the message
                    System.out.println("Sending message: "+message);
                    try{
                        outputSocket.println(message+Constants.CRLF);
                        outputSocket.println("STOP"+Constants.CRLF);
                    }catch(Exception e){
                        System.out.println("Error sending message");
                        break;
                    }
                    
                    String[] files = new String[100];
                    int index = 0;
                    String[] hashes = new String[100];
                    long[] times = new long[100];
                    //Receive the message
                    try{
                        line = inputSocket.nextLine();
                        while(!line.equals("STOP")){
                            if (line.isEmpty()) {
                                line = inputSocket.nextLine();
                                continue;
                            } else if (line.startsWith("DELETE")){
                                System.out.println("Received message: "+line);

                                String lastDeletedFile = line.split(":")[1];
                                File file = new File(directoryPath+lastDeletedFile);
                                if (file.exists()){
                                    file.delete();
                                    System.out.println("Deleted file: "+lastDeletedFile);
                                }
                            } else {


                                String fileN = line.split(":")[0];
                                String hash = line.split(":")[1];
                                long time = Long.parseLong(line.split(":")[2]);
                                hashes[index] = hash;
                                files[index] = fileN;
                                times[index] = time;
                                index++;
                                System.out.println("Received message: "+line);
                            }
                            line = inputSocket.nextLine();
                        }
                        
                    }catch(Exception e){
                        System.out.println(e.getMessage());
                        System.out.println("Error receiving message");
                        break;
                    }

                    //Get FILELIST from local directory
                   

                    //Process the FILELIST
                    System.out.println("Processing files...");
                    for (int i = 0; i < index; i++) {
                        boolean found = false;
                        boolean fileChanged = false;
                        for (int j = 0; j < localFiles.length; j++) {
                            if (files[i].equals(localFiles[j])){
                                found = true;
                                File tempFile = new File(directoryPath+files[i]);
                                String tempHash = "";

                                try {
                                    byte[] fileHash = calculateHash(Constants.CLIENT3_FILE_ROOT + files[i], algorithm);
                                    tempHash = bytesToHex(fileHash);
                                    System.out.println("Hash: " + tempHash);
                                } catch (NoSuchAlgorithmException | IOException e) {
                                    e.printStackTrace();
                                }
                                if (!tempHash.equals(hashes[i])){
                                    System.out.println("File Changed");
                                    if (times[i] > tempFile.lastModified()){
                                        action = "DOWNLOAD";
                                        suggestedFileName = files[i];
                                        fileChanged = true;
                                        break;}
                                    if (times[i] < tempFile.lastModified()){
                                        action = "UPLOAD";
                                        suggestedFileName = files[i];
                                        fileChanged = true;
                                        break;
                                    }
                                }
                                break;
                                
                            }
                        }
                        if (!found){
                            action = "DOWNLOAD";
                            suggestedFileName = files[i];
                            List<String> templist = new ArrayList<String>(Arrays.asList(localFiles));
                            templist.add(files[i]);
                            localFiles = templist.toArray(new String[0]);
                            break;
                        }
                        if (fileChanged){
                            break;
                        }
    
                    }
                    if (!action.equals("FILELIST")){
                        System.out.println("File Changed");
                        break;
                    }
                    action = "LOCALCHECK";
                    //delay
                    try{
                        Thread.sleep(500);
                    }catch(Exception e){
                        System.out.println("Error sleeping");
                    }
                    break;
                    
                case "LOCALCHECK":
                    //Get FILELIST from local directory
                    String[] currentLocalFiles = directory.list();
                    //Process the FILELIST to check if file wa deleted
                    System.out.println("Processing files...");
                    for (int i = 0; i < localFiles.length; i++) {
                        boolean found = false;
                        for (int j = 0; j < currentLocalFiles.length; j++) {
                            if (localFiles[i].equals(currentLocalFiles[j])){
                                found = true;
                                break;
                            }
                        }
                        if (!found){
                            action = "DELETE";
                            suggestedFileName = localFiles[i];
                            List<String> templist = new ArrayList<String>(Arrays.asList(localFiles));
                            templist.remove(localFiles[i]);
                            localFiles = templist.toArray(new String[0]);
                            break;
                        
                        }
                    }
                    if (action.equals("DELETE")){
                        System.out.println("File Deleted");
                        break;
                    }
                    //Check the FileSystem for addition of files
                    for (int i = 0; i < currentLocalFiles.length; i++) {
                        boolean found = false;
                        for (int j = 0; j < localFiles.length; j++) {
                            if (currentLocalFiles[i].equals(localFiles[j])){
                                found = true;
                                break;
                            }
                        }
                        if (!found){
                            action = "UPLOAD";
                            suggestedFileName = currentLocalFiles[i];
                            List<String> templist = new ArrayList<String>(Arrays.asList(localFiles));
                            templist.add(currentLocalFiles[i]);
                            localFiles = templist.toArray(new String[0]);
                            break;
                        }
                    }
                    if (action.equals("UPLOAD")){
                        System.out.println("File Added");
                        break;
                    }

                    action = "FILELIST";
                    break;

                case "DOWNLOAD":
                    //Send the message
                    message = "DOWNLOAD";
                    System.out.println("Sending message: "+message);
                    //Connect to the server to download the file
                    receiverPort=Constants.CLIENT2_UDP_PORT;
                    while(isPortAvailable(receiverPort)==false) {
                        receiverPort++;
                    }
                    try{
                        outputSocket.println(message+Constants.CRLF);
                        outputSocket.println("DOWNLOAD PORT :" + receiverPort+Constants.CRLF);
                        outputSocket.println("DOWNLOAD FILENAME :"+suggestedFileName+Constants.CRLF);
                        outputSocket.println("STOP"+Constants.CRLF);

                    }catch(Exception e){
                        System.out.println("Error sending message");
                        break;
                    }
                    senderPort = 0;
                    boolean ready = false;
                    //Receive the message
                    try{
                        line = inputSocket.nextLine();
                        while(!line.equals("STOP")){
                            if (line.isEmpty()) {
                                line = inputSocket.nextLine();
                                continue;
                            } else{
                                if(line.startsWith("DOWNLOAD PORT")){
                                    senderPort = Integer.parseInt(line.split(":")[1]);
                                    ready = true;
                                    
                                }
                                System.out.println("Received message: "+line);
                            }
                            line = inputSocket.nextLine();
                        }
                        
                    }catch(Exception e){
                        System.out.println(e.getMessage());
                        System.out.println("Error receiving message");
                        break;
                    }
                    
                    if (!ready){
                        System.out.println("Sender port not ready");
                        break;
                    }
                    
                    //Receive the File


                    try{

                        receiverIp = InetAddress.getByName("localhost");
                        senderIp = tcpSocket.getInetAddress();

                    } catch (Exception e){
                        System.out.println("Error getting receiver IP");
                        break;
                    }
                    
                    receiveFile(receiverIp,receiverPort,senderIp, senderPort);

                    
                    action = "FILELIST";
                    break;
                case "UPLOAD":
                    //Send the message
                    message = "UPLOAD";
                    System.out.println("Sending message: "+message);
                    //Connect to the server to download the file
                    senderPort=Constants.CLIENT2_UDP_PORT;
                    while(isPortAvailable(senderPort)==false) {
                        senderPort++;
                    }
                    try{
                        outputSocket.println(message+Constants.CRLF);
                        outputSocket.println("UPLOAD PORT :" + senderPort+Constants.CRLF);
                        outputSocket.println("UPLOAD FILENAME :"+suggestedFileName+Constants.CRLF);
                        outputSocket.println("STOP"+Constants.CRLF);

                    }catch(Exception e){
                        System.out.println("Error sending message");
                        break;
                    }
                    receiverPort = 0;
                    ready = false;
                    //Receive the message
                    line = inputSocket.nextLine();
                    try{
                        while(!line.equals("STOP")){
                            if (line.isEmpty()) {
                                line = inputSocket.nextLine();
                                continue;
                            } else{
                                if(line.startsWith("UPLOAD PORT")){
                                    receiverPort = Integer.parseInt(line.split(":")[1]);
                                    ready = true;
                                    
                                }
                                System.out.println("Received message: "+line);
                            }
                            line = inputSocket.nextLine();
                        }
                        
                    }catch(Exception e){
                        System.out.println("Error receiving message");
                        break;
                    }
                    
                    if (!ready){
                        System.out.println("Receiver port not ready");
                        break;
                    }
                    
                    //Receive the File
                    try{

                        senderIp = InetAddress.getByName("localhost");
                        receiverIp = tcpSocket.getInetAddress();

                    } catch (Exception e){
                        System.out.println("Error getting sender IP");
                        break;
                    }
                    
                    sendFile(suggestedFileName,senderPort,senderIp,receiverIp,receiverPort);

                    action = "LOCALCHECK";
                    break;

                case "DELETE":
                    message = "DELETE";
                    System.out.println("Sending message: "+message);
                    try{
                        outputSocket.println(message+Constants.CRLF);
                        outputSocket.println("DELETE FILENAME :"+suggestedFileName+Constants.CRLF);
                        outputSocket.println("STOP"+Constants.CRLF);
                    } catch(Exception e){
                        System.out.println("Error sending message");
                        break;
                    }
                    action = "LOCALCHECK";
                    try{
                        Thread.sleep(1000);
                    }catch(Exception e){
                        System.out.println("Error sleeping");
                    }
                    break;
                default:
                    break;
            }

            try {
                inputSocket.close();
                outputSocket.close();
                tcpSocket.close();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                System.out.println("Error closing sockets");    
            }

        }while(true);
    }

    public static void sendFile(String fileName,int senderPort,InetAddress senderIp,InetAddress receiverIP,int receiverPort){
        PacketBoundedBufferMonitor bufferMonitor=new PacketBoundedBufferMonitor(Constants.MONITOR_BUFFER_SIZE);			
        PacketSender packetSender=new PacketSender(bufferMonitor,senderIp,senderPort,receiverIP,receiverPort);
        packetSender.start();
        FileReader fileReader=new FileReader(bufferMonitor,fileName, Constants.CLIENT3_FILE_ROOT);
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
        
        FileWriter fileWriter=new FileWriter(bm, Constants.CLIENT3_FILE_ROOT);
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
