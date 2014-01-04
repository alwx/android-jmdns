package me.alwx.localcommunication;

import android.app.Application;

import me.alwx.localcommunication.connection.ConnectionWrapper;

/**
 * @author alwx
 * @version 1.0
 */
public class SampleApplication extends Application {
  private ConnectionWrapper mConnectionWrapper;

  @Override
  public void onCreate() {
    super.onCreate();
  }

  public void createConnectionWrapper(ConnectionWrapper.OnCreatedListener listener) {
    mConnectionWrapper = new ConnectionWrapper(getApplicationContext(), listener);
  }

  public ConnectionWrapper getConnectionWrapper() {
    return mConnectionWrapper;
  }
}
