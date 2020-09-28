package com.myexample.serverless.apigw;

import com.myexample.serverless.apigw.functions.Greet;
import com.myexample.serverless.apigw.functions.models.Greeting;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.FunctionType;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.messaging.Message;

@SpringBootApplication
public class SpringCloudFunctionAwsApigwSampleApplication implements ApplicationContextInitializer<GenericApplicationContext> {

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudFunctionAwsApigwSampleApplication.class, args);
    }

    @Override
    public void initialize(GenericApplicationContext context) {
        context.registerBean(
                FunctionRegistration.class,
                () -> new FunctionRegistration<>(new Greet()).type(FunctionType.of(Greet.class)));
    }
}
