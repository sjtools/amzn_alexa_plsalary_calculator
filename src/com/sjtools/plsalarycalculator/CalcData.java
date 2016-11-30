package com.sjtools.plsalarycalculator;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CalcData 
{
	public List<CalcStep> calcSteps = new LinkedList<CalcStep>();
	public CalcStep startStep = null;
	public CalcStep currentStep = null;
	//already collected steps id, value
	public Map<String, Object> collectedData = new HashMap<String, Object>();
}
