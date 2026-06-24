import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Server class starts a multithreaded TCP server that listens for
 * incoming client connections on a specific port and delegates communication
 * to a ClientHandler run inside a cached thread pool.
 */
public class Server {

    // Default listening port for the server socket
    private static final int PORT = 4000;

    public static void main(String[] args) {
        // Create a cached thread pool to dynamically allocate threads for incoming client connections
        ExecutorService threadPool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT + "...");

            // Keep listening for connections indefinitely
            while (true) {
                // Blocks until a client connects (TCP handshaking succeeds)
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());
                
                // Submit a new ClientHandler instance to run on a background thread
                threadPool.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean shutdown of the thread pool on server termination
            threadPool.shutdown();
        }
    }
}