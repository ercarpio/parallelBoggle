package parallelBoggle;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * BoggleServerInterface interface.
 * It is implemented by the BoggleServer class and it is used to execute
 * RMI calls from game clients into the boggle server.
 *
 * Created by ecarpio
 */
public interface BoggleServerInterface extends Remote {
  /**
   * Creates a session with the given number of players.
   * The player that creates the session is set as the session owner.
   */
  Session createSession(int numPlayers, String playerName) throws RemoteException;

  /**
   * Joins the player to the given boggle session.
   */
  Session joinSession(int sessionId, String playerName) throws RemoteException;

  /**
   * Requests a session to be started. A session starts when all the players
   * have sent their start request to the server.
   */
  void requestStart(int id) throws RemoteException;

  /**
   * Submits a word to be reviewed by the boggle server. Points are awarded
   * to the player if it is a new word, otherwise points get deducted.
   */
  BoggleResponse submitWord(int id, String playerName, String word) throws RemoteException;

  /**
   * Retrieves the current statistics from the boggle server.
   */
  BoggleResponse getStatistics(int id, String playerName) throws RemoteException;

  /**
   * Finalizes the boggle session. Updates the server wide statistics.
   */
  void finalizeSession(int id) throws RemoteException;

  /**
   * Retrieves all the statistics of the boggle session. It serves as a synchronization point
   * to make sure that the statistics in every client reflect the current statistics.
   */
  BoggleResponse getSessionStatistics(int id, String playerName) throws RemoteException;
}
