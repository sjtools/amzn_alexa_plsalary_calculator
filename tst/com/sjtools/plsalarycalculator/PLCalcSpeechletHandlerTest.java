package com.sjtools.plsalarycalculator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Test;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class PLCalcSpeechletHandlerTest {

    private static final String SAMPLE_INPUT_STRING = 
    	"{  \"session\": {    \"sessionId\": \"SessionId.9eef5473-6aa2-48f8-958b-6da61eee5d2a\",    \"application\": {      \"applicationId\": \"amzn1.ask.skill.018e7ec2-6815-472e-995c-5c00c3ba49a6\"    },    \"attributes\": {},    \"user\": {      \"userId\": \"amzn1.ask.account.AHU5O5NU6HIDHGWH3D6TX7GHIRZENBSZ36FRLPUSZDZH45IROBFN2KVUAOWLMD4HRHKXHAH4LKNIKCIBMZ6HAPGICTJSAZDYMDWYTXH6NSKKEPPW6HJKSFTPUFZZKOQZLA6MDIEULYIIFESG5FRMMSRVBOFVRXNEYNDBNOEXQYIL4OBWCPURZ3JMKCWHUWESS5EFQJPVCFDSXKA\"    },    \"new\": true  },  \"request\": {    \"type\": \"IntentRequest\",    \"requestId\": \"EdwRequestId.efcd43f3-a388-42d2-a496-483c6d94c795\",    \"locale\": \"en-US\",    \"timestamp\": \"2016-11-26T16:02:32Z\",    \"intent\": {      \"name\": \"AskFullIntent\",      \"slots\": {        \"SalaryPeriod\": {          \"name\": \"SalaryPeriod\"        },        \"SalaryValue\": {          \"name\": \"SalaryValue\"        },        \"JobType\": {          \"name\": \"JobType\"        },        \"SalaryType\": {          \"name\": \"SalaryType\"        }      }    }  },  \"version\": \"1.0\"}";
    
    private static final String SAMPLE_INPUT_STRING2= "{  \"session\": {    \"sessionId\": \"SessionId.9eef5473-6aa2-48f8-958b-6da61eee5d2a\",    \"application\": {      \"applicationId\": \"amzn1.ask.skill.018e7ec2-6815-472e-995c-5c00c3ba49a6\"    },    \"attributes\": {},    \"user\": {      \"userId\": \"amzn1.ask.account.AHU5O5NU6HIDHGWH3D6TX7GHIRZENBSZ36FRLPUSZDZH45IROBFN2KVUAOWLMD4HRHKXHAH4LKNIKCIBMZ6HAPGICTJSAZDYMDWYTXH6NSKKEPPW6HJKSFTPUFZZKOQZLA6MDIEULYIIFESG5FRMMSRVBOFVRXNEYNDBNOEXQYIL4OBWCPURZ3JMKCWHUWESS5EFQJPVCFDSXKA\"    },    \"new\": true  },  \"request\": {    \"type\": \"IntentRequest\",    \"requestId\": \"EdwRequestId.efcd43f3-a388-42d2-a496-483c6d94c795\",    \"locale\": \"en-US\",    \"timestamp\": \"2016-11-26T16:02:32Z\",    \"intent\": {      \"name\": \"AskFullIntent\",      \"slots\": {        \"SalaryPeriod\": {          \"name\": \"SalaryPeriod\"        },        \"SalaryValue\": {          \"name\": \"SalaryValue\"        },        \"JobType\": {          \"name\": \"JobType\"        },        \"SalaryChoice\": {          \"name\": \"SalaryChoice\", \"value\": \"net\"        }      }    }  },  \"version\": \"1.0\"}";
    private static final String EXPECTED_OUTPUT_STRING = "{\"FOO\": \"BAR\"}";

    @Test
    public void testLambdaFunctionHandler() throws IOException {
        PLCalcSpeechletHandler handler = new PLCalcSpeechletHandler();

        InputStream input = new ByteArrayInputStream(SAMPLE_INPUT_STRING2.getBytes());;
        OutputStream output = new ByteArrayOutputStream();

        handler.handleRequest(input, output, null);

        // TODO: validate output here if needed.
        String sampleOutputString = output.toString();
        System.out.println(sampleOutputString);
        Assert.assertEquals(true, true);
    }
}
