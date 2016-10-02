package com.derpgroup.echodebugger.model;

import java.util.List;

public interface UserDao {

	public void initialize();

	public Boolean isInitialized();

	/**
	 * Creates a user
	 * @param echoId
	 */
	public User createUser(String echoId);

	/**
	 * Saves a user
	 * @param user
	 */
	public Boolean saveUser(User user);

	/**
	 * Returns the user account associated with this echoId. Returns null if it doesn't exist.
	 * @param echoId
	 * @return
	 */
	public User getUserByEchoId(String echoId);

	/**
	 * Returns the user account associated with this id. Returns null if it doesn't exist.
	 * @param id
	 * @return
	 */
	public User getUserById(String id);

	/**
	 * Gets all data from all users
	 * @return
	 */
	public List<User> getAllUserData();

	/**
	 * Deletes a user
	 * @param user
	 * @return
	 */
	public User deleteUser(User user);

}
