import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

public class Server {

    static DataInputStream dataInputStream = null;
    static DataOutputStream dataOutputStream = null;

    public static void main(String[] args) {
        try(ServerSocket server = new ServerSocket(4000)) {
            System.out.println("Server started on port 4000. Awaiting connections...");
            while (true) {
                try {
                    Socket client = server.accept();
                    System.out.println("Client connected");

                    dataInputStream = new DataInputStream(client.getInputStream());
                    dataOutputStream = new DataOutputStream(client.getOutputStream());
                    String command = "";

                    while(!command.equals("EXIT")) {
                        command = dataInputStream.readUTF();
                        switch (command) {
                            case "UPLOAD":
                                receiveFile();
                                break;
                            case "LIST":
                                sendFileNames();
                                break;
                            case "DOWNLOAD":
                                sendFile();
                                break;
                            case "EXIT":
                                break;
                            default:
                                System.out.println("Client sent invalid option.");
                                dataOutputStream.writeUTF("Invalid option. Valid options are UPLOAD, LIST, DOWNLOAD, EXIT");
                        }
                    }
                    client.close();
                    System.out.println("Client disconnected");
                } catch (EOFException e) {
                    System.out.println("Client disconnected abruptly.");
                } catch (IOException e) {
                    System.out.println("Error handling client session: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void receiveFile() {
        try {
            String fileName = dataInputStream.readUTF();
            int fileLength = dataInputStream.readInt();

            // Check if the server folder exists and create if not
            File file = new File("server/" + fileName);
            File serverFolder = file.getParentFile();
            if(serverFolder != null &&  !serverFolder.exists()) {
                boolean isFolderCreated = serverFolder.mkdirs();
                if(!isFolderCreated) {
                    System.out.println("Failed to receive file from client.");
                    System.out.println("Server folder could not be created. Try creating it manually and try again");
                    return;
                }
            }

            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int totalBytesRead = 0;

                while (totalBytesRead < fileLength) {
                    // Safeguard against buffer overflow and calculate exact remaining bytes
                    int bytesToRead = Math.min(buffer.length, fileLength - totalBytesRead);
                    int bytesRead = dataInputStream.read(buffer, 0, bytesToRead);

                    // Safeguard against unexpected stream closure (-1)
                    if (bytesRead == -1) {
                        throw new EOFException("Client disconnected before file transfer completed.");
                    }

                    // Write each chunk immediately to avoid data loss
                    fileOutputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
            }

            System.out.println("File successfully received and saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Error occurred in receiving file from client: " + e.getMessage());
            e.printStackTrace();
        }

    }

    private static void sendFileNames() {
        // Check if the server folder exists and create if not
        File serverFolder = new File("server");
        if(!serverFolder.exists()) {
            boolean isFolderCreated = serverFolder.mkdir();
            if(!isFolderCreated) {
                System.out.println("Server folder could not be created.");
                try {
                    dataOutputStream.writeUTF("[]");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }

        File[] files = serverFolder.listFiles();
        List<String> fileNames = List.of();
        if (files != null) {
            fileNames = Arrays.stream(files)
                    .map(File::getName)
                    .toList();
        }

        try {
            dataOutputStream.writeUTF(fileNames.toString());
        } catch (IOException e) {
            System.out.println("An error occurred in sending file names: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sendFile() {
        try {
            String fileName = dataInputStream.readUTF();
            File file = new File("server/" + fileName);
            if(!file.exists() || !file.isFile()) {
                dataOutputStream.writeInt(-1);
                return;
            }

            dataOutputStream.writeInt((int) file.length());

            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    dataOutputStream.write(buffer, 0, bytesRead);
                }
            }
            dataOutputStream.flush();
        } catch (IOException e) {
            System.out.println("An error occurred in sending file to client: " + e.getMessage());
            e.printStackTrace();
        }
    }
}