package parallelBoggle;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * GameServer class.
 * It runs a BoggleServer and handles the server's RMI registry
 * and socket connections.
 *
 * Created by ecarpio
 */
public class GameServer {
  public static final String SERVER_NAME = "ERP48Server";
  private final BoggleServer boggle;
  private Registry registry;

  /**
   * Creates a server for the given boggle server.
   */
  public GameServer(BoggleServer boggle) {
    this.boggle = boggle;
  }

  /**
   * Starts the server by binding it to a registry.
   * This method was taken from the Remote Bank assignment of CS835
   * as skeleton code for this project.
   *
   * <ul>
   *
   * <li>If {@code port} is positive, the server attempts to locate a registry at this port.</li>
   *
   * <li>If {@code port} is negative, the server attempts to start a new registry at this
   * port.</li>
   *
   * <li>If {@code port} is 0, the server attempts to start a new registry at a randomly chosen
   * port.</li>
   *
   * </ul>
   *
   * @return the registry port
   */
  public synchronized int start(int port) throws RemoteException {
    if (registry != null)
      throw new IllegalStateException("The server is already running.");
    Registry reg;
    if (port > 0) { // registry already exists
      reg = LocateRegistry.getRegistry(port);
    } else if (port < 0) { // create on given port
      port = -port;
      reg = LocateRegistry.createRegistry(port);
    } else { // create registry on random port
      Random rand = new Random();
      int tries = 0;
      while (true) {
        port = 50000 + rand.nextInt(10000);
        try {
          reg = LocateRegistry.createRegistry(port);
          break;
        } catch (RemoteException e) {
          if (++tries < 10 && e.getCause() instanceof java.net.BindException)
            continue;
          throw e;
        }
      }
    }
    reg.rebind(boggle.getServerName(), boggle);
    registry = reg;
    return port;
  }

  /**
   * Stops the server by removing the boggle server form the registry.
   */
  public synchronized void stop() {
    if (registry != null) {
      try {
        registry.unbind(boggle.getServerName());
      } catch (Exception e) {
        System.err.printf("Unable to stop: %s.%n", e.getMessage());
      } finally {
        registry = null;
      }
    }
  }

  /**
   * Prints all active games and their players.
   */
  public synchronized void printStatus() {
    System.out.printf("Server status:");
    Map<Integer, List<String>> games = boggle.getActiveGames();
    if (games.isEmpty()) {
      System.out.printf(" no active games.%n");
      return;
    }
    System.out.println();
    for (Map.Entry<Integer, List<String>> game : games.entrySet())
      System.out.printf("  game %d: players = %s%n", game.getKey(),
              game.getValue());
  }

  /**
   * Command-line program.  Single (optional) argument is a port number (see {@link #start(int)}).
   */
  public static void main(String[] args) throws Exception {
    int port = 0;
    if (args.length > 0)
      port = Integer.parseInt(args[0]);
    BoggleServer boggle = new BoggleServer(SERVER_NAME);
    GameServer server = new GameServer(boggle);
    try {
      port = server.start(port);
      System.out.printf("RMI server running on port %d.%n", port);
    } catch (RemoteException e) {
      Throwable t = e.getCause();
      if (t instanceof java.net.ConnectException)
        System.err.println("Unable to connect to registry: " + t.getMessage());
      else if (t instanceof java.net.BindException)
        System.err.println("Cannot start registry: " + t.getMessage());
      else
        System.err.println("Cannot start server: " + e.getMessage());
      UnicastRemoteObject.unexportObject(boggle, false);
    }
    Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    ServerSocketHandler socketHandler = new ServerSocketHandler(port + 1, boggle);
    server.waitForCommands();
  }

  /**
   * Processes any administrator commands that are executed in the game server.
   */
  private void waitForCommands() {
    Scanner in = new Scanner(System.in);
    try {
      while (true) {
        String line = in.nextLine().trim();
        if (line.isEmpty()) {
          continue;
        }
        switch (line) {
          case "print status":
            printStatus();
            break;
          case "print records":
            boggle.getRecords();
            break;
          case "save records":
            boggle.saveRecords();
            break;
          case "clear records":
            boggle.clearRecords();
            break;
          case "load records":
            boggle.loadRecords();
            break;
          case "stop server":
            System.exit(1);
            return;
        }
      }
    } catch (java.util.NoSuchElementException e) {
      /* EOF */
    }
  }
}

