package me.alwx.localcommunication.connection;

/**
 * @author alwx
 * @version 1.0
 */
public class Communication {
  public static final String MESSAGE = "message";
  public static final String MESSAGE_TYPE = "type";

  public static final class Connect {
    public static final String TYPE = Connect.class.getName();
    public static final String DEVICE = "device";
  }

  public static final class ConnectSuccess {
    public static final String TYPE = ConnectSuccess.class.getName();
  }
}
