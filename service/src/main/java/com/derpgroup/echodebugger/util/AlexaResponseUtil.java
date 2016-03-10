package com.derpgroup.echodebugger.util;

import com.amazon.speech.json.SpeechletResponseEnvelope;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.SsmlOutputSpeech;

public class AlexaResponseUtil {
  public static SpeechletResponseEnvelope createSimpleResponse(String title){
    return createSimpleResponse(title, title, title);
  }
  
  public static SpeechletResponseEnvelope createSimpleResponse(String title, String content){
    return createSimpleResponse(title, content, content);
  }
  
  public static SpeechletResponseEnvelope createSimpleResponse(String title, String content, String outputSpeech){
    SpeechletResponseEnvelope responseEnvelope = new SpeechletResponseEnvelope();
    responseEnvelope.setSessionAttributes(null);

    SimpleCard card = new SimpleCard();
    card.setContent(content);
    card.setTitle(title);

    SpeechletResponse speechletResponse = new SpeechletResponse();
    
    SsmlOutputSpeech output = new SsmlOutputSpeech();
    
    // TODO: So hacky
    output.setSsml(convertToSSML(outputSpeech));
    
    speechletResponse.setOutputSpeech(output);
    speechletResponse.setCard(card);
    speechletResponse.setShouldEndSession(true);
    responseEnvelope.setResponse(speechletResponse);
    return responseEnvelope;
  }
  
  public static String convertToSSML(String input){
    return "<speak>"+input+"</speak>";
  }
}
