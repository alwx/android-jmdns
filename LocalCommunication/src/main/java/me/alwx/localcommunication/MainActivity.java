package me.alwx.localcommunication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import me.alwx.localcommunication.connection.Communication;
import me.alwx.localcommunication.connection.Connection;
import me.alwx.localcommunication.connection.ConnectionWrapper;
import me.alwx.localcommunication.connection.MessageHandler;
import me.alwx.localcommunication.connection.NetworkDiscovery;

public class MainActivity extends Activity {
  private static final String DEBUG_TAG = MainActivity.class.getName();

  private Button mButtonStartServer;
  private Button mButtonConnect;

  private View.OnClickListener mButtonStartServerOnClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
      int intaddr = wifi.getConnectionInfo().getIpAddress();

      if (wifi.getWifiState() == WifiManager.WIFI_STATE_DISABLED || intaddr == 0) {
        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
      } else {
        getConnectionWrapper().stopNetworkDiscovery();
        getConnectionWrapper().startServer();
        getConnectionWrapper().setHandler(mServerHandler);
      }
    }
  };

  private View.OnClickListener mButtonConnectOnClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      connect();
    }
  };

  private Connection.ConnectionListener mConnectionListener = new Connection.ConnectionListener() {
    @Override
    public void onConnection() {
      getConnectionWrapper().send(
          new HashMap<String, String>() {{
            put(Communication.MESSAGE_TYPE, Communication.Connect.TYPE);
            put(Communication.Connect.DEVICE, Build.MODEL);
          }}
      );
    }
  };

  private Handler mServerHandler = new MessageHandler(MainActivity.this) {
    @Override
    public void onMessage(String type, JSONObject message) {
      try {
        if (type.equals(Communication.Connect.TYPE)) {
          final String deviceFrom = message.getString(Communication.Connect.DEVICE);
          Toast.makeText(getApplicationContext(), "Device: " + deviceFrom, Toast.LENGTH_SHORT).show();
          getConnectionWrapper().send(
              new HashMap<String, String>() {{
                put(Communication.MESSAGE_TYPE, Communication.ConnectSuccess.TYPE);
              }}
          );
        }
      } catch (JSONException e) {
        Log.d(DEBUG_TAG, "JSON parsing exception: " + e);
      }
    }
  };

  private Handler mClientHandler = new MessageHandler(MainActivity.this) {
    @Override
    public void onMessage(String type, JSONObject message) {
      if (type.equals(Communication.ConnectSuccess.TYPE)) {
        Toast.makeText(getApplicationContext(), "Connection succesfully performed!", Toast.LENGTH_SHORT).show();
      }
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mButtonStartServer = (Button) findViewById(R.id.button_start_server);
    mButtonConnect = (Button) findViewById(R.id.button_connect);
    mButtonConnect.setEnabled(false);

    ((SampleApplication) getApplication()).createConnectionWrapper(
        new ConnectionWrapper.OnCreatedListener() {
          @Override
          public void onCreated() {
            mButtonConnect.setEnabled(true);
          }
        }
    );
  }

  @Override
  protected void onResume() {
    super.onResume();

    mButtonStartServer.setOnClickListener(mButtonStartServerOnClickListener);
    mButtonConnect.setOnClickListener(mButtonConnectOnClickListener);
  }

  @Override
  protected void onPause() {
    mButtonStartServer.setOnClickListener(null);
    mButtonConnect.setOnClickListener(null);

    super.onPause();
  }

  @Override
  protected void onStop() {
    getConnectionWrapper().reset();
    super.onStop();
  }

  private void connect() {
    getConnectionWrapper().findServers(new NetworkDiscovery.OnFoundListener() {
      @Override
      public void onFound(javax.jmdns.ServiceInfo info) {
        if (info != null && info.getInet4Addresses().length > 0) {
          getConnectionWrapper().stopNetworkDiscovery();
          getConnectionWrapper().connectToServer(
              info.getInet4Addresses()[0],
              info.getPort(),
              mConnectionListener
          );
          getConnectionWrapper().setHandler(mClientHandler);
        }
      }
    });
  }

  private ConnectionWrapper getConnectionWrapper() {
    return ((SampleApplication) getApplication()).getConnectionWrapper();
  }
}
