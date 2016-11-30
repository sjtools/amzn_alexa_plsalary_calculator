package com.sjtools.plsalarycalculator;

import java.util.HashSet;
import java.util.Set;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;


public class PLCalcSpeechletHandler extends SpeechletRequestStreamHandler 
{

    private static final Set<String> supportedApplicationIds;

    static {
        /*
         * This Id can be found on https://developer.amazon.com/edw/home.html#/ "Edit" the relevant
         * Alexa Skill and put the relevant Application Ids in this Set.
         */
        supportedApplicationIds = new HashSet<String>();
    }


	public PLCalcSpeechletHandler() 
	{
		super(new PLCalcSpeechlet(), supportedApplicationIds);
		
	}
}
