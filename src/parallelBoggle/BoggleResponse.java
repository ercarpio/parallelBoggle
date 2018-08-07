package parallelBoggle;

/**
 * BoggleResponse class.
 * Objects of this class are used to send information from the
 * boggle server to the clients.
 * The fields of this class reflect the effect that a submission or request
 * had in the server-side of the session.
 *
 * Created by ecarpio
 */
public class BoggleResponse  implements java.io.Serializable {
  private static final long serialVersionUID = 6469802641474823293L;
  private int ranking;
  private int highScore;
  private int score;
  private int latestPoints;

  /**
   * Creates a new response with the given information.
   */
  public BoggleResponse(int points, int score, int highScore, int rank) {
    this.latestPoints = points;
    this.score = score;
    this.highScore = highScore;
    this.ranking = rank;
  }

  /**
   * Retrieves the ranking field from a response.
   */
  public int getRanking() {
    return ranking;
  }

  /**
   * Retrieves the high score field from a response.
   */
  public int getHighScore() {
    return highScore;
  }

  /**
   * Retrieves the score field from a response.
   */
  public int getScore() {
    return score;
  }

  /**
   * Retrieves the latest points field from a response.
   */
  public int getLatestPoints() {
    return latestPoints;
  }

  /**
   * Formats the fields of the class into a string.
   * This method is used to send information over the network to clients
   * that connected using the socket implementation.
   */
  @Override
  public String toString() {
    return 1 + "|" + latestPoints + "|" + score + "|" + highScore + "|" + ranking;
  }
}
