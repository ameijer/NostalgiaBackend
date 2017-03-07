package com.nostalgia;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;
import com.nostalgia.persistence.model.ProcessContentRequestPojo;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class IdenticonGenTest {

    private static ProcessContentRequestPojo input;

    @BeforeClass
    public static void createInput() throws IOException {
        // TODO: set up your sample input object here.
        input = new ProcessContentRequestPojo();
        input.id = "3caa568c-be73-4f17-a589-6c6365d127bb";
    }

    private Context createContext() {
        TestContext ctx = new TestContext();

        // TODO: customize your context here if needed.
        ctx.setFunctionName("ContentProcessFunction");

        return ctx;
    }

    @Test
    public void testContentProcessFunction() throws Exception {
        IdenticonGenFunction handler = new IdenticonGenFunction();
        Context ctx = createContext();

//        Object output = handler.handleRequest(input, ctx);
//
//        // TODO: validate output here if needed.
//        if (output != null) {
//            System.out.println(output.toString());
//        }
//    	HttpClient client = HttpClientBuilder.create().build();
//    	
//    	LambdaClient cli = new LambdaClient(new LambdaAPIConfig(), client);
//    	List<String> results = cli.processVideo("3caa568c-be73-4f17-a589-6c6365d127bb", true);
//    	for(String result: results){
//    		System.out.println("Returned thumbnail URL: " + result);
//    	}
    }
}
