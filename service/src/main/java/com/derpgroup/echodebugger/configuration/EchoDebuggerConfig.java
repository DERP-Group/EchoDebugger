package com.derpgroup.echodebugger.configuration;

public class EchoDebuggerConfig {
  private String password;
  private Integer maxAllowedResponseLength;
  private Boolean debugMode;
  private String contentFile;
  private Integer saveRate;

  public String getPassword() {return password;}
  public void setPassword(String password) {this.password = password;}
  public Integer getMaxAllowedResponseLength() {return maxAllowedResponseLength;}
  public void setMaxAllowedResponseLength(Integer maxAllowedResponseLength) {this.maxAllowedResponseLength = maxAllowedResponseLength;}
  public Boolean getDebugMode() {return debugMode;}
  public void setDebugMode(Boolean debugMode) {this.debugMode = debugMode;}
  public String getContentFile() {return contentFile;}
  public void setContentFile(String contentFile) {this.contentFile = contentFile;}
  public Integer getSaveRate() {return saveRate;}
  public void setSaveRate(Integer saveRate) {this.saveRate = saveRate;}
}
