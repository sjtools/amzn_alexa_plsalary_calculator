package com.sjtools.plsalarycalculator;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.Directive;
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioItem;
import com.amazon.speech.speechlet.interfaces.audioplayer.PlayBehavior;
import com.amazon.speech.speechlet.interfaces.audioplayer.Stream;
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.PlayDirective;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SsmlOutputSpeech;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PLCalcSpeechlet implements Speechlet {
	/*
	 * various messages
	 */
	private static final String HELP_SHORT_STRING = 
			" You can ask a question like: "
            + " how much is, net, from, 15000, Or, "
            + " how much is, be two be, from 17000, Or"
            + " just say either gross, or, net, or, be two be.";		
    
	private static final String HELP_STRING = 
    		" You can calculate net, gross, or be two be profit for a given salary. "
    		+ HELP_SHORT_STRING;
    
    private static final String WELCOME_STRING =
            "Welcome. " + HELP_STRING;

    public static final String REPROMPT_DEFAULT = 
    		"For instructions, please say help me ";
    		
	
    private static final String SHORT_REMINDER=  
    		" Please ask your salary question.";
    
    private static final Logger log = LoggerFactory.getLogger(PLCalcSpeechlet.class);

    
    /*
     * calculator intents
     */
    private static final String CALC_REGULAR_INTENT = "AskRegularIntent";
    
    /**
     * intent slots
     */
    private static final String SALARY_VALUE = 	"SalaryValue";
    private static final String SALARY_PERIOD = "SalaryPeriod";
    private static final String SALARY_CHOICE = "SalaryChoice";
    private static final String SALARY_COMMUTE_CHOICE = "SalaryCommuteChoice";
    
    /**
     * intent slot values
     */
    
    private static final String SALARY_CHOICE_NET 		= "net";
    private static final String SALARY_CHOICE_GROSS 	= "gross";
    private static final String SALARY_CHOICE_B2B 		= "be 2 be";
    
    private static final String SALARY_PERIOD_MONTHLY 	= "monthly";
    private static final String SALARY_PERIOD_YEARLY 	= "annual";
    
    private static final String SALARY_COMMUTE_CHOICE_REGULAR 		= "regular";
    private static final String SALARY_COMMUTE_CHOICE_EXTENDED 		= "extended";
    
    /*
     * steps
     */
    private static final String STEPS_DATA = "STEPS_DATA";
    private static final String CALC_SALARY_TYPE = "1";
    private static final String CALC_REGULAR_SALARY = "2";
    private static final String CALC_B2B_SALARY = "3";
    private static final String CALC_PERIOD = "4";
    private static final String CALC_COPYRIGHTS = "5";
    private static final String  CALC_COMMUTE = "6";

    /*
     * step update response
     */ 
    private class StepUpdateResponse
    {
    	boolean status; 
    	String message; //aux message with response status
    }
    
    @Override
	public SpeechletResponse onLaunch(LaunchRequest request, Session session) throws SpeechletException {
        
		return SpeechletResponse.newAskResponse(
				getResponse(false, WELCOME_STRING), getReprompt(false, REPROMPT_DEFAULT));
	}

	@Override
	public void onSessionEnded(SessionEndedRequest request, Session session) throws SpeechletException {
		//nothing to do here for now
	}

	@Override
	public void onSessionStarted(SessionStartedRequest request, Session session) throws SpeechletException {
	//set first step
		session.setAttribute(STEPS_DATA, CALC_SALARY_TYPE);
	}
	@Override
	public SpeechletResponse onIntent(IntentRequest request, Session session) throws SpeechletException {

		
		Intent intent = request.getIntent();
		
		CalcData calcData = generateCalcData(session);
		
		String intentName  = (null != intent ) ? intent.getName() : null;
		if (CALC_REGULAR_INTENT.equals(intentName))
		{
			return calculateResponse(intent, session, calcData);
		}
		if ("AMAZON.HelpIntent".equals(intentName)) 
		{
			return getHelpResponse();
	    }
		if ("AMAZON.RepeatIntent".equals(intentName)) 
		{
			if (calcData==null || calcData.currentStep == null)
			{
				throw new SpeechletException("Calculator data error");
			}		
			return getHelpResponse(calcData.currentStep.message, REPROMPT_DEFAULT);
	    }
		if ("AMAZON.StartOverIntent".equals(intentName)) 
		{
			if (calcData==null || calcData.currentStep == null)
			{
				throw new SpeechletException("Calculator data error");
			}		
			clearCalcData(session, calcData);
			return getHelpResponse(calcData.currentStep.message, REPROMPT_DEFAULT);
	    } 

		if ("AMAZON.StopIntent".equals(intentName) || 
				("AMAZON.CancelIntent".equals(intentName))) 
	    {
	        return SpeechletResponse.newTellResponse(getResponse(false,"Goodbye"));
	    } 
		throw new SpeechletException("Invalid intent name:" + intentName);
	}

	private SpeechletResponse calculateResponse(Intent intent, Session session, CalcData calcData) throws SpeechletException 
	{	
		if (calcData==null || calcData.currentStep == null)
		{
			throw new SpeechletException("Calculator data error");
		}		
		if (null == intent.getSlots() || intent.getSlots().size()==0)
		{// no slots in intent
			return getHelpResponse("I am sorry. I did not understand that. " + calcData.currentStep.message, SHORT_REMINDER);
		}		
		return updateStep(intent, session, calcData);
	}
	
	/* 
	 * update current step and subsequent ones if data available
	 * find first step to start from, unless all are done, so calculate		 * 
	 */
	private SpeechletResponse updateStep(Intent intent, Session session, CalcData calcData) 
	{
		StepUpdateResponse stepUpdateAnswer = null;
		int count = intent.getSlots().size();
		boolean currentStepForGivenIntent = true; //others will have it false
		while (count>=0)
		{
			count --;
			CalcStep step = calcData.currentStep;
			if (step == null)
				break; //already finished, this will never be the first step in while
			
			if (CALC_SALARY_TYPE.equals(step.stepID))
			{
				
				stepUpdateAnswer = updateSalaryTypeStep(currentStepForGivenIntent,
						step,
						(null!=intent.getSlot(step.slot)) ? intent.getSlot(step.slot).getValue() : null, 
						calcData);
			}
			if (CALC_B2B_SALARY.equals(step.stepID) ||
					CALC_REGULAR_SALARY.equals(step.stepID))
			{				
				stepUpdateAnswer = updateSalaryValueStep(currentStepForGivenIntent,
						step,
						(null!=intent.getSlot(step.slot)) ? intent.getSlot(step.slot).getValue() : null, 
						calcData);
			}
			if (CALC_PERIOD.equals(step.stepID))
			{
				
				stepUpdateAnswer = updateSalaryPeriodStep(currentStepForGivenIntent,
						step,
						(null!=intent.getSlot(step.slot)) ? intent.getSlot(step.slot).getValue() : null, 
						calcData);
			}			
			if (CALC_COPYRIGHTS.equals(step.stepID))
			{
				
				stepUpdateAnswer = updateSalaryCopyrightsStep(currentStepForGivenIntent,
						step,
						(null!=intent.getSlot(step.slot)) ? intent.getSlot(step.slot).getValue() : null, 
						calcData);
			}			
			if (CALC_COMMUTE.equals(step.stepID))
			{
				
				stepUpdateAnswer = updateSalaryCommuteStep(currentStepForGivenIntent,
						step,
						(null!=intent.getSlot(step.slot)) ? intent.getSlot(step.slot).getValue() : null, 
						calcData);
			}			
			currentStepForGivenIntent = false; //after first round same intent could have moved to next step
			 if (stepUpdateAnswer!=null && stepUpdateAnswer.status ==false)
			 {
				 //fail on first invalid intent or step
				 break;
			 }
		}
		//update session with collected data and current step
		if (null == calcData.currentStep)
		{
			session.removeAttribute(STEPS_DATA);
		}
		else
		{
			session.setAttribute(STEPS_DATA, calcData.currentStep.stepID);
		}
		calcData.collectedData.forEach((k, v ) ->
				{
					session.setAttribute(k, v);
				}); 
		
		//do the housekeeping
		if (stepUpdateAnswer == null)
		{//unknown intent
			return getHelpResponse(
					"I am sorry. I did not understand that. " + calcData.currentStep.message, 
					REPROMPT_DEFAULT);
		}
		//something was processed
		if (stepUpdateAnswer.status == true) //step updated correctly, moved to next
		{
			if (calcData.currentStep == null) 
			{//data collection finished, show me the money now :-)
				String response = showMeTheMoney(calcData); 
				clearCalcData(session, calcData); //prepare to start over
				return getMoneyResponse(
						response, 
						"What salary do you have in mind?");
			}
			else
			{// ask for new current step data
				return getHelpResponse(calcData.currentStep.message, REPROMPT_DEFAULT);
			}
		}
		else
		{
			// ask again, something was wrong
			return getHelpResponse(stepUpdateAnswer.message + calcData.currentStep.message, REPROMPT_DEFAULT);
		}
	}

	private StepUpdateResponse updateSalaryTypeStep(boolean currentStepForGivenIntent, 
													CalcStep step, String value ,CalcData calcData) 
	{
		StepUpdateResponse response = getDefaultStepUpdateResponse();
		if (value == null)
		{//ask again if that is first step in intent, otherwise just ask
			// this is a trick to avoid redundant message aired.
			if (!currentStepForGivenIntent) 
			{
				response.message = "";
			}
			return response;
		}
		if (SALARY_CHOICE_NET.equals(value))
		{
			calcData.collectedData.put(step.stepID, SALARY_CHOICE_NET);
			calcData.currentStep = step.nextCalcSteps.get(SALARY_CHOICE_NET);
		}
		else if (SALARY_CHOICE_GROSS.equals(value))
			{
				calcData.collectedData.put(step.stepID, SALARY_CHOICE_GROSS);
				calcData.currentStep = step.nextCalcSteps.get(SALARY_CHOICE_GROSS);
			}
			else if (SALARY_CHOICE_B2B.equals(value))
				{
					calcData.collectedData.put(step.stepID, SALARY_CHOICE_B2B);
					calcData.currentStep = step.nextCalcSteps.get(SALARY_CHOICE_B2B);
				}
				else
					{//ask again
						return response;
					}
		//looks like salary type was properly set
		response.status = true;
		response.message =value;
		return response;
	}

	private StepUpdateResponse updateSalaryValueStep(boolean currentStepForGivenIntent,
								CalcStep step, String value ,CalcData calcData) 
	{
		StepUpdateResponse response = getDefaultStepUpdateResponse();
		if (value == null)
		{//ask again if that is first step in intent, otherwise just ask
			// this is a trick to avoid redundant message aired.
			if (!currentStepForGivenIntent) 
			{
				response.message = "";
			}
			return response;
		}
		double salary = 0;
		try
		{
			salary = Double.parseDouble(value);
		}
		catch (NumberFormatException e)
		{
			response.status = false;
			response.message = "I am sorry, I did not understand salary amount. ";
			return response;
		}
		if (salary > 0 )
		{
			calcData.collectedData.put(step.stepID, ""+salary);
			//for b2b this will set current to NULL as this will be the last step
			calcData.currentStep = step.nextCalcSteps.get(SALARY_PERIOD);
			response.status = true;
			response.message = value;
		}
		else
		{
			response.status = false;
			response.message = "Salary amount must be greater than zero I guess. ";
		}
		return response;
	}

	private StepUpdateResponse updateSalaryPeriodStep(boolean currentStepForGivenIntent,
											CalcStep step, String value ,CalcData calcData) 
	{
		StepUpdateResponse response = getDefaultStepUpdateResponse();
		if (value == null)
		{//ask again if that is first step in intent, otherwise just ask
			// this is a trick to avoid redundant message aired.
			if (!currentStepForGivenIntent) 
			{
				response.message = "";
			}
			return response;
		}
		if (SALARY_PERIOD_MONTHLY.equals(value))
		{
			calcData.collectedData.put(step.stepID, SALARY_PERIOD_MONTHLY);
			calcData.currentStep = step.nextCalcSteps.get(SALARY_VALUE);
		}
		else if (SALARY_PERIOD_YEARLY.equals(value))
			{
				calcData.collectedData.put(step.stepID, SALARY_PERIOD_YEARLY);
				calcData.currentStep = step.nextCalcSteps.get(SALARY_VALUE);
			}
			else
			{//ask again
				return response;
			}
		//looks like salary type was properly set
		response.status = true;
		response.message =value;
		return response;
	}
	private StepUpdateResponse updateSalaryCopyrightsStep(boolean currentStepForGivenIntent,
									CalcStep step, String value ,CalcData calcData) 
	{
		StepUpdateResponse response = getDefaultStepUpdateResponse();
		if (value == null)
		{//ask again if that is first step in intent, otherwise just ask
			// this is a trick to avoid redundant message aired.
			if (!currentStepForGivenIntent) 
			{
				response.message = "";
			}
			return response;
		}
		double salary = 0;
		try
		{
			salary = Double.parseDouble(value);
		}
		catch (NumberFormatException e)
		{
			response.status = false;
			response.message = "I am sorry, I did not understand percentage value. ";
			return response;
		}
		if (salary >= 0 && salary <=100)
		{
			calcData.collectedData.put(step.stepID, ""+salary);
			calcData.currentStep = step.nextCalcSteps.get(SALARY_COMMUTE_CHOICE);
			response.status = true;
			response.message = value;
		}
		else
		{
			response.status = false;
			response.message = "Percentage value must be between 0. and 100. ";
		}
		return response;
	}

	private StepUpdateResponse updateSalaryCommuteStep(boolean currentStepForGivenIntent,
									CalcStep step, String value ,CalcData calcData) 
	{
		StepUpdateResponse response = getDefaultStepUpdateResponse();
		if (value == null)
		{//ask again if that is first step in intent, otherwise just ask
			// this is a trick to avoid redundant message aired.
			if (!currentStepForGivenIntent) 
			{
				response.message = "";
			}
			return response;
		}
		if (SALARY_COMMUTE_CHOICE_EXTENDED.equals(value))
		{
			calcData.collectedData.put(step.stepID, SALARY_COMMUTE_CHOICE_EXTENDED);
			//will set current to NULL as this is final step
			calcData.currentStep = step.nextCalcSteps.get(SALARY_VALUE);
		}
		else if (SALARY_COMMUTE_CHOICE_REGULAR.equals(value))
			{
				calcData.collectedData.put(step.stepID, SALARY_COMMUTE_CHOICE_REGULAR);
				//will set current to NULL as this is final step
				calcData.currentStep = step.nextCalcSteps.get(SALARY_VALUE);
			}
			else
			{//ask again
				return response;
			}
		//looks like salary type was properly set
		response.status = true;
		response.message =value;
		return response;
	}	
	private StepUpdateResponse getDefaultStepUpdateResponse() {
		StepUpdateResponse response = new StepUpdateResponse();
		response.status = false;
		response.message = "I am sorry I did not understand that. ";
		return response;
	}

	private String showMeTheMoney(CalcData calcData) 
	{
		/* Ideally this should yield one of the two following answers:
		 * For be two be with XXX net on an invoice one will get YYY profit
		 * For a regular employment that will be ZZZZ net and WWWW gross monthly
		 * 
		 * However, since this is too complicated to handle and Alexa Skill example, 
		 * just reply kindly with what was asked for for now. :-)
		 */
		
		StringBuilder sb = new StringBuilder();
		double val = 0;
		//salary type
		String type = (String) calcData.collectedData.get(CALC_SALARY_TYPE);
		if (SALARY_CHOICE_B2B.equals(type))
		{
			String v = (String)calcData.collectedData.get(CALC_B2B_SALARY);
			try
			{
			val = Double.valueOf(v).doubleValue();
			}
			catch (NumberFormatException e)
			{
				
			}
			sb.append("Thank you. You asked for profit from be, two, be, invoice with net amount of " + roundValueString(val)+", ");
			sb.append(" Unfortunately I am still researching on the topic. Online calculators may help you with this for now.");
			/*the real thing
			double res = getB2BMoneyMagic(val);
			sb.append("For be two be ");
			sb.append(" with " + val + " net on an invoice one will get ");
			sb.append(res);
			sb.append(" profit.");
			*/
		}
		else
		{
			String v = (String)calcData.collectedData.get(CALC_REGULAR_SALARY);
			try
			{
			val = Double.valueOf(v).doubleValue();
			}
			catch (NumberFormatException e)
			{
				
			}
			boolean isValMonthly= (SALARY_PERIOD_MONTHLY.equals(calcData.collectedData.get(CALC_PERIOD))) ?  
					true : false;
			boolean isValNet = (SALARY_CHOICE_NET.equals(calcData.collectedData.get(CALC_SALARY_TYPE))) ?  
					true : false;
			double rights = 0;
			v = (String)calcData.collectedData.get(CALC_COPYRIGHTS);
			try
			{
				rights = Double.valueOf(v).doubleValue();
			}
			catch (NumberFormatException e)
			{
				
			}
			boolean isCommuteRegular= (SALARY_COMMUTE_CHOICE_REGULAR.equals(calcData.collectedData.get(CALC_COMMUTE))) ?  
					true : false;

			sb.append("Thank you. You asked to calculate, monthly, ");
			sb.append( isValNet ? " gross, " : " net, " );
			sb.append( "from, " + (isValMonthly ? roundValueString(val) : roundValueString(val/12.0)));
			sb.append( isValNet ? " net, " : " gross, " );
			sb.append( (rights>0) ? (" with, " + roundValueString(rights) + ", percent of intellectual property rights "): "");
			sb.append( isCommuteRegular ? " and, regular, " : " and, extended, " );
			sb.append("commuting costs. ");
			sb.append(" Unfortunately I am still researching on the topic. Online calculators may help you with this for now.");

			/*
			val = isValMonthly ? val : Math.ceil(val/12.0);
			double res[] = getRegularMoneyMagic(val, isValNet, rights, isCommuteRegular);
			sb.append("Thank you. That will be, montly, ");
			sb.append( roundValueString(res[0]) + " gross, and, ");
			sb.append( roundValueString(res[1]) + " net, ");
			sb.append( (rights>0) ? (" given, " + roundValueString(rights) + ", percent of intellectual property rights "): "");
			sb.append( isCommuteRegular ? " and, regular, " : " and, extended, " );
			sb.append("commuting costs. ");
			//take into account minimal salary in Poland, gross - 1850, net 1355			
			if (res[0]<1850 || res[1]<1355)
			{
				sb.append("Please mind that minimal salary in Poland is, " + 1850 + " gross and, " + " 1355 net.");
			}
			*/
		}
	
		return sb.toString();
	}

	/*
	 * round value and remove precision in string representation
	 * e.g. 10.0 -> string "10"
	 */
	private String roundValueString(double val) {
		return String.format(" %1$.0f ", Math.ceil(val)); 
	}

	private double[] getRegularMoneyMagic(double val, boolean isValNet, 
									double rights, boolean isCommuteRegular) {
		double res[] = new double[2]; //0 - gross , 1 - net
		double zus_healthpension_sick = (9.76 + 1.5 + 2.45)/100.0;
		double payable_sick = 7.75/100.0;
		double regCommute  = 111.25;
		double extCommute = 438.72;
		double taxAllowance = 46.33;
		double taxRate = 18.0/100.0;
		
		//good luck to calculate this
		if (isValNet)
		{
			res[0] = 0;
			res[1] = val;
		}
		else
		{
			res[0] = val;
			double result = res[0];
			double taxable = 
					//deduct 50% cost for copyrights part
					result* (rights/100.0)*(50.0/100.0)
					+
					//deduct commute cost for the non copyrights part
					result* (1.0-rights/100.0)- ((isCommuteRegular) ? regCommute : extCommute);
			double incomeDeduct = result * zus_healthpension_sick;
			taxable -= incomeDeduct;
		
			double nonCopyrights = result* (1.0-rights/100.0);
			nonCopyrights -= nonCopyrights*zus_healthpension_sick;
			nonCopyrights -= (isCommuteRegular) ? regCommute : extCommute;
			nonCopyrights *= (1.0-taxRate);
			
			double withCopyrights = result*(rights/100.0);
			
			result = 0;
			res[1] = result;
		}
		return res;
	}

	private double getB2BMoneyMagic(double val) {
		
		//good luck to calculate this
		return val;
	}

	private CalcData generateCalcData(Session session)
	{
		/* -----startStep(salary_type)----
		 * |       |               |
		 * net     gross          b2b
		 *  |        |             |
		 *  |        |             |
		 *value(v>0) value(>0)    invoice net value (>0)
		 * ----------              |
		 *      |                <calculate>
		 * yearly/monthly
		 * |
		 * intellectual prop rights %<0,100>
		 * |
		 * commuting cost (regular/extended)
		 * |
		 * <calculate>
		 * 
		 */
		//build step nodes
		CalcData data = new CalcData();
		CalcStep rootStep = createCalcStep(
				data,
				HELP_STRING,
				SALARY_CHOICE,
				CALC_SALARY_TYPE, 
				new HashMap<String,CalcStep>()
					);
		CalcStep b2bValue = createCalcStep(
				data,
				" For be, two, be, tell net value on an invoice ",
				SALARY_VALUE,
				CALC_B2B_SALARY, 
				new HashMap<String,CalcStep>() //this is final step, no next ones 
				);
		data.calcSteps.add(b2bValue);
		CalcStep netValue = createCalcStep(
				data,
				" Tell gross amount to calculate net for.",
				SALARY_VALUE,
				CALC_REGULAR_SALARY, 
				new HashMap<String,CalcStep>()
				);
		CalcStep grossValue = createCalcStep(
				data,
				" Tell net amount to calculate gross for.",
				SALARY_VALUE,
				CALC_REGULAR_SALARY, 
				new HashMap<String,CalcStep>()
				);
		CalcStep periodType = createCalcStep(
				data,
				" Is that monthly or annual salary?",
				SALARY_PERIOD,
				CALC_PERIOD, 
				new HashMap<String,CalcStep>()
				);
		CalcStep crightsValue = createCalcStep(
				data,
				" Tell intellectual property rights percentage for salary.",
				SALARY_VALUE,
				CALC_COPYRIGHTS, 
				new HashMap<String,CalcStep>()
				);
		CalcStep commuteValue = createCalcStep(
				data,
				" Is that regular or extended commuting cost?",
				SALARY_COMMUTE_CHOICE,
				CALC_COMMUTE, 
				new HashMap<String,CalcStep>() //this is final step, no next ones 
				);		
		//build step flow
		rootStep.nextCalcSteps.put(SALARY_CHOICE_B2B, b2bValue);
		rootStep.nextCalcSteps.put(SALARY_CHOICE_GROSS, grossValue);
		rootStep.nextCalcSteps.put(SALARY_CHOICE_NET, netValue);
		grossValue.nextCalcSteps.put(SALARY_PERIOD, periodType);
		netValue.nextCalcSteps.put(SALARY_PERIOD, periodType);
		periodType.nextCalcSteps.put(SALARY_VALUE, crightsValue);
		crightsValue.nextCalcSteps.put(SALARY_COMMUTE_CHOICE, commuteValue);

		//set starting step
		data.startStep = rootStep;
		String setCurrentStep = (String)session.getAttribute(STEPS_DATA); 
		if (setCurrentStep == null)
			data.currentStep = rootStep;
		else
		{
			data.calcSteps.forEach((s) -> 
					{
						if (s.stepID.equals(setCurrentStep))
							data.currentStep = s;
					});
		}
		//set already collected values
		data.calcSteps.forEach((s) -> 
		{
			if (session.getAttribute(s.stepID) != null)
			{
				data.collectedData.put(s.stepID, session.getAttribute(s.stepID));
			}
		});
		
		
		return data;
	}
	private CalcStep createCalcStep(CalcData calcData, String message, String slot, String id, Map<String,CalcStep> next) 
	{
		//build single step node
		CalcStep step = new CalcStep();
		step.message = message;
		step.slot = slot;
		step.stepID = id;
		step.nextCalcSteps = next;
		calcData.calcSteps.add(step);
		return step;
	}	 
	private void clearCalcData(Session session, CalcData data)
	{
		//start from start node again, clear already collected data
		session.setAttribute(STEPS_DATA, CALC_SALARY_TYPE);
		data.calcSteps.forEach((s) -> 
				{
					session.removeAttribute(s.stepID);
				}
				);
		data.currentStep = data.startStep;
	}

	private SpeechletResponse getHelpResponse() {
		return getHelpResponse(HELP_STRING,SHORT_REMINDER);
	}


	private SpeechletResponse getMoneyResponse(String resp, String reprompt) 
	{
		SpeechletResponse s = getHelpResponse(true, "<audio src=\"https://s3.amazonaws.com/lambda-function-bucket-us-east-1-sjtools/atmsound.mp3\"/> " + resp, 
												false, reprompt);
									
	/*	PlayDirective d = new PlayDirective();
		d.setPlayBehavior(PlayBehavior.ENQUEUE);
		AudioItem a = new AudioItem();
		Stream st = new Stream();
		st.set
		st.setOffsetInMilliseconds(0);
		st.setUrl("https://s3.amazonaws.com/lambda-function-bucket-us-east-1-sjtools/atmsound.mp3");
		st.setToken("ATM_Machine_sound_token");
		a.setStream(st);
		d.setAudioItem(a);
		ArrayList<Directive> toplay = new ArrayList<Directive>();
		toplay.add(d);
		s.setDirectives(toplay);
		*/
		
		return s;
	}

	private SpeechletResponse getHelpResponse(String resp, String reprompt) 
	{
		return getHelpResponse(false, resp, false, reprompt);
	}
	private SpeechletResponse getHelpResponse(boolean isSSMLResp, String resp, boolean isSSMLRepr, String reprompt) 
	{
		SpeechletResponse s = new SpeechletResponse();
		return SpeechletResponse.newAskResponse(
				getResponse(isSSMLResp,resp), getReprompt(isSSMLRepr, reprompt));
	}
	private SsmlOutputSpeech getSSML(String message)
	{
		SsmlOutputSpeech s = new SsmlOutputSpeech();
		s.setSsml("<speak>"+ message + "</speak>");
		return s;
	}
	private PlainTextOutputSpeech getPlainText(String message)
	{
		PlainTextOutputSpeech resp = new PlainTextOutputSpeech();
		resp.setText(message);
		return resp;		
	}
	private OutputSpeech getResponse(boolean isSSML, String message)
	{
		return isSSML ? getSSML(message) : getPlainText(message);
	}
	private  Reprompt getReprompt(boolean isSSML, String message)
	{
		Reprompt rep = new Reprompt();
		rep.setOutputSpeech(isSSML ? getSSML(message) : getPlainText(message));
		return rep;
	}
	

    private static final String SAMPLE_INPUT_STRING = 
    	"{  \"session\": {    \"sessionId\": \"SessionId1111111\",    \"application\": {      \"applicationId\": \"amzn1.ask.skill.1111111\"    },    \"attributes\": {},    \"user\": {      \"userId\": \"amzn1.ask.account.1111111\"    },    \"new\": true  },  \"request\": {    \"type\": \"IntentRequest\",    \"requestId\": \"EdwRequestId.1111111\",    \"locale\": \"en-US\",    \"timestamp\": \"2016-11-26T16:02:32Z\",    \"intent\": {      \"name\": \"AskRegularIntent\",      \"slots\": {        \"SalaryPeriod\": {          \"name\": \"SalaryPeriod\"        },        \"SalaryValue\": {          \"name\": \"SalaryValue\"        },        \"JobType\": {          \"name\": \"JobType\"        },        \"SalaryType\": {          \"name\": \"SalaryType\"        }      }    }  },  \"version\": \"1.0\"}";
    
    private static final String SAMPLE_INPUT_STRING2= "{  \"session\": {    \"sessionId\": \"SessionId1111111\",    \"application\": {      \"applicationId\": \"amzn1.ask.skill.1111111\"    },    \"attributes\": {},    \"user\": {      \"userId\": \"amzn1.ask.account.1111111\"    },    \"new\": true  },  \"request\": {    \"type\": \"IntentRequest\",    \"requestId\": \"EdwRequestId.1111111\",    \"locale\": \"en-US\",    \"timestamp\": \"2016-11-26T16:02:32Z\",    \"intent\": {      \"name\": \"AskRegularIntent\",      \"slots\": {        \"SalaryPeriod\": {          \"name\": \"SalaryPeriod\"        },        \"SalaryValue\": {          \"name\": \"SalaryValue\"        },        \"JobType\": {          \"name\": \"JobType\"        },        \"SalaryChoice\": {          \"name\": \"SalaryChoice\", \"value\": \"net\"        }      }    }  },  \"version\": \"1.0\"}";
    private static final String SAMPLE_INPUT_STRING3="{  \"session\": {    \"sessionId\": \"SessionId1111111\",    \"application\": {      \"applicationId\": \"amzn1.ask.skill.1111111\"    },    \"attributes\": {},    \"user\": {      \"userId\": \"amzn1.ask.account.1111111\"    },    \"new\": true  },  \"request\": {    \"type\": \"IntentRequest\",    \"requestId\": \"EdwRequestId.1111111\",    \"locale\": \"en-US\",    \"timestamp\": \"2016-11-26T16:02:32Z\",    \"intent\": {      \"name\": \"AskRegularIntent\",      \"slots\": {        \"SalaryPeriod\": {          \"name\": \"SalaryPeriod\", \"value\": \"monthly\"        },        \"SalaryValue\": {          \"name\": \"SalaryValue\", \"value\": \"1000\"        },        \"JobType\": {          \"name\": \"JobType\"        },        \"SalaryChoice\": {          \"name\": \"SalaryChoice\", \"value\": \"b2b\"        }      }    }  },  \"version\": \"1.0\"}";
    private static final String SAMPLE_INPUT_STRING4="{  \"session\": {    \"sessionId\": \"SessionId1111111\",    \"application\": {      \"applicationId\": \"amzn1.ask.skill.1111111\"    },    \"attributes\": {},    \"user\": {      \"userId\": \"amzn1.ask.account.1111111\"    },    \"new\": true  },  \"request\": {    \"type\": \"IntentRequest\",    \"requestId\": \"EdwRequestId.1111111\",    \"locale\": \"en-US\",    \"timestamp\": \"2016-11-26T16:02:32Z\",    \"intent\": {      \"name\": \"AskRegularIntent\",      \"slots\": {        \"SalaryPeriod\": {          \"name\": \"SalaryPeriod\", \"value\": \"monthly\"        },        \"SalaryValue\": {          \"name\": \"SalaryValue\", \"value\": \"100\"        },        \"JobType\": {          \"name\": \"JobType\"        },        \"SalaryChoice\": {          \"name\": \"SalaryChoice\", \"value\": \"net\"        }      }    }  },  \"version\": \"1.0\"}";
    private static final String SAMPLE_INPUT_STRING5="{  \"session\": {    \"sessionId\": \"SessionId1111111\",    \"application\": {      \"applicationId\": \"amzn1.ask.skill.1111111\"    },    \"attributes\": {},    \"user\": {      \"userId\": \"amzn1.ask.account.1111111\"    },    \"new\": true  },  \"request\": {    \"type\": \"IntentRequest\",    \"requestId\": \"EdwRequestId.1111111\",    \"locale\": \"en-US\",    \"timestamp\": \"2016-11-26T16:02:32Z\",    \"intent\": {      \"name\": \"AskRegularIntent\",      \"slots\": {        \"SalaryPeriod\": {          \"name\": \"SalaryPeriod\", \"value\": \"monthly\"        },        \"SalaryValue\": {          \"name\": \"SalaryValue\", \"value\": \"10.5\"        },        \"SalaryCommuteChoice\": {          \"name\": \"SalaryCommuteChoice\", \"value\": \"regular\"        },        \"SalaryChoice\": {          \"name\": \"SalaryChoice\", \"value\": \"be 2 be\"        }      }    }  },  \"version\": \"1.0\"}";    
    private static final String SAMPLE_INPUT_STRING6="{  \"session\": {    \"sessionId\": \"SessionId1111111\",    \"application\": {      \"applicationId\": \"amzn1.ask.skill.1111111\"    },    \"attributes\": {},    \"user\": {      \"userId\": \"amzn1.ask.account.1111111\"    },    \"new\": true  },  \"request\": {    \"type\": \"IntentRequest\",    \"requestId\": \"EdwRequestId.1111111\",    \"locale\": \"en-US\",    \"timestamp\": \"2016-11-26T16:02:32Z\",    \"intent\": {      \"name\": \"AskRegularIntent\",      \"slots\": {        \"SalaryPeriod\": {          \"name\": \"SalaryPeriod\", \"value\": \"monthly\"        },        \"SalaryValue\": {          \"name\": \"SalaryValue\", \"value\": \"100\"        },        \"SalaryCommuteChoice\": {          \"name\": \"SalaryCommuteChoice\", \"value\": \"regular\"        },        \"SalaryChoice\": {          \"name\": \"SalaryChoice\", \"value\": \"net\"        }      }    }  },  \"version\": \"1.0\"}";    

    
    private static final String SAMPLE_INPUT_STRING_net="{  \"session\": {    \"sessionId\": \"SessionId1111111\",    \"application\": {      \"applicationId\": \"amzn1.ask.skill.1111111\"    },    \"attributes\": {},    \"user\": {      \"userId\": \"amzn1.ask.account.1111111\"    },    \"new\": true  },  \"request\": {    \"type\": \"IntentRequest\",    \"requestId\": \"EdwRequestId.1111111\",    \"locale\": \"en-US\",    \"timestamp\": \"2016-11-26T16:02:32Z\",    \"intent\": {      \"name\": \"AskRegularIntent\",      \"slots\": {        \"SalaryPeriod\": {          \"name\": \"SalaryPeriod\", \"value\": \"monthly\"        },        \"SalaryValue\": {          \"name\": \"SalaryValue\", \"value\": \"100\"        },        \"SalaryCommuteChoice\": {          \"name\": \"SalaryCommuteChoice\", \"value\": \"regular\"        },        \"SalaryChoice\": {          \"name\": \"SalaryChoice\", \"value\": \"be two be\"        }      }    }  },  \"version\": \"1.0\"}";    
    private static final String SAMPLE_INPUT_STRING_val="{  \"session\": {    \"sessionId\": \"SessionId1111111\",    \"application\": {      \"applicationId\": \"amzn1.ask.skill.1111111\"    },    \"attributes\": {},    \"user\": {      \"userId\": \"amzn1.ask.account.1111111\"    },    \"new\": true  },  \"request\": {    \"type\": \"IntentRequest\",    \"requestId\": \"EdwRequestId.1111111\",    \"locale\": \"en-US\",    \"timestamp\": \"2016-11-26T16:02:32Z\",    \"intent\": {      \"name\": \"AskRegularIntent\",      \"slots\": {        \"SalaryPeriod\": {          \"name\": \"SalaryPeriod\", \"value\": \"monthly\"        },        \"SalaryValue\": {          \"name\": \"SalaryValue\", \"value\": \"100\"        },        \"SalaryCommuteChoice\": {          \"name\": \"SalaryCommuteChoice\", \"value\": \"regular\"        },        \"SalaryChoice\": {          \"name\": \"SalaryChoice\", \"value\": \"be two be\"        }      }    }  },  \"version\": \"1.0\"}";    
    private static final String SAMPLE_INPUT_STRING_period="{  \"session\": {    \"sessionId\": \"SessionId1111111\",    \"application\": {      \"applicationId\": \"amzn1.ask.skill.1111111\"    },    \"attributes\": {},    \"user\": {      \"userId\": \"amzn1.ask.account.1111111\"    },    \"new\": true  },  \"request\": {    \"type\": \"IntentRequest\",    \"requestId\": \"EdwRequestId.1111111\",    \"locale\": \"en-US\",    \"timestamp\": \"2016-11-26T16:02:32Z\",    \"intent\": {      \"name\": \"AskRegularIntent\",      \"slots\": {        \"SalaryPeriod\": {          \"name\": \"SalaryPeriod\", \"value\": \"monthly\"        },        \"SalaryValue\": {          \"name\": \"SalaryValue\", \"value\": \"10.5\"        },        \"SalaryCommuteChoice\": {          \"name\": \"SalaryCommuteChoice\", \"value\": \"regular\"        },        \"SalaryChoice\": {          \"name\": \"SalaryChoice\", \"value\": \"be two be\"        }      }    }  },  \"version\": \"1.0\"}";    

    
    public static void main(String[] args) throws IOException {
        PLCalcSpeechletHandler handler = new PLCalcSpeechletHandler();

        InputStream input = new ByteArrayInputStream(SAMPLE_INPUT_STRING6.getBytes());
        OutputStream output = new ByteArrayOutputStream();

        handler.handleRequest(input, output, null);
        String sampleOutputString = output.toString();
        System.out.println(sampleOutputString);
    }
}
