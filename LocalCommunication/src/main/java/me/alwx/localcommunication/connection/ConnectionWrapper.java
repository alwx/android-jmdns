package me.alwx.localcommunication.connection;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import org.json.JSONObject;

import java.net.Inet4Address;
import java.util.Map;

/**
 * easy-to-use wrapper for clients
 *
 * @author alwx
 * @version 1.0
 */
public class ConnectionWrapper {
  private Context mContext;
  private NetworkDiscovery mNetworkDiscovery;
  private Connection mConnection;

  /**
   * wrapper constructor
   * see example of usage in {@link me.alwx.localcommunication.MainActivity}
   *
   * @param context  application context
   * @param listener listener, that will be called after all preparation finished
   *                 (see {@link me.alwx.localcommunication.connection.ConnectionWrapper.OnCreatedListener})
   */
  public ConnectionWrapper(final Context context,
                           final OnCreatedListener listener) {
    mContext = context;
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        mNetworkDiscovery = new NetworkDiscovery(mContext);
        return null;
      }

      @Override
      protected void onPostExecute(Void aVoid) {
        if (listener != null) {
          listener.onCreated();
        }
      }
    }.execute((Void) null);
  }

  /**
   * sets message handler
   * you need to call it to handle messages
   *
   * @param handler message handler
   */
  public void setHandler(Handler handler) {
    if (mConnection != null) {
      mConnection.setHandler(handler);
    }
  }

  /**
   * starts server
   * you need to use this function only for phone you need to register as server
   */
  public void startServer() {
    mConnection = new Connection();
    mConnection.createServer();

    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        mNetworkDiscovery.startServer(mConnection.getLocalPort());

        return null;
      }
    }.execute((Void) null);
  }

  /**
   * performs servers search
   *
   * @param listener listener, that will be called after something found
   *                 (see {@link me.alwx.localcommunication.connection.NetworkDiscovery.OnFoundListener})
   */
  public void findServers(final NetworkDiscovery.OnFoundListener listener) {
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        mNetworkDiscovery.findServers(listener);

        return null;
      }
    }.execute((Void) null);
  }

  /**
   * function to connect to server when you know IP address & port
   *
   * @param address            server address
   * @param port               server port
   * @param connectionListener listener, that will be called after connection
   *                           (see {@link me.alwx.localcommunication.connection.Connection.ConnectionListener})
   */
  public void connectToServer(Inet4Address address,
                              int port,
                              Connection.ConnectionListener connectionListener) {
    mConnection = new Connection(connectionListener);
    mConnection.connectToServer(address, port);
  }

  /**
   * sends message
   *
   * @param values key-value map
   */
  public void send(Map values) {
    if (mConnection != null) {
      JSONObject jsonObject = new JSONObject(values);
      mConnection.sendMessage(jsonObject.toString());
    }
  }

  /**
   * closes connection
   */
  public void reset() {
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        if (mConnection != null) {
          mConnection.closeConnection();
          mConnection = null;
        }
        stopNetworkDiscovery();

        return null;
      }
    }.execute((Void) null);
  }

  /**
   * stops network discovery
   */
  public void stopNetworkDiscovery() {
    if (mNetworkDiscovery != null) {
      mNetworkDiscovery.reset();
    }
  }

  public interface OnCreatedListener {
    void onCreated();
  }
}