package parallelBoggle;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;

/**
 * Boggle client.
 * It receives one or two parameters.
 *
 * The first parameter indicates the service location. If this parameter is
 * started with a "-" the client uses socket connections to interact with the
 * boggle server. Otherwise it will RMI to connect. The format of this parameter
 * is the following: [-]xxx.xxx.xxx.xxx:yyyyy where the x's represent an IP address
 * and the y's represent a port number.
 *
 * The second parameter indicates if the client is a ghost. It takes a number between
 * 1 and 5 that will indicate the frecuency with which the ghost player will submit
 * words to the server. This parameter can be null.
 *
 * Created by ecarpio
 */
public class BoggleClient {
  private static final int BOARD_DIMENSION = BoggleServer.BOARD_DIMENSION;
  private static final int REQUEST_SESSION = BoggleServer.REQUEST_SESSION;
  private static final int REQUEST_START = BoggleServer.REQUEST_START;
  private static final int SUBMIT_WORD = BoggleServer.SUBMIT_WORD;
  private static final int REQUEST_STATISTICS = BoggleServer.REQUEST_STATISTICS;
  private static final int FINALIZE_SESSION = BoggleServer.FINALIZE_SESSION;
  private static final short JOIN_SESSION = BoggleServer.JOIN_SESSION;
  private static final int REQUEST_SESSION_STATISTICS = BoggleServer.REQUEST_SESSION_STATISTICS;
  private final BoggleServerInterface server;
  private JTable gameBoard;
  private JTextField userTextField;
  private JTextField gameIdTextField;
  private JButton sendButton;
  private JTextField submissionsTextField;
  private JRadioButton joinRadButton;
  private JRadioButton createRadButton;
  private JPanel mainPanel;
  private JLabel highScoreLabel;
  private JLabel playerScoreLabel;
  private JLabel rankingLabel;
  private JLabel timeLabel;
  private JLabel paramLabel;
  private JLabel statusLabel;
  private Timer gameClock;
  private int gameTime;
  private Session session;
  private final int port;
  private final String host;
  private Socket socket;
  private boolean sessionOwner;
  private boolean isGhost;
  private int ghostTime;
  private Random r;

  /**
   * Preapres the interface and connects to the server.
   */
  public BoggleClient(String serviceLocation, int t, boolean isGhost) throws
          IOException, NotBoundException {
    addListeners();
    gameBoard.setModel(new DefaultTableModel(BOARD_DIMENSION, BOARD_DIMENSION));
    DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
    cellRenderer.setVerticalAlignment(SwingConstants.CENTER);
    cellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    gameBoard.setDefaultRenderer(Object.class, cellRenderer);
    gameBoard.setRowHeight(30);
    gameBoard.setBorder(new MatteBorder(1, 1, 1, 1, Color.black));
    userTextField.setText(System.getProperty("user.name") + String.valueOf(System.currentTimeMillis()).substring(10));
    joinRadButton.setSelected(true);
    sessionOwner = false;
    this.isGhost = isGhost;
    this.ghostTime = t;
    this.r = new Random();
    makeGUIUnplayable();
    if (!serviceLocation.substring(0,1).equals("-")) {
      String service = "rmi://" + serviceLocation + "/" + GameServer.SERVER_NAME;
      server = (BoggleServerInterface) java.rmi.Naming.lookup(service);
      port = 0;
      host = "";
      socket = null;
    }
    else {
      server = null;
      String[] info = serviceLocation.split(":");
      port = Integer.parseInt(info[1]);
      host = info[0].replace("-","");
      socket = new Socket(host, port);
      System.out.println("Connection established.");
    }
  }

  /**
   * Sends a text command to the server using a socket connection.
   */
  private String sendTextCommand(String command) {
    String retVal = "";
    if (socket.isClosed())
      try {
        socket = new Socket(host, port);
      } catch (IOException e) {
        e.printStackTrace();
      }
    try (
            Scanner in = new Scanner(socket.getInputStream());
            OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream())
    ) {
      out.write(command);
      out.write("\n");
      out.flush();
      retVal = in.nextLine();
      System.out.println("Client message: " + command);
      System.out.println("Server response: " + retVal);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return retVal;
  }

