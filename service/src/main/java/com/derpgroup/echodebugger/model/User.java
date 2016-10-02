package com.derpgroup.echodebugger.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

public class User {

	private UUID id;
	private String echoId;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "PST")
	private Instant creationTime;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "PST")
	private Instant lastUploadTime;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "PST")
	private Instant lastWebDownloadTime;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "PST")
	private Instant lastEchoDownloadTime;

	private int numContentUploads;
	private int numContentDownloads;
	private int numCharactersUploaded;
	private int numCharactersDownloaded;
	private int numUploadsTooLarge;

	private Map<String,IntentResponses> intents = new HashMap<>();

	public User(){}
	public User(String echoId){
		this.echoId = echoId;
		this.id = UUID.randomUUID();
		creationTime = Instant.now();
	}

	public UUID getId() {return id;}
	public void setId(UUID id) {this.id = id;}
	public String getEchoId() {return echoId;}
	public void setEchoId(String echoId) {this.echoId = echoId;}
	public Map<String, IntentResponses> getIntents() {return intents;}
	public void setIntents(Map<String, IntentResponses> intents) {this.intents = intents;}
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
