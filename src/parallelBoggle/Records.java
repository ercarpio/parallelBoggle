package parallelBoggle;

import java.io.*;

/**
 * Records class.
 * Stores the server-wide records. It can be serialized in order to
 * store and load the records from and to disk.
 *
 * This class is thread safe.
 *
 * Created by ecarpio
 */
public class Records implements Serializable {
  private String bestWord;
  private int bestWordScore;
  private int highestScore;
  private String highestScoreUsername;
  private int gamesCompleted;
  private int uniqueWords;
  private int repeatedWords;

  /**
   * Creates a new set of records and initializes
   * the records to the default values.
   */
  public Records() {
    bestWord = "";
    highestScoreUsername = "";
    bestWordScore = 0;
    highestScore = 0;
    gamesCompleted = 0;
    uniqueWords = 0;
    repeatedWords = 0;
  }

  /**
   * Updates the records if any has been broken by the
   * players during the given session.
   */
  public synchronized void updateRecords(LocalSession session) {
    session.computeStatistics();
    if (session.getBestWordScore() > bestWordScore) {
      bestWordScore = session.getBestWordScore();
      bestWord = session.getBestWord();
    }
    if (session.getHighestScore() > highestScore) {
      highestScore = session.getHighestScore();
      highestScoreUsername = session.getWinner();
    }
    uniqueWords += session.getUniqueWordsCount();
    repeatedWords += session.getRepeatedWordsCount();
    gamesCompleted++;
  }

  /**
   * Prints the current records to the standard output.
   */
  public synchronized void getRecords() {
    System.out.printf("Highest score:  %s (%d points)%n", highestScoreUsername, highestScore);
    System.out.printf("Best word:      %s (%d points)%n", bestWord, bestWordScore);
    System.out.printf("Games played:   %d%n", gamesCompleted);
    System.out.printf("Valid words:    %d%n", uniqueWords);
    System.out.printf("Repeated words: %d%n", repeatedWords);
  }

  /**
   * Serializes and stores the current records to disk.
   */
  public synchronized void saveRecords() {
    try (
            OutputStream outFile = new FileOutputStream("server.records");
            OutputStream buffer = new BufferedOutputStream(outFile);
            ObjectOutput output = new ObjectOutputStream(buffer)
    ){
      output.writeObject(this);
      System.out.println("The records have been saved.");
    } catch (IOException e) {
      System.out.println("The records could not be saved.");
    }
  }

  /**
   * Deletes all the existing records and restores them
   * to their default initial values.
   */
  public synchronized void clearRecords() {
    highestScore = 0;
    highestScoreUsername = "";
    bestWord = "";
    bestWordScore = 0;
    gamesCompleted = 0;
    uniqueWords = 0;
    repeatedWords = 0;
    System.out.println("The records were cleared.");
  }

  /**
   * Loads the records from disk if a previous set of records
   * is available.
   */
  public synchronized void loadRecords() {
    try (
            InputStream inFile = new FileInputStream("server.records");
            InputStream buffer = new BufferedInputStream(inFile);
            ObjectInput input = new ObjectInputStream(buffer)
    ){
      saveRecords((Records) input.readObject());
      System.out.println("The were loaded successfully.");
    }
    catch (ClassNotFoundException|IOException e) {
      System.out.println("The records could not be loaded.");
    }
  }

  /**
   * Copies the contents of a given set of records into the
   * current records of the server.
   */
  private synchronized void saveRecords(Records records) {
    this.bestWord = records.bestWord;
    this.bestWordScore = records.bestWordScore;
    this.highestScore = records.highestScore;
    this.highestScoreUsername = records.highestScoreUsername;
    this.uniqueWords = records.uniqueWords;
    this.repeatedWords = records.repeatedWords;
  }
}
