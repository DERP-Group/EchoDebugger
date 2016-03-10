package com.derpgroup.echodebugger.model;

import io.dropwizard.setup.Environment;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.derpgroup.echodebugger.configuration.MainConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ContentDao {
  private final Logger LOG = LoggerFactory.getLogger(ContentDao.class);

  private Map<String,Map<String, Object>> userData = new ConcurrentHashMap<String,Map<String, Object>>();
  private String contentFile;
  
  public ContentDao(MainConfig config, Environment env){
    contentFile = config.getEchoDebuggerConfig().getContentFile();
  }
  
  public void createNewUser(String userId){
    userData.put(userId, null);
  }
  
  public boolean containsUser(String userId){
    return userData.containsKey(userId);
  }
  
  public void saveUserData(String echoId, Map<String, Object> content){
    userData.put(echoId, content);
  }
  
  public Map<String, Object> getUserContent(String userId){
    return userData.get(userId);
  }

  public Map<String, Map<String, Object>> getUserData() {
    return userData;
  }

  public void setUserData(Map<String, Map<String, Object>> userData) {
    this.userData = userData;
  }
  
  
  // Local helper functions
  protected Map<String, Map<String, Object>> readUserResponsesFromFile(String fileName) throws IOException{
    final File file = new File(fileName);
    if(file.createNewFile()){
      LOG.info("Created blank user data file for use");
    }
    
    String content = new String(Files.readAllBytes(Paths.get(fileName)),Charset.defaultCharset());
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(content, new TypeReference<Map<String, Map<String, Object>>>(){});
  }
  
  public void saveUsersToFile(){
    Map<String, Map<String, Object>> localUserDataMap = new HashMap<String, Map<String, Object>>();
    localUserDataMap.putAll(userData);
    try {
      writeToFile(localUserDataMap, contentFile);
    } catch (IOException e) {
      LOG.debug("There was a problem saving user data",e);
    }
  }
  
  public void writeToFile(Map<String, Map<String, Object>> userDataMap, String fileName) throws IOException{
    final File file = new File(fileName);
    if(file.createNewFile()){
      LOG.info("Created new user data file for use");
    }
    
    ObjectMapper mapper = new ObjectMapper();
    String content = mapper.writeValueAsString(userDataMap);
    
    FileUtils.writeStringToFile(file, content, Charset.defaultCharset());
  }
}
