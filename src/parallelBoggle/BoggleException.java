package parallelBoggle;

/**
 * Exceptions thrown by the boggle server. They are serializable
 * so they can be send over RMI.
 *
 * Created by ecarpio
 */
public class BoggleException extends RuntimeException {
  private static final long serialVersionUID = -3011912973465419856L;

  BoggleException(String message){
    super(message);
  }
}
