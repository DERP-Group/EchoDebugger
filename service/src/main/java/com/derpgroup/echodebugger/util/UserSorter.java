package com.derpgroup.echodebugger.util;

import java.time.Instant;
import java.util.Comparator;

import com.derpgroup.echodebugger.model.User;

public class UserSorter {
  
  public static final Comparator<User> SORT_USER_BY_MOST_RECENT_UPLOAD = new Comparator<User>() {

    public int compare(User user1, User user2) {

      Instant user1Upload = user1.getLastUploadTime();
      Instant user2Upload = user2.getLastUploadTime();

      if(user1Upload != null && user2Upload != null){
        return user2.getLastUploadTime().compareTo(user1.getLastUploadTime());
      }
      else if(user1Upload == null && user2Upload != null){
        return 1;
      }
      else if(user1Upload != null && user2Upload == null){
        return -1;
      }
      else{
        return 0;
      }
    }

  };
}