/**
 * ServerSocketHandler.
 * Handles socket connections established to the game server.
 */
class ServerSocketHandler {
  private final Executor exec;
  private final BoggleServer boggle;

  /**
   * Creates a new server socket handler object and initializes the
   * executor thread pool and stores a reference to the boggle server
   * it will use to process requests.
   */
  public ServerSocketHandler(int port, BoggleServer boggle) throws IOException {
    this.boggle = boggle;
    exec = Executors.newCachedThreadPool();
    exec.execute(new Listener(port));
  }

  /**
   * Listener class.
   * Listens requests received in a given port. For each requests it schedules
   * a new task in the thread pool provided by the server socket handler.
   */
  class Listener implements Runnable {
    private final ServerSocket serv;

    /**
     * Creates a new listener in the given port.
     */
    public Listener(int port) throws IOException {
      System.out.println("Server listening on port " + port);
      serv = new ServerSocket(port);
    }

    /**
     * Listens for requests in a given socket. Schedules the execution
     * of any received commands.
     */
    public void run() {
      while (true) {
        try {
          Socket s = serv.accept();
          exec.execute(new ConnectionHandler(s));
        } catch (IOException e) {
          e.printStackTrace();
          return;
        }
      }
    }
  }

  /**
   * ConnectionHandler class.
   * Handles any incoming commands into the given socket.
   */
  class ConnectionHandler implements Runnable {
    private final Socket socket;

    /**
     * Assigns the socket provided as a parameter as the class socket.
     */
    public ConnectionHandler(Socket s) {
      socket = s;
    }

    /**
     * Processes any received commands and passes them to the boggle server.
     */
    public void run() {
      BufferedReader in = null;
      PrintWriter out = null;

      try {
        socket.setKeepAlive(true);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

        String input;
        try {
          input = in.readLine();
        } catch (IOException e) {
          input = "";
        }
        if (!input.equals("")) {
          String response = processMessage(input);
          out.print(response);
          out.flush();
        }
      }
      catch (IOException e) {
        System.out.println("IO error in socket service. " + e.getMessage());
      }
      finally {
        try {
          if (in != null)
            in.close();
          if (out != null)
            out.close();
        } catch (IOException e) {
          System.out.println("IO error when closing socket. " + e.getMessage());
        }
      }
    }

    /**
     * Processes the received messages and passes them to the boggle server after
     * they have been processed and turned into boggle instructions.
     */
    private String processMessage(String input) throws RemoteException {
      String[] values = input.split("[|]");
      int commandId = Integer.valueOf(values[0]);
      try {
        switch (commandId) {
          case BoggleServer.REQUEST_SESSION:
            return boggle.createSession(Integer.parseInt(values[1]), values[2]).toString();
          case BoggleServer.REQUEST_START:
            boggle.requestStart(Integer.parseInt(values[1]));
            return "2|Command processed";
          case BoggleServer.SUBMIT_WORD:
            return boggle.submitWord(Integer.parseInt(values[1]), values[2], values[3]).toString();
          case BoggleServer.REQUEST_STATISTICS:
            return boggle.getStatistics(Integer.parseInt(values[1]), values[2]).toString();
          case BoggleServer.FINALIZE_SESSION:
            boggle.finalizeSession(Integer.parseInt(values[1]));
            return "2|Command processed";
          case BoggleServer.JOIN_SESSION:
            return boggle.joinSession(Integer.parseInt(values[1]), values[2]).toString();
          case BoggleServer.REQUEST_SESSION_STATISTICS:
            return boggle.getSessionStatistics(Integer.parseInt(values[1]), values[2]).toString();
          default:
            return "0|Command not recognized";
        }
      }
      catch (Exception e) {
        return "0|" + e.getMessage();
      }
    }
  }
}