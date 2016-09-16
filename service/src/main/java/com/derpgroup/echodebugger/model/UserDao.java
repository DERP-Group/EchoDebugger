package com.derpgroup.echodebugger.model;

import java.util.List;

public interface UserDao {
  
  public void initialize();
  
  public Boolean isInitialized();
  
  /**
   * Creates a user if they don't already exist
   * @param userId
   */
  public Boolean createUser(String userId);
  
  /**
   * Saves a user
   * @param userId
   */
  public Boolean saveUser(User user);
  
  /**
   * Returns a true/false indicating if we have an account for the id
   * @param userId
   * @return
   */
  public Boolean containsUser(String userId);

  /**
   * Returns the user account associated with this id. Null if it doesn't exist.
   * @param userId
   * @return
   */
  public User getUser(String userId);

  /**
   * Gets all data from all users
   * @return
   */
  public List<User> getAllUserData();

}
