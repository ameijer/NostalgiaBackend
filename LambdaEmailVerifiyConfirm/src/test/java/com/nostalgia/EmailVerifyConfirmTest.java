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
public class EmailVerifyConfirmTest {

    private static ProcessContentRequestPojo input;

    @BeforeClass
    public static void createInput() throws IOException {
        input = new ProcessContentRequestPojo();
        input.id = "3caa568c-be73-4f17-a589-6c6365d127bb";
    }

    private Context createContext() {
        TestContext ctx = new TestContext();
        ctx.setFunctionName("ContentProcessFunction");

        return ctx;
    }

    @Test
    public void testContentProcessFunction() throws Exception {
        EmailConfirmFunction handler = new EmailConfirmFunction();
        Context ctx = createContext();
    }
}
