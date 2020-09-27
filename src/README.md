# Spring Cloud Function を使って、API Gateway の HTTPプロキシ統合を使った処理を作る

# AWS Lambda で動く処理を書く

# 上記 Lambda を呼び出す API Gateway を作成
* このとき、HTTPプロキシ統合として作成する
* Lambda 関数から作るとすぐに作成出来る

# ハンドラの書き換え
* API Gateway からの HTTPプロキシ統合されたリクエストを受け取るため、ハンドラを書き換える
* RequestHandlerを以下の様にする
```
package com.myexample.serverless.apigw;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.springframework.cloud.function.adapter.aws.SpringBootRequestHandler;

public class RequestHandler extends SpringBootRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
}
```

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
* Handler を以下の様に書き換える
```
package com.myexample.serverless.apigw;

import org.springframework.cloud.function.adapter.aws.SpringBootApiGatewayRequestHandler;

public class RequestHandler extends SpringBootApiGatewayRequestHandler {
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