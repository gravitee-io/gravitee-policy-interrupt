/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.interrupt;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.apim.gateway.tests.sdk.AbstractPolicyTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.policy.interrupt.configuration.InterruptPolicyConfiguration;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Consumer;
import io.vertx.core.http.HttpMethod;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.util.function.BiConsumer;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
class InterruptPolicyIntegrationTest
  extends AbstractPolicyTest<InterruptPolicy, InterruptPolicyConfiguration> {

  @SneakyThrows
  @Test
  @DisplayName("Should do interrupt on-request")
  @DeployApi("/apis/interrupt.json")
  void shouldDoInterruptOnRequest(HttpClient client, VertxTestContext ctx) {
    final var obs = client
      .rxRequest(HttpMethod.GET, "/test")
      .flatMap(HttpClientRequest::rxSend)
      .flatMapPublisher(InterruptPolicyIntegrationTest::buildResult)
      .subscribe(
        asserts(
          ctx,
          (softly, result) -> {
            softly.assertThat(result.statusCode()).isEqualTo(500);
            softly
              .assertThat(result.body())
              .hasToString("Message in a bottle...");
          },
          () -> wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")))
        ),
        ctx::failNow
      );
  }

  @SneakyThrows
  @Test
  @DisplayName("Should do interrupt on-request with custom status code")
  @DeployApi("/apis/interrupt-customstatuscode.json")
  void shouldDoInterruptOnRequestWithCustomStatusCode(
    HttpClient client,
    VertxTestContext ctx
  ) {
    client
      .rxRequest(HttpMethod.GET, "/test")
      .flatMap(HttpClientRequest::rxSend)
      .flatMapPublisher(InterruptPolicyIntegrationTest::buildResult)
      .subscribe(
        asserts(
          ctx,
          (softly, result) -> {
            softly.assertThat(result.statusCode()).isEqualTo(503);
            softly
              .assertThat(result.body())
              .hasToString("Service unavailable...");
          },
          () -> wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")))
        ),
        ctx::failNow
      );
  }

  @SneakyThrows
  @Test
  @DisplayName("Should do interrupt on-request with response template")
  @DeployApi("/apis/interrupt-responsetemplate.json")
  void shouldDoInterruptOnRequestWithResponseTemplate(
    HttpClient client,
    VertxTestContext ctx
  ) {
    client
      .rxRequest(HttpMethod.GET, "/test")
      .flatMap(request -> {
        request.putHeader("my-custom-header", "the anonymous consumer");
        return request.rxSend();
      })
      .flatMapPublisher(InterruptPolicyIntegrationTest::buildResult)
      .subscribe(
        asserts(
          ctx,
          (softly, result) -> {
            softly.assertThat(result.statusCode()).isEqualTo(400);
            softly
              .assertThat(result.body())
              .hasToString(
                "An other message in a bottle... from the anonymous consumer"
              );
          },
          () -> wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")))
        ),
        ctx::failNow
      );
  }

  @SneakyThrows
  @Test
  @DisplayName("Should do interrupt on-request-content")
  @DeployApi("/apis/interrupt-onrequestcontent.json")
  void shouldDoInterruptOnRequestContent(
    HttpClient client,
    VertxTestContext ctx
  ) {
    wiremock.stubFor(post("/endpoint").willReturn(ok("response from backend")));

    client
      .rxRequest(HttpMethod.POST, "/test")
      .flatMap(HttpClientRequest::rxSend)
      .flatMapPublisher(InterruptPolicyIntegrationTest::buildResult)
      .subscribe(
        asserts(
          ctx,
          (softly, result) -> {
            softly.assertThat(result.statusCode()).isEqualTo(500);
            softly
              .assertThat(result.body())
              .hasToString("Message in a bottle...");
          },
          () ->
            wiremock.verify(0, postRequestedFor(urlPathEqualTo("/endpoint")))
        ),
        ctx::failNow
      );
  }

  @SneakyThrows
  @Test
  @DisplayName("Should do interrupt on-request-content with response template")
  @DeployApi("/apis/interrupt-onrequestcontent-responsetemplate.json")
  void shouldDoInterruptOnRequestContentWithResponseTemplate(
    HttpClient client,
    VertxTestContext ctx
  ) {
    wiremock.stubFor(post("/endpoint").willReturn(ok("response from backend")));

    client
      .rxRequest(HttpMethod.POST, "/test")
      .flatMap(request -> {
        request.putHeader("my-custom-header", "the anonymous consumer");
        return request.rxSend(Buffer.buffer("request payload"));
      })
      .flatMapPublisher(InterruptPolicyIntegrationTest::buildResult)
      .subscribe(
        asserts(
          ctx,
          (softly, result) -> {
            softly.assertThat(result.statusCode()).isEqualTo(400);
            softly
              .assertThat(result.body())
              .hasToString(
                "An other message in a bottle... from the anonymous consumer"
              );
          },
          () ->
            wiremock.verify(0, postRequestedFor(urlPathEqualTo("/endpoint")))
        ),
        ctx::failNow
      );
  }

  @SneakyThrows
  @Test
  @DisplayName("Should do interrupt on-response")
  @DeployApi("/apis/interrupt-onresponse.json")
  void shouldDoInterruptOnResponse(HttpClient client, VertxTestContext ctx) {
    client
      .rxRequest(HttpMethod.GET, "/test")
      .flatMap(HttpClientRequest::rxSend)
      .flatMapPublisher(InterruptPolicyIntegrationTest::buildResult)
      .subscribe(
        asserts(
          ctx,
          (softly, result) -> {
            softly.assertThat(result.statusCode()).isEqualTo(500);
            softly
              .assertThat(result.body())
              .hasToString("Message in a bottle...");
          },
          () -> wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")))
        ),
        ctx::failNow
      );
  }

  @SneakyThrows
  @Test
  @DisplayName(
    "Should interrupt on response when deprecated scope is request-side (REQUEST)"
  )
  @DeployApi("/apis/interrupt-onresponse-with-request-scope.json")
  void shouldDoInterruptOnResponseWhenScopeIsRequest(
    HttpClient client,
    VertxTestContext ctx
  ) {
    client
      .rxRequest(HttpMethod.GET, "/test")
      .flatMap(HttpClientRequest::rxSend)
      .flatMapPublisher(InterruptPolicyIntegrationTest::buildResult)
      .subscribe(
        asserts(
          ctx,
          (softly, result) -> {
            softly.assertThat(result.statusCode()).isEqualTo(500);
            softly
              .assertThat(result.body())
              .hasToString("Interrupted on response despite REQUEST scope");
          },
          () -> wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")))
        ),
        ctx::failNow
      );
  }

  @SneakyThrows
  @Test
  @DisplayName(
    "Should interrupt on response when deprecated scope is request-side (REQUEST_CONTENT)"
  )
  @DeployApi("/apis/interrupt-onresponse-with-request-content-scope.json")
  void shouldDoInterruptOnResponseWhenScopeIsRequestContent(
    HttpClient client,
    VertxTestContext ctx
  ) {
    client
      .rxRequest(HttpMethod.GET, "/test")
      .flatMap(HttpClientRequest::rxSend)
      .flatMapPublisher(InterruptPolicyIntegrationTest::buildResult)
      .subscribe(
        asserts(
          ctx,
          (softly, result) -> {
            softly.assertThat(result.statusCode()).isEqualTo(500);
            softly
              .assertThat(result.body())
              .hasToString(
                "Interrupted on response despite REQUEST_CONTENT scope"
              );
          },
          () -> wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")))
        ),
        ctx::failNow
      );
  }

  @SneakyThrows
  @Test
  @DisplayName("Should do interrupt on-response with response template")
  @DeployApi("/apis/interrupt-onresponse-responsetemplate.json")
  void shouldDoInterruptOnResponseWithResponseTemplate(
    HttpClient client,
    VertxTestContext ctx
  ) {
    client
      .rxRequest(HttpMethod.GET, "/test")
      .flatMap(request -> {
        request.putHeader("my-custom-header", "the anonymous consumer");
        return request.rxSend();
      })
      .flatMapPublisher(InterruptPolicyIntegrationTest::buildResult)
      .subscribe(
        asserts(
          ctx,
          (softly, result) -> {
            softly.assertThat(result.statusCode()).isEqualTo(400);
            softly
              .assertThat(result.body())
              .hasToString(
                "An other message in a bottle... from the anonymous consumer"
              );
          },
          () -> wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")))
        ),
        ctx::failNow
      );
  }

  @SneakyThrows
  @Test
  @DisplayName("Should do interrupt on-response-content")
  @DeployApi("/apis/interrupt-onresponsecontent.json")
  void shouldDoInterruptOnResponseContent(
    HttpClient client,
    VertxTestContext ctx
  ) {
    wiremock.stubFor(post("/endpoint").willReturn(ok("response from backend")));

    client
      .rxRequest(HttpMethod.POST, "/test")
      .flatMap(HttpClientRequest::rxSend)
      .flatMapPublisher(InterruptPolicyIntegrationTest::buildResult)
      .subscribe(
        asserts(
          ctx,
          (softly, result) -> {
            softly.assertThat(result.statusCode()).isEqualTo(500);
            softly
              .assertThat(result.body())
              .hasToString("Message in a bottle...");
          },
          () ->
            wiremock.verify(1, postRequestedFor(urlPathEqualTo("/endpoint")))
        ),
        ctx::failNow
      );
  }

  @SneakyThrows
  @Test
  @DisplayName("Should do interrupt on-response-content with response template")
  @DeployApi("/apis/interrupt-onresponsecontent-responsetemplate.json")
  void shouldDoInterruptOnResponseContentWithResponseTemplate(
    HttpClient client,
    VertxTestContext ctx
  ) {
    wiremock.stubFor(post("/endpoint").willReturn(ok("response from backend")));

    client
      .rxRequest(HttpMethod.POST, "/test")
      .flatMap(request ->
        request
          .putHeader("my-custom-header", "the anonymous consumer")
          .rxSend(Buffer.buffer("request payload"))
      )
      .flatMapPublisher(InterruptPolicyIntegrationTest::buildResult)
      .subscribe(
        asserts(
          ctx,
          (softly, result) -> {
            softly.assertThat(result.statusCode()).isEqualTo(400);
            softly
              .assertThat(result.body())
              .hasToString(
                "An other message in a bottle... from the anonymous consumer"
              );
          },
          () ->
            wiremock.verify(1, postRequestedFor(urlPathEqualTo("/endpoint")))
        ),
        ctx::failNow
      );
  }

  private static @NonNull Flowable<Result> buildResult(
    HttpClientResponse response
  ) {
    return response
      .toFlowable()
      .map(buffer -> new Result(response.statusCode(), buffer));
  }

  private static Consumer<Result> asserts(
    VertxTestContext ctx,
    BiConsumer<SoftAssertions, Result> ass,
    Runnable verify
  ) {
    return result ->
      ctx
        .verify(() -> {
          assertSoftly(softAssertions -> ass.accept(softAssertions, result));
          verify.run();
        })
        .completeNow();
  }

  record Result(int statusCode, Buffer body) {}
}
