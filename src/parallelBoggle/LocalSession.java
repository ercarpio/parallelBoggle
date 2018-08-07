package parallelBoggle;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LocalSession class.
 * This class extends the session class and it is used to execute server-side
 * actions to the active boggle sessions in the server.
 *
 * This class is thread safe.
 *
 * Created by ecarpio
 */
public class LocalSession extends Session {
  Map<String, Integer> submissions;
  private int bestWordScore;
  private int highestScore;
  private int uniqueWordCounter;
  private int repeatedWordCounter;
  private String bestWord;
  private String winner;

  /**
   * Creates a session with the values received as parameters.
   */
  public LocalSession(int id, String board, List<String> solution,
                      Map<String, Player> players, int numPlayers) {
    super(id, board, solution, players, numPlayers);
    submissions = new ConcurrentHashMap<>();
    bestWord = "";
    winner = "";
    bestWordScore = 0;
    highestScore = 0;
    uniqueWordCounter = 0;
    repeatedWordCounter = 0;
  }

  /**
   * Adds a player to the session.
   */
  public synchronized void addPlayer(String playerName) {
    if (players.size() < minPlayers)
      players.put(playerName, new Player(playerName));
    else
      throw new BoggleException("The maximum number of players has been reached.");
  }

  /**
   * Verifies if a username has not been used in the session.
   */
  public synchronized boolean validPlayer(String playerName) {
    return !players.containsKey(playerName);
  }

  /**
   * Increments the submission count for a word.
   */
  public synchronized Integer evaluateWord(String word) {
    Integer count = submissions.get(word);
    if (count == null)
      submissions.put(word, 1);
    else {
      Integer oldval = count;
      count += 1;
      submissions.replace(word, oldval, count);
    }
    return count;
  }

  /**
   * Updates the current score board and returns an object that reflects all
   * of the changes that were applied.
   */
  public synchronized BoggleResponse updateScoreBoard(int points,
                                                      String playerName,
                                                      String word) {
    int score = players.get(playerName).updateScore(points, word);
    TreeSet<Integer> scores = new TreeSet<>();
    for (Player p: players.values()) {
      scores.add(p.getScore());
    }
    Iterator i = scores.descendingIterator();
    int rank = 1;
    Integer highScore = scores.last();
    while (i.hasNext()) {
      Integer val = (Integer) i.next();
      if (score < val) {
        rank++;
      }
    }
    return new BoggleResponse(points, score, highScore, rank);
  }

  /**
   * Returns the statistics of a specific player.
   */
  public synchronized BoggleResponse getPlayerStatistics(String playerName) {
    int score = players.get(playerName).getScore();
    TreeSet<Integer> scores = new TreeSet<>();
    for (Player p: players.values()) {
      scores.add(p.getScore());
    }
    Iterator i = scores.descendingIterator();
    int rank = 1;
    Integer highScore = scores.last();
    while (i.hasNext()) {
      Integer val = (Integer) i.next();
      if (score < val) {
        rank++;
      }
    }
    return new BoggleResponse(0, score, highScore, rank);
  }

  /**
   * Retrieves the bestWordScore field.
   */
  public synchronized int getBestWordScore() {
    return bestWordScore;
  }

  /**
   * Retrieves the bestWord field.
   */
  public synchronized String getBestWord() {
    return bestWord;
  }

  /**
   * Retrieves the bestHighScore field.
   */
  public synchronized int getHighestScore() {
    return highestScore;
  }

  /**
   * Retrieves the winner field.
   */
  public synchronized String getWinner() {
    return winner;
  }

  /**
   * Computes the final statistics of a boggle session.
   */
  public synchronized void computeStatistics() {
    for (Player p: players.values()) {
      if (bestWordScore < p.getBestWordScore()) {
        bestWordScore = p.getBestWordScore();
        bestWord = p.getBestWord();
      }
      if (highestScore < p.getScore()) {
        highestScore = p.getScore();
        winner = p.getUsername();
      }
      uniqueWordCounter += p.getNewWords();
      repeatedWordCounter += p.getRepeatedWords();
    }
  }

  /**
   * Retrieves the count of the new words that were submitted.
   */
  public synchronized int getUniqueWordsCount() {
    return uniqueWordCounter;
  }

  /**
   * Retrieves the count of the repeated words that were submitted.
   */
  public synchronized int getRepeatedWordsCount() {
    return repeatedWordCounter;
  }

  /**
   * Formats the session into a string that can be sent to clients that
   * connected to the server using the socket implementation.
   */
  @Override
  public String toString() {
    synchronized (this) {
      String string = "1|" + String.valueOf(getId()) + "|" + getBoard() + "|";
      for (String s : getSolution())
        string += "," + s;
      return string;
    }
  }
}
