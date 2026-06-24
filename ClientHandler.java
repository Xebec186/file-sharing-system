import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

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
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

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
            cleanup();
        }
    }

    private void receiveFile() throws IOException {
        String fileName = dataInputStream.readUTF();
        int fileLength = dataInputStream.readInt();

        File file = new File("server/" + fileName);
        File serverFolder = file.getParentFile();
        if (serverFolder != null && !serverFolder.exists()) {
            serverFolder.mkdirs();
        }

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

    private void sendFileNames() throws IOException {
        File serverFolder = new File("server");
        if (!serverFolder.exists()) {
            serverFolder.mkdir();
        }
        File[] files = serverFolder.listFiles();
        List<String> fileNames = List.of();
        if (files != null) {
            fileNames = Arrays.stream(files).map(File::getName).toList();
        }
        dataOutputStream.writeUTF(fileNames.toString());
    }

    private void sendFile() throws IOException {
        String fileName = dataInputStream.readUTF();
        File file = new File("server/" + fileName);
        if (!file.exists() || !file.isFile()) {
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
    }

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
