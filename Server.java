import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    static DataInputStream clientInput = null;

    public static void main(String[] args) {
        try(ServerSocket server = new ServerSocket(4000)) {
            System.out.println("Server started on port 4000. Awaiting connections...");
            Socket client = server.accept();
            System.out.println("Client connected");

            clientInput = new DataInputStream(client.getInputStream());
            String command = "";

            while(!command.equals("exit")) {
                command = clientInput.readUTF();
                if(command.equals("UPLOAD")) {
                    receiveFile();
                }
            }
        } catch (IOException e) {
            System.out.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void receiveFile() {
        try {
            String fileName = clientInput.readUTF();
            int fileLength = clientInput.readInt();

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
                    int bytesRead = clientInput.read(buffer, 0, bytesToRead);

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
}