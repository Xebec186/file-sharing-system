import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Client is a Command Line Interface (CLI) application for communicating with the
 * file-sharing Server, allowing users to list, upload, and download files.
 */
public class Client {

    // 50 MB Limit (50 * 1024 * 1024 bytes) for uploads to protect server storage
    private static final int MAX_FILE_SIZE = 50 * 1024 * 1024;

    static Scanner scanner = null;
    static Socket socket = null;
    static DataOutputStream dataOutputStream = null;
    static DataInputStream dataInputStream = null;

    public static void main(String[] args) {
        System.out.println("Connecting to server...");
        try {
            // Establish TCP connection to server on port 4000
            socket = new Socket("localhost", 4000);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
            System.out.println("Connected to server successfully.\n");

            scanner = new Scanner(System.in);
            int userChoice = 0;

            // Simple event loop for CLI menu options
            while(userChoice != 3) {
                displayMenu();
                if (scanner.hasNextInt()) {
                    userChoice = scanner.nextInt();
                } else {
                    scanner.next(); // Consume invalid non-integer input
                    userChoice = 0;
                }

                // Consume trailing newline in input buffer
                scanner.nextLine();

                switch (userChoice) {
                    case 1:
                        System.out.print("Enter absolute path of file: ");
                        String path = scanner.nextLine();
                        uploadFile(path);
                        break;
                    case 2:
                        listServerFiles();
                        System.out.print("Enter name of file to download (or press Enter to cancel): ");
                        String fileName = scanner.nextLine();
                        if (!fileName.trim().isEmpty()) {
                            downloadFile(fileName);
                        }
                        break;
                    case 3:
                        // Notify the server about disconnection and release socket
                        dataOutputStream.writeUTF("EXIT");
                        dataOutputStream.close();
                        socket.close();
                        break;
                    default:
                        System.out.println("Invalid input. Try again.\n");
                }
            }

        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

    private static void displayMenu() {
        System.out.println("=========MAIN MENU=========");
        System.out.println("1. Upload file to server");
        System.out.println("2. View uploaded files");
        System.out.println("3. Exit");
        System.out.print("Enter choice: ");
    }

    /**
     * Sends the LIST command to the server and prints the returned raw file array string.
     */
    private static void listServerFiles() {
        try {
            dataOutputStream.writeUTF("LIST");
            System.out.println(dataInputStream.readUTF());
        } catch (IOException e) {
            System.out.println("An error occurred in listing server files: " + e.getMessage());
        }
    }

    /**
     * Uploads a local file to the server.
     * Protocol sent:
     * 1. String: "UPLOAD"
     * 2. String: fileName
     * 3. int: fileLength
     * 4. raw bytes: file payload
     */
    private static void uploadFile(String path) {
        File file = new File(path);
        if(!file.exists() || !file.isFile()) {
            System.out.println("File with path " + path + " does not exist or is not a file.\n");
            return;
        }

        // Validate file size before uploading
        if(file.length() > MAX_FILE_SIZE) {
            System.out.println("Error: File exceeds the maximum size limit of " + (MAX_FILE_SIZE / (1024 * 1024)) + "MB.\n");
            return;
        }

        try {
            // Write upload headers
            dataOutputStream.writeUTF("UPLOAD");
            dataOutputStream.writeUTF(file.getName());
            dataOutputStream.writeInt((int) file.length());

            // Stream actual file bytes
            try (FileInputStream fileInput = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInput.read(buffer)) != -1) {
                    dataOutputStream.write(buffer, 0, bytesRead);
                }
            }

            dataOutputStream.flush();
            System.out.println("File uploaded successfully\n");
        } catch (IOException e) {
            System.out.println("An error occurred in uploading file: " + e.getMessage());
        }
    }

    /**
     * Downloads a file from the server and saves it in a dedicated client directory.
     * Protocol:
     * 1. Write String "DOWNLOAD" and String fileName
     * 2. Read int fileLength (-1 if not exists)
     * 3. Stream incoming bytes if found
     */
    private static void downloadFile(String fileName) {
        try {
            dataOutputStream.writeUTF("DOWNLOAD");
            dataOutputStream.writeUTF(fileName);

            int fileLength = dataInputStream.readInt();
            if (fileLength == -1) {
                System.out.println("File does not exist on server with name: " + fileName + "\n");
                return;
            }

            // Save inside a folder unique to this connection port (e.g. client_63686)
            File file = new File("client_" + socket.getLocalPort() + "/" + fileName);
            File clientFolder = file.getParentFile();
            if(!clientFolder.exists()) {
               boolean isFolderCreated = clientFolder.mkdirs();
               if(!isFolderCreated) {
                   return;
               }
            }
            file.createNewFile();

            // Stream the bytes from network stream to the local file output stream
            try(FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int totalBytesRead = 0;

                while(totalBytesRead < fileLength) {
                    int bytesToRead = Math.min(buffer.length, fileLength - totalBytesRead);
                    int bytesRead = dataInputStream.read(buffer, 0, bytesToRead);
                    if(bytesRead == -1) {
                        return;
                    }
                    fileOutputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
            }

            System.out.println("File successfully downloaded to " + file.getAbsolutePath() + "\n");
        } catch (IOException e) {
            System.out.println("An error occurred in downloading file: " + e.getMessage());
        }
    }
}