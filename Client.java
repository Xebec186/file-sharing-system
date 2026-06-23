import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    static Scanner scanner = null;
    static Socket socket = null;
    static DataOutputStream dataOutputStream = null;
    static DataInputStream dataInputStream = null;

    public static void main(String[] args) {
        System.out.println("Connecting to server...");
        try {
            socket = new Socket("localhost", 4000);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
            System.out.println("Connected to server successfully.\n");

            scanner = new Scanner(System.in);
            int userChoice = 0;

            while(userChoice != 3) {
                displayMenu();
                userChoice = scanner.nextInt();

                // clear \n in buffer
                scanner.nextLine();

                switch (userChoice) {
                    case 1:
                        System.out.print("Enter absolute path of file: ");
                        String path = scanner.nextLine();
                        uploadFile(path);
                        break;
                    case 2:
                        listServerFiles();
                        System.out.print("Enter name of file to download: ");
                        String fileName = scanner.nextLine();
                        downloadFile(fileName);
                        break;
                    case 3:
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
            e.printStackTrace();
        }



    }

    private static void displayMenu() {
        System.out.println("=========MAIN MENU=========");
        System.out.println("1. Upload file to server");
        System.out.println("2. View uploaded files");
        System.out.println("3. Exit");
        System.out.print("Enter choice: ");
    }

    private static void listServerFiles() {
        try {
            dataOutputStream.writeUTF("LIST");
            System.out.println(dataInputStream.readUTF());
        } catch (IOException e) {
            System.out.println("An error occurred in listing server files: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private static void uploadFile(String path) {
        File file = new File(path);
        if(!file.exists()) {
            System.out.println("File with path " + path + " does not exist.\n");
            return;
        }
        try {
            // Get client file bytes
            FileInputStream fileInput = new FileInputStream(file);
            byte[] fileBytes = fileInput.readAllBytes();

            // send header
            dataOutputStream.writeUTF("UPLOAD");
            dataOutputStream.writeUTF(file.getName());
            dataOutputStream.writeInt((int) file.length());

            // send actual file content
            dataOutputStream.write(fileBytes);

            dataOutputStream.flush();

            // close stream to free up resources
            fileInput.close();
            System.out.println("File uploaded successfully\n");
        } catch (IOException e) {
            System.out.println("An error occurred in uploading file: " + e.getMessage());
            e.printStackTrace();
        }

    }

    private static void downloadFile(String fileName) {
        
    }
}