/**
 * Copyright (C) 2015 David Phillips
 * Copyright (C) 2015 Eric Olson
 * Copyright (C) 2015 Rusty Gerard
 * Copyright (C) 2015 Paul Winters
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.derpgroup.echodebugger.manager;

import com.amazon.speech.json.SpeechletResponseEnvelope;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.SsmlOutputSpeech;

/**
 * Manager class for dispatching input messages.
 *
 * @author David
 * @since 0.0.1
 */
public class EchoDebuggerManager {

  // TODO: Move actual processing of stuff into here lol
  
  public SpeechletResponseEnvelope createSimpleResponse(String title){
    return createSimpleResponse(title, title, title);
  }
  
  public SpeechletResponseEnvelope createSimpleResponse(String title, String content){
    return createSimpleResponse(title, content, content);
  }
  
  public SpeechletResponseEnvelope createSimpleResponse(String title, String content, String outputSpeech){
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
  
  public String convertToSSML(String input){
    return "<speak>"+input+"</speak>";
  }
  
}
