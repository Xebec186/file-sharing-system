import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class MainController {

    private static final int MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB

    @FXML private TextField serverAddressField;
    @FXML private Button connectButton;
    @FXML private Button newWindowButton;
    @FXML private Button refreshButton;
    @FXML private Label connectionStatusLabel;
    @FXML private TextField searchField;
    @FXML private TableView<FileItem> fileTable;
    @FXML private TableColumn<FileItem, String> fileNameColumn;
    @FXML private TableColumn<FileItem, String> fileSizeColumn;
    @FXML private Button uploadButton;
    @FXML private Button downloadButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;

    private Stage stage;
    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private boolean connected = false;

    private final ObservableList<FileItem> masterFileList = FXCollections.observableArrayList();
    private FilteredList<FileItem> filteredFileList;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        // Configure Table Columns
        fileNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        fileSizeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSize()));

        // Set up filtering
        filteredFileList = new FilteredList<>(masterFileList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredFileList.setPredicate(fileItem -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return fileItem.getName().toLowerCase().contains(lowerCaseFilter);
            });
        });

        fileTable.setItems(filteredFileList);
    }

    @FXML
    private void handleConnect(ActionEvent event) {
        if (connected) {
            disconnect();
        } else {
            connect();
        }
    }

    private void connect() {
        String address = serverAddressField.getText().trim();
        if (address.isEmpty()) {
            showErrorAlert("Connection Error", "Server address cannot be empty.");
            return;
        }

        String host = "localhost";
        int port = 4000;

        if (address.contains(":")) {
            String[] parts = address.split(":");
            host = parts[0];
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                showErrorAlert("Connection Error", "Invalid port number format.");
                return;
            }
        } else {
            host = address;
        }

        final String finalHost = host;
        final int finalPort = port;

        statusLabel.setText("Connecting to " + finalHost + ":" + finalPort + "...");
        connectButton.setDisable(true);

        Task<Void> connectTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                socket = new Socket(finalHost, finalPort);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());
                return null;
            }
        };

        connectTask.setOnSucceeded(e -> {
            connected = true;
            connectButton.setDisable(false);
            connectButton.setText("Disconnect");
            
            connectionStatusLabel.setText("Connected");
            connectionStatusLabel.getStyleClass().clear();
            connectionStatusLabel.getStyleClass().addAll("label", "status-badge", "status-connected");

            int localPort = socket.getLocalPort();
            if (stage != null) {
                stage.setTitle("File Sharing Client - Port: " + localPort);
            }

            statusLabel.setText("Connected to server successfully. Local port: " + localPort);
            
            // Enable dashboard controls
            refreshButton.setDisable(false);
            fileTable.setDisable(false);
            uploadButton.setDisable(false);
            downloadButton.setDisable(false);

            // Auto-refresh the file list
            handleRefresh(null);
        });

        connectTask.setOnFailed(e -> {
            connectButton.setDisable(false);
            statusLabel.setText("Connection failed.");
            Throwable ex = connectTask.getException();
            showErrorAlert("Connection Failed", "Could not connect to server:\n" + ex.getMessage());
        });

        new Thread(connectTask).start();
    }

    private void disconnect() {
        shutdown();
        
        connected = false;
        connectButton.setText("Connect");
        connectionStatusLabel.setText("Disconnected");
        connectionStatusLabel.getStyleClass().clear();
        connectionStatusLabel.getStyleClass().addAll("label", "status-badge", "status-disconnected");

        if (stage != null) {
            stage.setTitle("File Sharing Client - Offline");
        }

        statusLabel.setText("Disconnected from server.");
        masterFileList.clear();

        // Disable dashboard controls
        refreshButton.setDisable(true);
        fileTable.setDisable(true);
        uploadButton.setDisable(true);
        downloadButton.setDisable(true);
        progressBar.setProgress(0.0);
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        if (!connected) return;

        statusLabel.setText("Refreshing file list...");
        refreshButton.setDisable(true);

        Task<ObservableList<FileItem>> listTask = new Task<>() {
            @Override
            protected ObservableList<FileItem> call() throws Exception {
                dataOutputStream.writeUTF("LIST");
                dataOutputStream.flush();
                
                String rawList = dataInputStream.readUTF();
                ObservableList<FileItem> files = FXCollections.observableArrayList();
                
                if (rawList.startsWith("[") && rawList.endsWith("]")) {
                    String content = rawList.substring(1, rawList.length() - 1);
                    if (!content.trim().isEmpty()) {
                        String[] items = content.split(", ");
                        for (String item : items) {
                            files.add(new FileItem(item, "Remote File", -1));
                        }
                    }
                }
                return files;
            }
        };

        listTask.setOnSucceeded(e -> {
            refreshButton.setDisable(false);
            masterFileList.clear();
            masterFileList.addAll(listTask.getValue());
            statusLabel.setText("File list refreshed. Found " + masterFileList.size() + " files.");
        });

        listTask.setOnFailed(e -> {
            refreshButton.setDisable(false);
            statusLabel.setText("Failed to retrieve file list.");
            showErrorAlert("Error", "Could not refresh file list:\n" + listTask.getException().getMessage());
        });

        new Thread(listTask).start();
    }

    @FXML
    private void handleUpload(ActionEvent event) {
        if (!connected) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");
        File file = fileChooser.showOpenDialog(stage);

        if (file == null) {
            return; // Cancelled
        }

        if (file.length() > MAX_FILE_SIZE) {
            showErrorAlert("Upload Failed", "File size exceeds the maximum limit of 50 MB.");
            return;
        }

        // Disable buttons during upload
        setActionsDisabled(true);

        Task<Void> uploadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Preparing to upload " + file.getName() + "...");
                updateProgress(0, 100);

                dataOutputStream.writeUTF("UPLOAD");
                dataOutputStream.writeUTF(file.getName());
                dataOutputStream.writeInt((int) file.length());
                dataOutputStream.flush();

                try (FileInputStream fileInput = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    int totalBytesRead = 0;
                    long fileLength = file.length();

                    while ((bytesRead = fileInput.read(buffer)) != -1) {
                        dataOutputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        
                        updateProgress(totalBytesRead, fileLength);
                        updateMessage("Uploading " + file.getName() + "... (" + (totalBytesRead / 1024) + " KB / " + (fileLength / 1024) + " KB)");
                    }
                }
                dataOutputStream.flush();
                return null;
            }
        };

        progressBar.progressProperty().bind(uploadTask.progressProperty());
        statusLabel.textProperty().bind(uploadTask.messageProperty());

        uploadTask.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            progressBar.setProgress(0.0);
            statusLabel.setText("File uploaded successfully: " + file.getName());
            setActionsDisabled(false);
            handleRefresh(null); // Refresh lists
        });

        uploadTask.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            progressBar.setProgress(0.0);
            statusLabel.setText("Upload failed.");
            setActionsDisabled(false);
            showErrorAlert("Upload Error", "Failed to upload file:\n" + uploadTask.getException().getMessage());
        });

        new Thread(uploadTask).start();
    }

    @FXML
    private void handleDownload(ActionEvent event) {
        if (!connected) return;

        FileItem selectedItem = fileTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showErrorAlert("Selection Error", "Please select a file from the list to download.");
            return;
        }

        String fileName = selectedItem.getName();

        // Default initial directory is client_<port>
        String defaultDirName = "client_" + socket.getLocalPort();
        File defaultDir = new File(defaultDirName);
        if (!defaultDir.exists()) {
            defaultDir.mkdirs();
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose Save Destination");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        File selectedDir = directoryChooser.showDialog(stage);

        if (selectedDir == null) {
            return; // Cancelled
        }

        setActionsDisabled(true);

        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Requesting " + fileName + "...");
                updateProgress(0, 100);

                dataOutputStream.writeUTF("DOWNLOAD");
                dataOutputStream.writeUTF(fileName);
                dataOutputStream.flush();

                int fileLength = dataInputStream.readInt();
                if (fileLength == -1) {
                    throw new FileNotFoundException("File does not exist on the server.");
                }

                File destinationFile = new File(selectedDir, fileName);
                
                try (FileOutputStream fileOutput = new FileOutputStream(destinationFile)) {
                    byte[] buffer = new byte[4096];
                    int totalBytesRead = 0;

                    while (totalBytesRead < fileLength) {
                        int bytesToRead = Math.min(buffer.length, fileLength - totalBytesRead);
                        int bytesRead = dataInputStream.read(buffer, 0, bytesToRead);
                        if (bytesRead == -1) {
                            throw new EOFException("Server disconnected before download finished.");
                        }
                        fileOutput.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;

                        updateProgress(totalBytesRead, fileLength);
                        updateMessage("Downloading " + fileName + "... (" + (totalBytesRead / 1024) + " KB / " + (fileLength / 1024) + " KB)");
                    }
                }
                return null;
            }
        };

        progressBar.progressProperty().bind(downloadTask.progressProperty());
        statusLabel.textProperty().bind(downloadTask.messageProperty());

        downloadTask.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            progressBar.setProgress(0.0);
            statusLabel.setText("File successfully downloaded to: " + selectedDir.getAbsolutePath() + File.separator + fileName);
            setActionsDisabled(false);
        });

        downloadTask.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            progressBar.setProgress(0.0);
            statusLabel.setText("Download failed.");
            setActionsDisabled(false);
            showErrorAlert("Download Error", "Failed to download file:\n" + downloadTask.getException().getMessage());
        });

        new Thread(downloadTask).start();
    }

    @FXML
    private void handleNewWindow(ActionEvent event) {
        Main.createNewClientWindow();
    }

    private void setActionsDisabled(boolean disabled) {
        uploadButton.setDisable(disabled);
        downloadButton.setDisable(disabled);
        refreshButton.setDisable(disabled);
        connectButton.setDisable(disabled);
        newWindowButton.setDisable(disabled);
    }

    private void showErrorAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    /**
     * Cleans up client socket resources. Called on window closure or disconnect.
     */
    public void shutdown() {
        try {
            if (dataOutputStream != null) {
                if (connected) {
                    dataOutputStream.writeUTF("EXIT");
                    dataOutputStream.flush();
                }
                dataOutputStream.close();
            }
            if (dataInputStream != null) dataInputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // Ignore socket closure exceptions
        }
    }
}
