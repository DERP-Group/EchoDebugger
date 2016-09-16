package com.derpgroup.echodebugger.model;

import java.time.Instant;
import java.util.Map;

import com.derpgroup.echodebugger.util.InstantDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class User {

  private String echoId;
  private Map<String, Object> data;
  private int numContentUploads;
  private int numContentDownloads;
  private int numCharactersUploaded;
  private int numCharactersDownloaded;
  
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "PST")
  @JsonDeserialize(using = InstantDeserializer.class)
  private Instant creationTime;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "PST")
  @JsonDeserialize(using = InstantDeserializer.class)
  private Instant lastUploadTime;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "PST")
  @JsonDeserialize(using = InstantDeserializer.class)
  private Instant lastWebDownloadTime;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "PST")
  @JsonDeserialize(using = InstantDeserializer.class)
  private Instant lastEchoDownloadTime;
  private int numUploadsTooLarge;
  
  public User(){}
  public User(String echoId){
    this.echoId = echoId;
    creationTime = Instant.now();
  }
  
  public String getEchoId() {return echoId;}
  public void setEchoId(String echoId) {this.echoId = echoId;}
  public Map<String, Object> getData() {return data;}
  public void setData(Map<String, Object> data) {this.data = data;}
  public int getNumContentUploads() {return numContentUploads;}
  public void setNumContentUploads(int numContentUploads) {this.numContentUploads = numContentUploads;}
  public int getNumContentDownloads() {return numContentDownloads;}
  public void setNumContentDownloads(int numContentDownloads) {this.numContentDownloads = numContentDownloads;}
  public int getNumCharactersUploaded() {return numCharactersUploaded;}
  public void setNumCharactersUploaded(int numCharactersUploaded) {this.numCharactersUploaded = numCharactersUploaded;}
  public int getNumCharactersDownloaded() {return numCharactersDownloaded;}
  public void setNumCharactersDownloaded(int numCharactersDownloaded) {this.numCharactersDownloaded = numCharactersDownloaded;}
  public Instant getCreationTime() {return creationTime;}
  public void setCreationTime(Instant creationTime) {this.creationTime = creationTime;}
  public Instant getLastUploadTime() {return lastUploadTime;}
  public void setLastUploadTime(Instant lastUploadTime) {this.lastUploadTime = lastUploadTime;}
  public Instant getLastWebDownloadTime() {return lastWebDownloadTime;}
  public void setLastWebDownloadTime(Instant lastWebDownloadTime) {this.lastWebDownloadTime = lastWebDownloadTime;}
  public Instant getLastEchoDownloadTime() {return lastEchoDownloadTime;}
  public void setLastEchoDownloadTime(Instant lastEchoDownloadTime) {this.lastEchoDownloadTime = lastEchoDownloadTime;}
  public int getNumUploadsTooLarge() {return numUploadsTooLarge;}
  public void setNumUploadsTooLarge(int numUploadsTooLarge) {this.numUploadsTooLarge = numUploadsTooLarge;}
}
