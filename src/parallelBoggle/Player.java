package parallelBoggle;

/**
 * Player class.
 * This class stores specific information about the players
 * of a boggle session.
 * This objects are serializable because they are sent
 * to the clients using the RMI implementation.
 *
 * Created by ecarpio
 */
public class Player implements java.io.Serializable {
  private final String username;
  private int score;
  private String bestWord;
  private int bestWordScore;
  private int newWords;
  private int repeatedWords;

  /**
   * Creates a new player for the given username.
   */
  public Player(String username) {
    this.username = username;
    this.score = 0;
    this.bestWord = "";
    this.bestWordScore = Integer.MIN_VALUE;
    this.newWords = 0;
    this.repeatedWords = 0;
  }

  /**
   * Updates the current score of the player.
   * Updates the personal records if necessary.
   */
  public synchronized int updateScore(int points, String word) {
    if (points < 0)
      repeatedWords++;
    else
      newWords++;
    score += points;
    if (points > bestWordScore) {
      bestWordScore = points;
      bestWord = word;
    }
    return score;
  }

  /**
   * Returns the score of the player.
   */
  public int getScore() {
    return score;
  }

  /**
   * Return the score of the best word submitted by the player.
   */
  public int getBestWordScore() {
    return bestWordScore;
  }

  /**
   * Return the best word submitted by the player.
   */
  public String getBestWord() {
    return  bestWord;
  }

  /**
   * Returns the number of new words submitted by the player.
   */
  public int getNewWords() {
    return  newWords;
  }

  /**
   * Returns the number of repeated words submitted by the player.
   */
  public int getRepeatedWords() {
    return repeatedWords;
  }

  /**
   * Returns the username of the player.
   */
  public String getUsername() {
    return username;
  }
}
