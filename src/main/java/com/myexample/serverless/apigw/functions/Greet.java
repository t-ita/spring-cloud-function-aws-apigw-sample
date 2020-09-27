package com.myexample.serverless.apigw.functions;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myexample.serverless.apigw.functions.models.Greeting;

import java.util.function.Function;

public class Greet implements Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent apply(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent) {
        var responseEvent = new APIGatewayProxyResponseEvent();

        var mapper = new ObjectMapper();
        try {
            var body = apiGatewayProxyRequestEvent.getBody();
            var greeting = mapper.readValue(body, Greeting.class);

            var resGreeting = new Greeting();
            resGreeting.setName("Spring Cloud Function");
            resGreeting.setMessage(String.format("%s, %s", greeting.getMessage(), greeting.getName()));

            responseEvent.setBody(mapper.writeValueAsString(resGreeting));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return responseEvent;
    }
}
