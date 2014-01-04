package me.alwx.localcommunication.connection;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

/**
 * @author alwx
 * @version 1.0
 */
public class NetworkDiscovery {
  private final String DEBUG_TAG = NetworkDiscovery.class.getName();
  private final String TYPE = "_alwx._tcp.local.";
  private final String SERVICE_NAME = "LocalCommunication";

  private Context mContext;
  private JmDNS mJmDNS;
  private ServiceInfo mServiceInfo;
  private ServiceListener mServiceListener;
  private WifiManager.MulticastLock mMulticastLock;

  public NetworkDiscovery(Context context) {
    mContext = context;
    try {
      WifiManager wifi = (WifiManager) mContext.getSystemService(android.content.Context.WIFI_SERVICE);
      WifiInfo wifiInfo = wifi.getConnectionInfo();
      int intaddr = wifiInfo.getIpAddress();

      byte[] byteaddr = new byte[]{
          (byte) (intaddr & 0xff),
          (byte) (intaddr >> 8 & 0xff),
          (byte) (intaddr >> 16 & 0xff),
          (byte) (intaddr >> 24 & 0xff)
      };
      InetAddress addr = InetAddress.getByAddress(byteaddr);
      mJmDNS = JmDNS.create(addr);
    } catch (IOException e) {
      Log.d(DEBUG_TAG, "Error in JmDNS creation: " + e);
    }
  }

  public void startServer(int port) {
    try {
      wifiLock();
      mServiceInfo = ServiceInfo.create(TYPE, SERVICE_NAME, port, SERVICE_NAME);
      mJmDNS.registerService(mServiceInfo);
    } catch (IOException e) {
      Log.d(DEBUG_TAG, "Error in JmDNS initialization: " + e);
    }
  }

  public void findServers(final OnFoundListener listener) {
    mJmDNS.addServiceListener(TYPE, mServiceListener = new ServiceListener() {
      @Override
      public void serviceAdded(ServiceEvent serviceEvent) {
        ServiceInfo info = mJmDNS.getServiceInfo(serviceEvent.getType(), serviceEvent.getName());
        listener.onFound(info);
      }

      @Override
      public void serviceRemoved(ServiceEvent serviceEvent) {
      }

      @Override
      public void serviceResolved(ServiceEvent serviceEvent) {
        mJmDNS.requestServiceInfo(serviceEvent.getType(), serviceEvent.getName(), 1);
      }
    });
  }

  public void reset() {
    if (mJmDNS != null) {
      if (mServiceListener != null) {
        mJmDNS.removeServiceListener(TYPE, mServiceListener);
        mServiceListener = null;
      }
      mJmDNS.unregisterAllServices();
    }
    if (mMulticastLock != null && mMulticastLock.isHeld()) {
      mMulticastLock.release();
    }
  }

  private void wifiLock() {
    WifiManager wifiManager = (WifiManager) mContext.getSystemService(android.content.Context.WIFI_SERVICE);
    mMulticastLock = wifiManager.createMulticastLock(SERVICE_NAME);
    mMulticastLock.setReferenceCounted(true);
    mMulticastLock.acquire();
  }

  public interface OnFoundListener {
    void onFound(ServiceInfo info);
  }
}
