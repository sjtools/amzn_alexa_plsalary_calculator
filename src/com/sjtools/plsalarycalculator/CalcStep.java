package com.sjtools.plsalarycalculator;

import java.util.HashMap;
import java.util.Map;

public class CalcStep 
{
	public String message;
	public String slot;
	public Map<String,CalcStep> nextCalcSteps=null;
	public String stepID;
}
