package parallelBoggle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Stream;

/**
 * BoggleServer class.
 * Handles the logic and statistics of the boggle game.
 * It is capable of handling multiple boggle sessions.
 *
 * Created by ecarpio
 */
public class BoggleServer extends UnicastRemoteObject
        implements BoggleServerInterface {
  public static final int BOARD_DIMENSION = 4;

  public static final int REQUEST_SESSION = 1;
  public static final int REQUEST_START = 2;
  public static final int SUBMIT_WORD = 3;
  public static final int REQUEST_STATISTICS = 4;
  public static final int FINALIZE_SESSION = 5;
  public static final short JOIN_SESSION = 6;
  public static final int REQUEST_SESSION_STATISTICS = 7;

  private final String serverName;
  private final Map<Integer, LocalSession> activeGames;
  private final Map<Integer, CyclicBarrier> startBarriers;
  private final Map<String, Integer> dictionary;
  private int sessionCounter = 0;
  private final Object sessionLock;
  private Records records;
  private Random r;
  private String[] vowels;

  /**
   * Creates a new server and initializes its fields.
   */
  public BoggleServer(String serverName) throws RemoteException {
    this.r = new Random();
    vowels = new String[]{"A","E","I","O","U","Y"};
    this.serverName = serverName;
    this.activeGames = new ConcurrentHashMap<>();
    this.dictionary = new ConcurrentHashMap<>();
    this.startBarriers = new ConcurrentHashMap<>();
    this.sessionLock = new Object();
    this.records = new Records();
    loadDictionary();
  }

  /**
   * Loads the dictionary file from disk.
   * The value of each words in the dictionary is calculated while
   * they are being loaded into the server's memory.
   * This is executed only once during the server lifecycle.
   */
  private void loadDictionary() {
    try (Stream<String> words =
                 Files.lines(Paths.get("resources/dictionary.txt"))) {
        words.forEach(line -> dictionary.put(line, calculatePoints(line)));
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("Loaded words: " + dictionary.size());
  }

  /**
   * Function that calculates the value of the words in the dictionary.
   */
  private Integer calculatePoints(String line) {
    switch (line.length()) {
      case 3:
      case 4:
        return 1;
      case 5:
        return 2;
      case 6:
        return 3;
      case 7:
        return 5;
      default:
        return 11;
    }
  }

  /**
   * Returns a Map containing the active games and the
   * players that have joined each session.
   */
  public Map<Integer, List<String>> getActiveGames() {
    Map<Integer, List<String>> retMap = new ConcurrentHashMap<>();
    for (Map.Entry<Integer, LocalSession> game : activeGames.entrySet())
      retMap.put(game.getKey(), game.getValue().getPlayers());
    return retMap;
  }

  /**
   * Creates a session with the given number of players.
   * The player that creates the session is set as the session owner.
   */
  @Override
  public Session createSession(int numPlayers, String playerName)
          throws RemoteException {
    int newSessionId;
    synchronized(sessionLock) {
      sessionCounter++;
      newSessionId = sessionCounter;
    }
    int numWords = 0;
    List<String> solution = new LinkedList<>();
    String board = "";
    while (numWords < 15) {
      board = getBoard();
      solution = getSolution(board);
      numWords = solution.size();
    }
    Map<String, Player> players = new HashMap<>();
    players.put(playerName, new Player(playerName));
    LocalSession newSession = new LocalSession(newSessionId,
            board, solution, players, numPlayers);
    activeGames.put(newSessionId, newSession);
    startBarriers.put(newSessionId, new CyclicBarrier(numPlayers));
    return newSession;
  }

  /**
   * Creates a game board with random letters.
   */
  private String getBoard() {
    int min = 65;
    int max = 90;
    String[] board = new String[16];
    for (int i = 0; i < 16; i ++) {
      board[i] = String.valueOf((char)((char) r.nextInt((max - min) + 1) + min));
    }
    for (int i = 0; i < 4; i ++) {
      board[r.nextInt(16)] = vowels[r.nextInt(6)];
    }
    String ret = "";
    int counter = 0;
    for (int i = 0; i < 16; i ++) {
      ret += board[i];
      counter++;
      if (counter == 4) {
        ret += ",";
        counter = 0;
      }
      else {
        ret += " ";
      }
    }
    return ret;
    //return "S S T P,R E H G,D Y R W,U U B P";
  }

  /**
   * Generates a solution for a given boggle board.
   */
  private List<String> getSolution(String board) {
    String[][] boardMatrix = new String[BOARD_DIMENSION][BOARD_DIMENSION];
    String[] rows = board.split(",");
    Set<String> solution = new HashSet<>();
    for (int i = 0; i < BOARD_DIMENSION; i++) {
      boardMatrix[i] = rows[i].split(" ");
    }
    for (int i = 0; i < BOARD_DIMENSION; i++) {
      for (int j = 0; j < BOARD_DIMENSION; j++) {
        String[][] tBoard = deepArrayCopy(boardMatrix);
        String word = "";
        recursiveSolver(i, j, tBoard, word, solution);
      }
    }
    return Collections.unmodifiableList(new LinkedList<>(solution));
  }

  /**
   * Creates a copy of the board. Used for recursively finding
   * possible words in the boggle board.
   */
  private String[][] deepArrayCopy(String[][] boardMatrix) {
    String[][] ret = new String[BOARD_DIMENSION][BOARD_DIMENSION];
    for (int i = 0; i < BOARD_DIMENSION; i++)
      for (int j = 0; j < BOARD_DIMENSION; j++)
        ret[i][j] = boardMatrix[i][j];
    return ret;
  }

  /**
   * Used to recursively look for words in a boggle board.
   */
  private void recursiveSolver(int i, int j, String[][] boardMatrix,
                               String word, Set<String> solution) {
    word += boardMatrix[i][j].toLowerCase();
    boardMatrix[i][j] = "-";
    if (word.length() > 2)
      if (dictionary.containsKey(word)) {
        solution.add(word);
      }
    if (word.length() < 8) {
      if ((i != 0) && (j != 0) && (!boardMatrix[i - 1][j - 1].equals("-"))) {
        String[][] tBoard = deepArrayCopy(boardMatrix);
        recursiveSolver(i - 1, j - 1, tBoard, word, solution);
      }
      if ((j != 0) && (!boardMatrix[i][j - 1].equals("-"))) {
        String[][] tBoard = deepArrayCopy(boardMatrix);
        recursiveSolver(i, j - 1, tBoard, word, solution);
      }
      if ((i != BOARD_DIMENSION - 1) && (j != 0) &&
              (!boardMatrix[i + 1][j - 1].equals("-"))) {
        String[][] tBoard = deepArrayCopy(boardMatrix);
        recursiveSolver(i + 1, j - 1, tBoard, word, solution);
      }
      if ((i != 0) && (!boardMatrix[i - 1][j].equals("-"))) {
        String[][] tBoard = deepArrayCopy(boardMatrix);
        recursiveSolver(i - 1, j, tBoard, word, solution);
      }
      if ((i != BOARD_DIMENSION - 1) && (!boardMatrix[i + 1][j].equals("-"))) {
        String[][] tBoard = deepArrayCopy(boardMatrix);
        recursiveSolver(i + 1, j, tBoard, word, solution);
      }
      if ((i != 0) && (j != BOARD_DIMENSION - 1) &&
              (!boardMatrix[i - 1][j + 1].equals("-"))) {
        String[][] tBoard = deepArrayCopy(boardMatrix);
        recursiveSolver(i - 1, j + 1, tBoard, word, solution);
      }
      if ((j != BOARD_DIMENSION - 1) && (!boardMatrix[i][j + 1].equals("-"))) {
        String[][] tBoard = deepArrayCopy(boardMatrix);
        recursiveSolver(i, j + 1, tBoard, word, solution);
      }
      if ((i != BOARD_DIMENSION - 1) && (j != BOARD_DIMENSION - 1) &&
              (!boardMatrix[i + 1][j + 1].equals("-"))) {
        String[][] tBoard = deepArrayCopy(boardMatrix);
        recursiveSolver(i + 1, j + 1, tBoard, word, solution);
      }
    }
  }

  /**
   * Joins the player to the given boggle session.
   */
  @Override
  public Session joinSession(int sessionId, String playerName)
          throws RemoteException {
    if (!activeGames.containsKey(sessionId))
      throw new BoggleException("The game session id is not valid.");
    LocalSession session = activeGames.get(sessionId);
    session.addPlayer(playerName);
    return session;
  }

  /**
   * Requests a session to be started. A session starts when all the players
   * have sent their start request to the server.
   */
  @Override
  public void requestStart(int id) {
    CyclicBarrier startBarrier = startBarriers.get(id);
    if (!activeGames.containsKey(id))
        throw new BoggleException("The game session id is not valid");
    try {
      startBarrier.await();
      startBarrier.reset();
    } catch (BrokenBarrierException|InterruptedException e) {
      e.printStackTrace();
      throw new BoggleException(e.getMessage());
    }
  }

  /**
   * Submits a word to be reviewed by the boggle server. Points are awarded
   * to the player if it is a new word, otherwise points get deducted.
   */
  @Override
  public BoggleResponse submitWord(int id, String playerName, String word) {
    if (!activeGames.containsKey(id))
      throw new BoggleException("The game session id is not valid");
    LocalSession session = activeGames.get(id);
    Integer count = session.evaluateWord(word);
    int points = getSubmissionPoints(word, count);
    return session.updateScoreBoard(points, playerName, word);
  }

  /**
   * Gets the points associated with a given word in a specific session.
   */
  private int getSubmissionPoints(String word, Integer count) {
    if (count == null) {
      return dictionary.get(word);
    }
    else {
      return -count;
    }
  }

  /**
   * Retrieves the current statistics from the boggle server.
   */
  @Override
  public BoggleResponse getStatistics(int id, String playerName) {
    if (!activeGames.containsKey(id))
      throw new BoggleException("The game session id is not valid");
    LocalSession session = activeGames.get(id);
    return session.getPlayerStatistics(playerName);
  }

  /**
   * Finalizes the boggle session. Updates the server wide statistics.
   */
  @Override
  public void finalizeSession(int id) throws RemoteException {
    LocalSession session = activeGames.remove(id);
    records.updateRecords(session);
  }

  /**
   * Retrieves all the statistics of the boggle session. It serves as a synchronization point
   * to make sure that the statistics in every client reflect the current statistics.
   */
  @Override
  public BoggleResponse getSessionStatistics(int id, String playerName) throws RemoteException {
    if (!activeGames.containsKey(id))
      throw new BoggleException("The game session id is not valid");
    CyclicBarrier startBarrier = startBarriers.get(id);
    try {
      startBarrier.await();
    } catch (InterruptedException|BrokenBarrierException e) {
      e.printStackTrace();
    }
    startBarrier.reset();
    LocalSession session = activeGames.get(id);
    return session.getPlayerStatistics(playerName);
  }

  /**
   * Returns the server name.
   */
  public String getServerName() {
    return serverName;
  }

  /**
   * Retrieves the server wide records.
   */
  public void getRecords() {
    records.getRecords();
  }

  /**
   * Serializes the records and saves them to disk.
   */
  public void saveRecords(){
    records.saveRecords();
  }

  /**
   * Deletes all the existing records from the server.
   */
  public void clearRecords() {
    records.clearRecords();
  }

  /**
   * Loads an existing set of records from disk.
   */
  public void loadRecords() {
    records.loadRecords();
  }

}
