package com.derpgroup.echodebugger.model;

import io.dropwizard.setup.Environment;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.derpgroup.echodebugger.configuration.MainConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class UserDaoLocalImpl implements UserDao{
	private final Logger LOG = LoggerFactory.getLogger(UserDaoLocalImpl.class);

	private Map<String,String> mapOfIdToUserId = new HashMap<>();
	private Map<String,User> mapOfUsersByEchoId = new HashMap<>();
	private String contentFile;
	private Boolean initialized = false;
	private ObjectMapper mapper;

	public UserDaoLocalImpl(MainConfig config, Environment env){
		contentFile = config.getEchoDebuggerConfig().getContentFile();
		mapper = new ObjectMapper().registerModule(new JavaTimeModule());
		mapper.configure( SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false );
		mapper.configure( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false );
	}

	/**
	 * Loads the user account data from the local data file
	 */
	@Override
	public void initialize(){
		try {
			List<User> userList = readUsersFromFile(contentFile);
			for(User user : userList){
				mapOfUsersByEchoId.put(user.getEchoId(), user);
				mapOfIdToUserId.put(user.getId().toString(), user.getEchoId());
			}
			initialized = true;
		} catch (IOException e) {
			LOG.error("Could not initialize users from data file",e);
		}
	}

	@Override
	public Boolean isInitialized(){
		return initialized;
	}

	@Override
	public User createUser(String echoId){
		User user = mapOfUsersByEchoId.get(echoId);
		if(user != null){
			// TODO: Consider an exception here. We shouldn't be trying to create a user where one exists
			return user;
		}

		user = new User(echoId);
		mapOfUsersByEchoId.put(echoId, user);
		if(user.getId() != null){
			mapOfIdToUserId.put(user.getId().toString(), echoId);
		}
		return user;
	}

	@Override
	public Boolean saveUser(User user){
		mapOfUsersByEchoId.put(user.getEchoId(), user);
		return true;
	}

	@Override
	public User getUserById(String id){
		String echoId = mapOfIdToUserId.get(id);
		if(StringUtils.isEmpty(echoId)){return null;}
		return getUserByEchoId(echoId);
	}

	@Override
	public User getUserByEchoId(String echoId){
		return mapOfUsersByEchoId.get(echoId);
	}

	@Override
	public List<User> getAllUserData() {
		List<User> usersList = new ArrayList<User>();
		for(Entry<String, User> entry : mapOfUsersByEchoId.entrySet()){
			usersList.add(entry.getValue());
		}
		return usersList;
	}

	@Override
	public User deleteUser(User user){
		if(mapOfUsersByEchoId.containsKey(user.getEchoId())){
			mapOfUsersByEchoId.remove(user.getEchoId());
		}
		saveUsersToFile();
		return user;
	}

	@Override
	public IntentResponses deleteIntent(User user, String intentName){
		return user.getIntents().remove(intentName);
	}

	// Local helper functions
	protected List<User> readUsersFromFile(String fileName) throws IOException{
		final File file = new File(fileName);
		if(file.createNewFile()){
			LOG.info("Created blank user data file for use");
			writeToFile(new ArrayList<User>(),fileName);
		}

		String content = new String(Files.readAllBytes(Paths.get(fileName)),Charset.defaultCharset());
		return mapper.readValue(content, new TypeReference<List<User>>(){});
	}

	public void saveUsersToFile(){
		List<User> userList = getAllUserData();
		try {
			writeToFile(userList, contentFile);
		} catch (IOException e) {
			LOG.debug("There was a problem saving user data",e);
		}
	}

	public void writeToFile(List<User> userList, String fileName) throws IOException{
		final File file = new File(fileName);
		if(file.createNewFile()){
			LOG.info("Created new user data file for use");
		}
		String content = mapper.writeValueAsString(userList);

		FileUtils.writeStringToFile(file, content, Charset.defaultCharset());
	}
}
