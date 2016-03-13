package com.derpgroup.echodebugger.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.derpgroup.echodebugger.configuration.MainConfig;
import com.derpgroup.echodebugger.model.UserDaoLocalImpl;

/**
 * This Thread pairs with the UserDaoLocalImpl class, and will regularly save user accounts to disk.
 * @author David
 */
public class UserDaoLocalImplThread extends Thread {

  private final Logger LOG = LoggerFactory.getLogger(UserDaoLocalImplThread.class);
  
  private Integer usersFileSaveRate;
  private UserDaoLocalImpl dao;
  
  public UserDaoLocalImplThread(MainConfig config, UserDaoLocalImpl dao){
    usersFileSaveRate = config.getEchoDebuggerConfig().getSaveRate();
    this.dao = dao;
  }

  // Just loop for all time saving things
  public void run(){
    
    LOG.info("Starting the UserDaoLocalImplThread with a save rate of "+usersFileSaveRate+" seconds.");
    
    int sleepTime = usersFileSaveRate*1000;
    while(true){
      
      try {
        Thread.sleep(sleepTime);
        if(dao.isInitialized()){
          dao.saveUsersToFile();
        }
        else{
          LOG.error("The UserDaoLocalImpl is not initialized. Cannot save users.");
        }
      }
      // This must never kill the thread, else all is lost...
      catch (Throwable e) {
        LOG.error("There was a problem with the UserDaoLocalImplThread",e);
      }
    }
  }
}
