package parallelBoggle;

import java.util.*;

/**
 * Session class.
 * This class stores the information of a boggle session.
 * This objects are serializable because they are sent to the clients
 * in order to allow some aspects of the game to be more efficient.
 *
 * This class is thread safe.
 *
 * Created by ecarpio
 */
public class Session implements java.io.Serializable {
  private static final long serialVersionUID = 6469802641474828290L;

  private final int Id;
  private final String board;
  private final List<String> solution;
  final Map<String, Player> players;
  final int minPlayers;
  private int roundCounter;

  /**
   * Creates a nuew session with the given board, solution and players.
   */
  public Session(int Id, String board, List<String> solution,
                 Map<String, Player> players, int numPlayers) {
    this.Id = Id;
    this.board = board;
    this.solution = Collections.unmodifiableList(solution);
    this.minPlayers = numPlayers;
    this.players = players;
    this.roundCounter = 0;
  }

  /**
   * Returns the game board.
   */
  public String getBoard() {
    return board;
  }

  /**
   * Verifies if a word is contained in the game solution.
   */
  public boolean isValidWord(String word) {
    return solution.contains(word);
  }

  /**
   * Returns the number of players that
   * are required to play the session.
   */
  public int getMinPlayers() {
    return minPlayers;
  }

  /**
   * Returns the session id.
   */
  public int getId() {
    return Id;
  }

  /**
   * Returns the list of players that joined the session.
   */
  public List<String> getPlayers() {
    return Collections.unmodifiableList(
            new LinkedList<>(players.keySet()));
  }

  /**
   * Increment the round counter.
   * After three rounds a session is completed.
   */
  public int completeRound() {
    return roundCounter++;
  }

  /**
   * Returns the set of words that are contained in the
   * game board solution.
   */
  public List<String> getSolution() {
    return solution;
  }

  /**
   * Retrieves an answer from the set of words in the solution.
   */
  public String getAnswer(int index) {
    return solution.get(index);
  }
}
