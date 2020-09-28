# Spring Cloud Function を使って、API Gateway の HTTPプロキシ統合を使った処理を作る

# AWS Lambda で動く処理を書く

# 上記 Lambda を呼び出す API Gateway を作成
* このとき、HTTPプロキシ統合として作成する
* Lambda 関数から作るとすぐに作成出来る

# 関数の書き換え
* APIGatewayProxyRequestEvent および APIGatewayProxyResponseEvent を受け取るようにする
```
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
```

# AWS Lambda に Jar をアップロード
* 置き換える

# ハンドラを以下に指定
```
org.springframework.cloud.function.adapter.aws.SpringBootApiGatewayRequestHandler
```

# テスト
* Lambda側でのテスト
  * テストのリクエストを以下の様にする
  ```
  {
    "body": "{\"name\": \"t-ita\",\"message\": \"Hello\"}"
  }
  ```
  * 以下が返る
  ```
  {
    "body": "{\"name\":\"Spring Cloud Function\",\"message\":\"Hello, t-ita\"}"
  }
  ```
* API Gateway でのテスト
  * メソッドは POST
  * リクエスト本文は以下
  ```
  {
      "name": "t-ita",
      "message": "Hello"
  }
  ```
  * レスポンス本文に以下が返る
  ```
  {
    "name": "Spring Cloud Function",
    "message": "Hello, t-ita"
  }
  ```
  
# 関数で、springframework.messaging.Message を受け取るようにする
* 現状では、関数が AWS にロックインされてしまうので、汎用化したい
* 関数を、以下の様に書き換える
```
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
```

# 同様にテストする
* Lambda側でのテスト
  * テストのリクエストを以下の様にする
  ```
  {
    "body": "{\"name\": \"t-ita\",\"message\": \"Hello\"}"
  }
  ```
  * 以下が返る
  ```
    {
      "statusCode": 200,
      "headers": {
        "id": "786acb6f-d9de-22fc-3455-040eb6809534",
        "timestamp": "1601190852565"
      },
      "body": "{\"name\":\"Spring Cloud Function - with springframework.messaging\",\"message\":\"Hello, t-ita\"}"
    }  
  ```
* API Gateway でのテスト
  * メソッドは POST
  * リクエスト本文は以下
  ```
  {
      "name": "t-ita",
      "message": "Hello"
  }
  ```
  * レスポンス本文に以下が返る
  ```
    {
      "name": "Spring Cloud Function - with springframework.messaging",
      "message": "Hello, t-ita"
    }
  ```
  
# 起動の高速化を図る
* Functional Bean Definitons を利用する
* main クラスを以下の様に書き換える
```
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
```
* application.properties の 関数スキャンは不要になるので記述を削除