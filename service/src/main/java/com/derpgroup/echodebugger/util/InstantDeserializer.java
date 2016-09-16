package com.derpgroup.echodebugger.util;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * This class is temporary and should be removed once a conversion occurs (Sept 15th, 2016)
 */
public class InstantDeserializer extends JsonDeserializer<Instant> {

  @Override
  public Instant deserialize(JsonParser jsonParser, DeserializationContext arg1) throws IOException, JsonProcessingException {
    
    // Try to deserialize from the legacy form
    try{
      Map<String,Long> map = jsonParser.readValueAs(new TypeReference<Map<String,Long>>(){});
      Long epochSeconds = map.get("epochSecond");
      Long nanoAdjustment = map.get("nano");
      if(epochSeconds != null && nanoAdjustment != null){
        return Instant.ofEpochSecond(epochSeconds, nanoAdjustment);
      }
    }
    catch(Exception e){}
    
    // Else deserialize to the modern form
    try{
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").withZone(ZoneId.of("America/Los_Angeles"));
      return Instant.from(formatter.parse(jsonParser.getText()));
    }
    catch(Exception e){}
    return null;
  }
}