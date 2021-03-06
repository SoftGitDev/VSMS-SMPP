package com.softtech.smpp.session;

import com.softtech.smpp.bean.Outbind;

/**
 * @author SUTHAR
 */
public class OutbindRequest {
  private final String systemId;
  private final String password;

  public OutbindRequest(String systemId, String password) {
    this.systemId = systemId;
    this.password = password;
  }

  public OutbindRequest(Outbind outbind) {
    this(outbind.getSystemId(), outbind.getPassword());
  }

  public String getSystemId() {
    return this.systemId;
  }

  public String getPassword() {
    return this.password;
  }
}
