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

