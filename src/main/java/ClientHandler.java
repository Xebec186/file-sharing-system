import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

/**
 * ClientHandler implements Runnable to process commands from a single connected client socket
 * on a dedicated thread, allowing the Server to handle multiple concurrent clients.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Set up input and output streams for TCP communication
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            // Continuously listen for commands from the client until EXIT is received
            while (true) {
                String command = dataInputStream.readUTF();
                if ("EXIT".equals(command)) {
                    break;
                }

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
                    default:
                        System.out.println("Client sent invalid option: " + command);
                        dataOutputStream.writeUTF("Invalid option. Valid options are UPLOAD, LIST, DOWNLOAD, EXIT");
                }
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected abruptly.");
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        } finally {
            System.out.println("Client " + socket.getRemoteSocketAddress() + " disconnected");
            cleanup();
        }
    }

    /**
     * Receives a file sent by the client and saves it in the 'server' directory.
     * Expects:
     * 1. String: fileName
     * 2. int: fileLength
     * 3. raw bytes: file payload
     */
    private void receiveFile() throws IOException {
        String fileName = dataInputStream.readUTF();
        int fileLength = dataInputStream.readInt();

        File file = new File("server/" + fileName);
        File serverFolder = file.getParentFile();
        
        // Ensure the server folder exists
        if (serverFolder != null && !serverFolder.exists()) {
            serverFolder.mkdirs();
        }

        // Stream the incoming bytes from the client socket and write to file
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int totalBytesRead = 0;
            while (totalBytesRead < fileLength) {
                int bytesToRead = Math.min(buffer.length, fileLength - totalBytesRead);
                int bytesRead = dataInputStream.read(buffer, 0, bytesToRead);
                if (bytesRead == -1) {
                    throw new EOFException("Client disconnected before upload completed.");
                }
                fileOutputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
        }
        System.out.println("Received: " + fileName + " (" + fileLength + " bytes)");
    }

    /**
     * Compiles and sends a serialized list of available files in the 'server' directory
     * in the format: "[filename1:size1, filename2:size2, ...]"
     */
    private void sendFileNames() throws IOException {
        File serverFolder = new File("server");
        if (!serverFolder.exists()) {
            serverFolder.mkdir();
        }
        File[] files = serverFolder.listFiles();
        List<String> fileInfo = List.of();
        if (files != null) {
            // Map each file to "name:sizeInBytes" format
            fileInfo = Arrays.stream(files)
                .map(f -> f.getName() + ":" + f.length())
                .toList();
        }
        // Write the list as a single UTF string
        dataOutputStream.writeUTF(fileInfo.toString());
    }

    /**
     * Sends a requested file from the 'server' directory to the client.
     * Expects:
     * 1. String: fileName
     * Sends:
     * 1. int: fileLength (-1 if file not found)
     * 2. raw bytes: file content (only if fileLength > 0)
     */
    private void sendFile() throws IOException {
        String fileName = dataInputStream.readUTF();
        File file = new File("server/" + fileName);
        if (!file.exists() || !file.isFile()) {
            // Signal to the client that the file does not exist
            dataOutputStream.writeInt(-1);
            return;
        }

        // Write the file size then stream the bytes
        dataOutputStream.writeInt((int) file.length());
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
            }
        }
        dataOutputStream.flush();
    }

    /**
     * Closes socket streams and connections to prevent resource leaks.
     */
    private void cleanup() {
        try {
            if (dataInputStream != null) dataInputStream.close();
            if (dataOutputStream != null) dataOutputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("Client handler resources cleaned up.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
