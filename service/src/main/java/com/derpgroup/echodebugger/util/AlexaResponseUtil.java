package com.derpgroup.echodebugger.util;

import org.apache.commons.lang3.StringUtils;

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

		SpeechletResponse speechletResponse = new SpeechletResponse();

		// Only generate a card if there's content for it
		if(StringUtils.isNotBlank(title) || StringUtils.isNotBlank(content)){
			SimpleCard card = new SimpleCard();
			card.setContent(content);
			card.setTitle(title);
			speechletResponse.setCard(card);
		}

		// Only generate output speech if there's content for it
		if(StringUtils.isNotBlank(outputSpeech)){
			SsmlOutputSpeech output = new SsmlOutputSpeech();
			output.setSsml(convertToSSML(outputSpeech));
			speechletResponse.setOutputSpeech(output);
		}

		speechletResponse.setShouldEndSession(true);
		responseEnvelope.setResponse(speechletResponse);
		return responseEnvelope;
	}

	public static String convertToSSML(String input){
		return "<speak>"+input+"</speak>";
	}
}