  /**
   * Adds listeners to some of the controls of the form.
   */
  private void addListeners() {
    submissionsTextField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyChar() == 10) {
          String word = submissionsTextField.getText();
          submissionsTextField.setText("");
          if (session.isValidWord(word)) {
            BoggleResponse response =
                    null;
            try {
              if (server != null)
                response = server.submitWord(session.getId(), userTextField.getText(), word);
              else {
                String command = generateTextCommand(SUBMIT_WORD, session.getId(),
                        userTextField.getText() + "|" + word);
                String sResponse = sendTextCommand(command);
                response = createResponse(sResponse);
              }
            } catch (RemoteException e1) {
              e1.printStackTrace();
            }
            updateStatistics(response, "");
          }
          else {
            statusLabel.setText("Invalid word.");
          }
        }
      }
    });
    sendButton.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (gameIdTextField.getText().equals("") &&
                (joinRadButton.isSelected())) {
          showMessage("Game session Id required.", JOptionPane.WARNING_MESSAGE);
          gameIdTextField.requestFocus();
        }
        else if (userTextField.getText().equals("")) {
          showMessage("Username required.", JOptionPane.WARNING_MESSAGE);
          userTextField.requestFocus();
        }
        else if (joinRadButton.isSelected()){
          requestJoinToServer(gameIdTextField.getText().trim(),
                  userTextField.getText().trim());
        }
        else if (createRadButton.isSelected()) {
          requestSessionFromServer(gameIdTextField.getText().trim(),
                  userTextField.getText().trim());
        }
      }
    });
    createRadButton.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        paramLabel.setText("Players:");
      }
    });
    joinRadButton.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        paramLabel.setText("Game Id:");
      }
    });

    gameClock = new Timer(1000, (ActionEvent e) -> {
        gameTime--;
        if (gameTime > 60) {
          statusLabel.setText("Game starting in: " + (gameTime-60));
        }
        else {
          if (!submissionsTextField.getText().equals("") && isGhost) {
            submissionsTextField.getKeyListeners()[0].keyPressed(
                    new KeyEvent(this.mainPanel, 1, 20,
                            InputEvent.BUTTON1_DOWN_MASK, 10, '\n'));
          }
          if (gameTime == 60) {
            statusLabel.setText("Go!");
            submissionsTextField.setEnabled(true);
          }
          timeLabel.setText(Integer.toString(gameTime));
          if (gameTime == 0) {
            gameClock.stop();
            submissionsTextField.setEnabled(false);
            submissionsTextField.setText("");
            statusLabel.setText("Time is up!");
            gameTime = 60;
            BoggleResponse response = null;
            try {
              if (server != null)
                response = server.getSessionStatistics(session.getId(), userTextField.getText());
              else {
                String command = generateTextCommand(REQUEST_SESSION_STATISTICS,
                        session.getId(), userTextField.getText());
                String sResponse = sendTextCommand(command);
                response = createResponse(sResponse);
              }
              if (session.completeRound() ==  2) {
                if (sessionOwner) {
                  if (server != null)
                    server.finalizeSession(session.getId());
                  else {
                    String command = generateTextCommand(FINALIZE_SESSION, session.getId(), null);
                    sendTextCommand(command);
                  }
                }
                sessionOwner = false;
                finalizeGame(response);
              }
              else {
                setupAndStartRound("The round has ended!\n" +
                        "Current ranking: " + response.getRanking() +
                        "\nYour score: " + response.getScore() +
                        "\nHighest score: " + response.getHighScore());
              }
            } catch (RemoteException e1) {
              e1.printStackTrace();
            }
          }
          else {
            try {
              if (isGhost && ((gameTime%ghostTime) == 0)) {
                int index = r.nextInt(session.getSolution().size());
                submissionsTextField.setText(session.getAnswer(index));
              }
              BoggleResponse response;
              if (server != null)
                response = server.getStatistics(session.getId(), userTextField.getText());
              else {
                String command = generateTextCommand(REQUEST_STATISTICS,
                        session.getId(), userTextField.getText());
                String sResponse = sendTextCommand(command);
                response = createResponse(sResponse);
              }
              updateStatistics(response, "");
            } catch (RemoteException e1) {
              e1.printStackTrace();
            }
          }
        }
    });
  }

  /**
   * Creates a BoggleResponse object from a string received from the server.
   * Used by socket clients to parse the information received from the server.
   */
  private BoggleResponse createResponse(String sresponse) {
    String[] sval = sresponse.split("[|]");
    if (sval[0].equals("0"))
    {
      throw new BoggleException(sval[1]);
    }
    else {
      return new BoggleResponse(Integer.parseInt(sval[1]),
              Integer.parseInt(sval[2]), Integer.parseInt(sval[3]), Integer.parseInt(sval[4]));
    }
  }

  /**
   * Updates the statistics panel of the client.
   */
  private void updateStatistics(BoggleResponse response, String status) {
    if (response != null) {
      rankingLabel.setText(String.valueOf(response.getRanking()));
      highScoreLabel.setText(String.valueOf(response.getHighScore()));
      playerScoreLabel.setText(String.valueOf(response.getScore()));
      int points = response.getLatestPoints();
      if (points > 0)
        statusLabel.setText("New word! +" + points + " points.");
      else if (points < 0)
        statusLabel.setText("Repeated word... " + points + " points.");
    }
    else {
      statusLabel.setText("Connection error... Try again later");
    }
  }

  /**
   * Shows the final statistics of the session to the player.
   */
  private void finalizeGame(BoggleResponse response) {
    showMessage("The game has ended!\n" +
            "Final ranking: " + response.getRanking() +
            "\nYour score: " + response.getScore() +
            "\nHighest score: " + response.getHighScore(),
            JOptionPane.INFORMATION_MESSAGE);
    makeGUIUnplayable();
  }

  /**
   * Sends a request for a new session to the boggle server.
   */
  private void requestSessionFromServer(String players, String username) {
    int numPlayers = -1;
    try {
      numPlayers = Integer.parseInt(players);
    }
    catch (Exception e) {
      numPlayers = -1;
    }
    finally {
      if (numPlayers < 2) {
        showMessage("Incorrect number of players.\n" +
                        "At least 2 players are required to play a session.",
                JOptionPane.WARNING_MESSAGE);
        gameIdTextField.setText("");
        gameIdTextField.requestFocus();
      } else {
        try {
          if (server != null)
            session = server.createSession(numPlayers, username);
          else {
            String command = generateTextCommand(REQUEST_SESSION, numPlayers, username);
            String tsession = sendTextCommand(command);
            session = createSession(tsession);
          }
          sessionOwner = true;
          paramLabel.setText("Game Id:");
          resetStats();
          setupAndStartRound("Created session " + Integer.toString(session.getId()) + ".");
        } catch (RemoteException e) {
          showMessage(e.getMessage(), JOptionPane.WARNING_MESSAGE);
        }
      }
    }
  }

  /**
   * Creates a Session object from a string.
   * Used by socket-based clients to parse the information received from the server.
   */
  private Session createSession(String tsession) {
    String[] values = tsession.split("[|]");
    if (values[0].equals("0"))
    {
      throw new BoggleException(values[1]);
    }
    else {
      int id = Integer.parseInt(values[1]);
      String board = values[2];
      String[] solutions = values[3].split(",");
      List<String> solution = new LinkedList<>(Arrays.asList(solutions));
      return new Session(id, board, solution, null, 0);
    }
  }

  /**
   * Generates the strings that are then sent to the server socket.
   */
  private String generateTextCommand(int commandType, int iparam, String sparam) {
    String command = commandType + "|";
    switch (commandType) {
      case REQUEST_SESSION:
        command += iparam + "|" + sparam;
        break;
      case REQUEST_START:
        command += iparam;
        break;
      case SUBMIT_WORD:
        command += iparam + "|" + sparam;
        break;
      case FINALIZE_SESSION:
        command += iparam;
        break;
      case REQUEST_STATISTICS:
        command += iparam + "|" + sparam;
        break;
      case JOIN_SESSION:
        command += iparam + "|" + sparam;
        break;
      case REQUEST_SESSION_STATISTICS:
        command += iparam + "|" + sparam;
        break;
    }
    return command;
  }

  /**
   * Resets the stats panel.
   */
  private void resetStats() {
    highScoreLabel.setText("-");
    playerScoreLabel.setText("-");
    rankingLabel.setText("-");
  }

  /**
   * Displays a message to the user.
   */
  private void showMessage(String message, int type) {
    JOptionPane.showMessageDialog(null, message, "Boggle", type);
  }

  /**
   * Sends the server a request to join a given session.
   */
  private void requestJoinToServer(String gameId, String username) {
    int id = -1;
    try {
      id = Integer.parseInt(gameId);
    }
    catch (Exception e) {
      id = -1;
    }
    finally {
      if (id < 0) {
        showMessage("Incorrect game session Id.", JOptionPane.WARNING_MESSAGE);
        gameIdTextField.setText("");
        gameIdTextField.requestFocus();
      }
      else {
        try {
          if (server != null)
            session = server.joinSession(id, username);
          else {
            String command = generateTextCommand(JOIN_SESSION, id, username);
            String tsession = sendTextCommand(command);
            session = createSession(tsession);
          }
          sessionOwner = false;
          resetStats();
          setupAndStartRound("Joined session " + Integer.toString(id) + ".");
        }
        catch (RemoteException | BoggleException e) {
          showMessage(e.getMessage(), JOptionPane.WARNING_MESSAGE);
        }
      }
    }
  }

  /**
   * Sends the server a request to start the next boggle round.
   */
  private void setupAndStartRound(String message) throws RemoteException {
    statusLabel.setText("Waiting for other players.");
    makeGUIPlayable();
    showMessage(message + "\nClick OK to request the start of the round.",
            JOptionPane.INFORMATION_MESSAGE);
    if (server != null)
      server.requestStart(session.getId());
    else {
      String command = generateTextCommand(REQUEST_START, session.getId(), null);
      sendTextCommand(command);
    }
    fillBoard(session.getBoard());
    gameClock.start();
  }

  private void makeGUIPlayable() {
    userTextField.setEnabled(false);
    gameIdTextField.setEnabled(false);
    gameIdTextField.setText(String.valueOf(session.getId()));
    joinRadButton.setEnabled(false);
    createRadButton.setEnabled(false);
    sendButton.setEnabled(false);
    submissionsTextField.setText("");
    highScoreLabel.setText("-");
    playerScoreLabel.setText("-");
    rankingLabel.setText("-");
    timeLabel.setText("60");
    gameTime = 64;
  }

  /**
   * Updates the game board.
   */
  private void fillBoard(String board) {
    String[] rows = board.split(",");
    for (int i = 0; i < BOARD_DIMENSION; i++) {
      String[] cells = rows[i].split(" ");
      for (int j = 0; j < BOARD_DIMENSION; j++) {
        gameBoard.setValueAt(cells[j], i, j);
      }
    }
  }

  private void makeGUIUnplayable() {
    userTextField.setEnabled(true);
    gameIdTextField.setEnabled(true);
    gameIdTextField.setText("");
    joinRadButton.setEnabled(true);
    createRadButton.setEnabled(true);
    sendButton.setEnabled(true);
    submissionsTextField.setText("");
    submissionsTextField.setEnabled(false);
    for (int i = 0; i < BOARD_DIMENSION; i++) {
      for (int j = 0; j < BOARD_DIMENSION; j++) {
        gameBoard.setValueAt("-", i, j);
      }
    }
  }

  public static void main(String[] args) throws Exception {
    JFrame frame = new JFrame("Boggle");
    frame.setResizable(false);
    boolean isGhost = false;
    int t = 1;
    if (args.length > 1) {
      isGhost = true;
      t = Integer.valueOf(args[1]);
    }
    frame.setContentPane(new BoggleClient(args[0], t, isGhost).mainPanel);
    if (isGhost)
      frame.setTitle("GHOST");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.pack();
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    frame.setLocation(dim.width / 2 - frame.getSize().width / 2,
            dim.height / 2 - frame.getSize().height / 2);
    frame.setVisible(true);
  }
}
