package com.myexample.serverless.apigw.functions;

import com.myexample.serverless.apigw.functions.models.Greeting;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.function.Function;

public class Greet implements Function<Message<Greeting>, Message<Greeting>> {

    @Override
    public Message<Greeting> apply(Message<Greeting> greetingMessage) {
        var greeting = greetingMessage.getPayload();

        var resPayload = new Greeting();
        resPayload.setName("Spring Cloud Function - with springframework.messaging");
        resPayload.setMessage(String.format("%s, %s", greeting.getMessage(), greeting.getName()));

        var resHeader = new MessageHeaders(new HashMap<>());

        return new Message<Greeting>() {
            @Override
            public Greeting getPayload() {
                return resPayload;
            }
            @Override
            public MessageHeaders getHeaders() {
                return resHeader;
            }
        };
    }
}
