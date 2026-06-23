import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    static Scanner scanner = null;
    static Socket socket = null;
    static DataOutputStream dataOutputStream = null;

    public static void main(String[] args) {
        System.out.println("Connecting to server...");
        try {
            socket = new Socket("localhost", 4000);
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
                        displayServerFiles();
                        break;
                    case 3:
                        dataOutputStream.writeUTF("exit");
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

    private static void displayServerFiles() {
        File serverFolder = new File("server");

        if(!serverFolder.exists()) {
            System.out.println("No file has been uploaded to server. Upload one first.\n");
            return;
        }

        File[] files = serverFolder.listFiles();
        if(files != null && files.length == 0) {
            System.out.println("No file has been uploaded to server. Upload one first.\n");
            return;
        }

        System.out.println("\nServer Files");
        for(int i=0; i < files.length; i++) {
            System.out.println((i+1) + ". " + files[i].getName());
        }

        System.out.print("Enter number of file to download: ");
        int choice = scanner.nextInt();

        // clear \n in buffer
        scanner.nextLine();

        File file = files[choice-1];
        downloadFile(file);
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
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
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

    private static void downloadFile(File file) {
        System.out.println("\nDownloading " + file.getName() + "...");
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] fileBytes = fileInputStream.readAllBytes();

            File downloadedFile = new File("downloads/" + file.getName());
            File downloadsFolder = downloadedFile.getParentFile();
            if(downloadsFolder != null && !downloadsFolder.exists()) {
                boolean isFolderCreated = downloadsFolder.mkdirs();
                if(!isFolderCreated) {
                    System.out.println("Failed to download file from server.");
                    System.out.println("Downloads folder could not be created. Try creating it manually and try again");
                    return;
                }
            }

            downloadedFile.createNewFile();
            FileOutputStream downloadedFileOutputStream = new FileOutputStream(downloadedFile);
            downloadedFileOutputStream.write(fileBytes);

            fileInputStream.close();
            downloadedFileOutputStream.close();
            System.out.println("File successfully downloaded to " + downloadedFile.getAbsolutePath() + "\n");
        } catch (IOException e) {
            System.out.println("An error occurred in downloading file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}