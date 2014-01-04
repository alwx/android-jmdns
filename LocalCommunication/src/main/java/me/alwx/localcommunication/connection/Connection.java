package me.alwx.localcommunication.connection;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * class for all connection-related purposes
 *
 * @author alwx
 * @version 1.0
 */
public class Connection {
  private static final String DEBUG_TAG = Connection.class.getName();

  private Server mServer;
  private Client mClient;
  private Socket mSocket;
  private Handler mHandler;
  private int mPort = -1;
  private ConnectionListener mConnectionListener;

  public Connection() {
  }

  public Connection(ConnectionListener listener) {
    mConnectionListener = listener;
  }

  /**
   * sets handler for connection
   *
   * @param handler message handler
   */
  public void setHandler(Handler handler) {
    mHandler = handler;
  }

  /**
   * connects to server
   *
   * @param address server IP address
   * @param port    server port
   */
  public void connectToServer(InetAddress address, int port) {
    mClient = new Client(address, port);
  }

  /**
   * closes connection
   */
  public void closeConnection() {
    if (mServer != null) {
      mServer.closeConnection();
    }
    if (mClient != null) {
      mClient.closeConnection();
    }
  }

  /**
   * creates server & connects to it
   */
  public void createServer() {
    mServer = new Server();
  }

  /**
   * returns local port
   * used in {@link me.alwx.localcommunication.connection.ConnectionWrapper}
   *
   * @return port
   */
  public int getLocalPort() {
    return mPort;
  }

  private void setLocalPort(int port) {
    mPort = port;
  }

  /**
   * sends message to client
   *
   * @param msg message string
   */
  public void sendMessage(String msg) {
    if (mClient != null) {
      mClient.sendMessage(msg);
    }
  }

  private synchronized void setSocket(Socket socket) {
    if (socket == null) {
      Log.d(DEBUG_TAG, "Setting a null socket.");
    }
    if (mSocket != null) {
      if (mSocket.isConnected()) {
        try {
          mSocket.close();
        } catch (IOException e) {
          Log.d(DEBUG_TAG, "IOException while closing socket: " + e);
        }
      }
    }
    mSocket = socket;
  }

  private Socket getSocket() {
    return mSocket;
  }

  private synchronized void updateMessages(String msg, boolean local) {
    Bundle messageBundle = new Bundle();
    messageBundle.putString(Communication.MESSAGE, msg);
    Message message = new Message();
    message.setData(messageBundle);

    if (mHandler != null) {
      mHandler.sendMessage(message);
    }
  }

  private class Server {
    private Thread mThread;
    private ServerSocket mServerSocket;

    public Server() {
      mThread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            mServerSocket = new ServerSocket(0);
            setLocalPort(mServerSocket.getLocalPort());

            while (!Thread.currentThread().isInterrupted()) {
              setSocket(mServerSocket.accept());
              connectToServer(mSocket.getInetAddress(), mSocket.getPort());
            }
          } catch (IOException e) {
            Log.d(DEBUG_TAG, "Server IOException: " + e);
          }
        }
      });
      mThread.start();
    }

    /**
     * interrupts thread and closes server socket
     */
    public void closeConnection() {
      mThread.interrupt();
      try {
        mServerSocket.close();
      } catch (IOException e) {
        Log.e(DEBUG_TAG, "Error when closing server socket: " + e);
      }
    }
  }

  private class Client {
    private InetAddress mInetAddress;
    private int mPort;
    private Thread mSendingThread;
    private Thread mReceivingThread;

    public Client(InetAddress inetAddress, int port) {
      mInetAddress = inetAddress;
      mPort = port;

      mSendingThread = new Thread(new SendingThread());
      mSendingThread.start();
    }

    /**
     * sends given message to server
     *
     * @param msg message text
     */
    public void sendMessage(String msg) {
      try {
        Socket socket = getSocket();
        if (socket == null) {
          Log.d(DEBUG_TAG, "Socket is null");
        } else if (socket.getOutputStream() == null) {
          Log.d(DEBUG_TAG, "Socket output stream is null");
        }

        PrintWriter out = new PrintWriter(
            new BufferedWriter(
                new OutputStreamWriter(getSocket().getOutputStream())
            ),
            true
        );
        out.println(msg);
        out.flush();
        updateMessages(msg, true);
      } catch (UnknownHostException e) {
        Log.d(DEBUG_TAG, "Unknown Host: ", e);
      } catch (IOException e) {
        Log.d(DEBUG_TAG, "I/O Exception: ", e);
      } catch (Exception e) {
        Log.d(DEBUG_TAG, "Error: ", e);
      }
      Log.d(DEBUG_TAG, "Client sent message: " + msg);
    }

    /**
     * closes connection to server
     */
    public void closeConnection() {
      mReceivingThread.interrupt();
      mSendingThread.interrupt();
      try {
        getSocket().close();
      } catch (IOException e) {
        Log.e(DEBUG_TAG, "Error when closing server socket: " + e);
      }
    }

    /**
     * thread to send messages
     */
    private class SendingThread implements Runnable {
      BlockingQueue<String> mMessageQueue;
      private int QUEUE_CAPACITY = 10;

      public SendingThread() {
        mMessageQueue = new ArrayBlockingQueue<String>(QUEUE_CAPACITY);
      }

      @Override
      public void run() {
        try {
          if (getSocket() == null) {
            setSocket(new Socket(mInetAddress, mPort));
            Log.d(DEBUG_TAG, "Client-side socket initialized.");
          } else {
            Log.d(DEBUG_TAG, "Socket already initialized. skipping!");
          }

          // create receiving thread
          mReceivingThread = new Thread(new ReceivingThread());
          mReceivingThread.start();
        } catch (UnknownHostException e) {
          Log.d(DEBUG_TAG, "Initializing socket failed, UHE", e);
        } catch (IOException e) {
          Log.d(DEBUG_TAG, "Initializing socket failed, IOE.", e);
        }

        if (mConnectionListener != null) {
          mConnectionListener.onConnection();
        }

        // take message from queue and send it
        while (true) {
          try {
            String msg = mMessageQueue.take();
            sendMessage(msg);
          } catch (InterruptedException e) {
            Log.d(DEBUG_TAG, "Message sending loop interrupted, exiting");
          }
        }
      }
    }

    /**
     * thread to receive messages
     */
    private class ReceivingThread implements Runnable {
      @Override
      public void run() {
        BufferedReader input;
        try {
          input = new BufferedReader(new InputStreamReader(
              mSocket.getInputStream()));
          while (!Thread.currentThread().isInterrupted()) {
            String messageStr = null;
            messageStr = input.readLine();
            if (messageStr != null) {
              Log.d(DEBUG_TAG, "Read from the stream: " + messageStr);
              updateMessages(messageStr, false);
            } else {
              Log.d(DEBUG_TAG, "Null message");
              break;
            }
          }
          input.close();
        } catch (IOException e) {
          Log.e(DEBUG_TAG, "Server loop error: ", e);
        }
      }
    }
  }

  public interface ConnectionListener {
    void onConnection();
  }
}
