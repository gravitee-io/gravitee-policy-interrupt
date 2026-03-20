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
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractPolicyTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.policy.interrupt.configuration.InterruptPolicyConfiguration;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
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
  void shouldDoInterruptOnRequest(HttpClient client) {
    final var obs = client
      .rxRequest(HttpMethod.GET, "/test")
      .flatMap(request -> request.rxSend())
      .flatMapPublisher(response -> {
        assertThat(response.statusCode()).isEqualTo(500);
        return response.toFlowable();
      })
      .test();

    obs.await(30000, TimeUnit.MILLISECONDS);
    obs
      .assertComplete()
      .assertValue(buffer -> {
        assertThat(buffer.toString()).isEqualTo("Message in a bottle...");
        return true;
      })
      .assertNoErrors();

    wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
  }

  @SneakyThrows
  @Test
  @DisplayName("Should do interrupt on-request with custom status code")
  @DeployApi("/apis/interrupt-customstatuscode.json")
  void shouldDoInterruptOnRequestWithCustomStatusCode(HttpClient client) {
    final var obs = client
      .rxRequest(HttpMethod.GET, "/test")
      .flatMap(request -> request.rxSend())
      .flatMap(response -> {
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.statusMessage()).isEqualTo("Service Unavailable");
        return response.rxBody();
      })
      .toFlowable()
      .test();

    obs.await(30000, TimeUnit.MILLISECONDS);
    obs
      .assertComplete()
      .assertValue(buffer -> {
        assertThat(buffer.toString()).isEqualTo("Service unavailable...");
        return true;
      })
      .assertNoErrors();

    wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
  }

  @SneakyThrows
  @Test
  @DisplayName("Should do interrupt on-request with response template")
  @DeployApi("/apis/interrupt-responsetemplate.json")
  void shouldDoInterruptOnRequestWithResponseTemplate(HttpClient client) {
    final var obs = client
      .rxRequest(HttpMethod.GET, "/test")
      .flatMap(request -> {
        request.putHeader("my-custom-header", "the anonymous consumer");
        return request.rxSend();
      })
      .flatMap(response -> {
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.statusMessage()).isEqualTo("Bad Request");
        return response.rxBody();
      })
      .toFlowable()
      .test();

    obs.await(30000, TimeUnit.MILLISECONDS);
    obs
      .assertComplete()
      .assertValue(buffer -> {
        assertThat(buffer.toString()).isEqualTo(
          "An other message in a bottle... from the anonymous consumer"
        );
        return true;
      })
      .assertNoErrors();

    wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
  }

  @SneakyThrows
  @Test
  @DisplayName("Should do interrupt on-request-content")
  @DeployApi("/apis/interrupt-onrequestcontent.json")
  void shouldDoInterruptOnRequestContent(HttpClient client) {
    wiremock.stubFor(post("/endpoint").willReturn(ok("response from backend")));

    final var obs = client
      .rxRequest(HttpMethod.POST, "/test")
      .flatMap(request -> request.rxSend())
      .flatMap(response -> {
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.statusMessage()).isEqualTo("Internal Server Error");
        return response.rxBody();
      })
      .toFlowable()
      .test();

    obs.await(30000, TimeUnit.MILLISECONDS);
    obs
      .assertComplete()
      .assertValue(buffer -> {
        assertThat(buffer.toString()).isEqualTo("Message in a bottle...");
        return true;
      })
      .assertNoErrors();

    wiremock.verify(0, postRequestedFor(urlPathEqualTo("/endpoint")));
  }

  @SneakyThrows
  @Test
  @DisplayName("Should do interrupt on-request-content with response template")
  @DeployApi("/apis/interrupt-onrequestcontent-responsetemplate.json")
  void shouldDoInterruptOnRequestContentWithResponseTemplate(
    HttpClient client
  ) {
    wiremock.stubFor(post("/endpoint").willReturn(ok("response from backend")));

    final var obs = client
      .rxRequest(HttpMethod.POST, "/test")
      .flatMap(request -> {
        request.putHeader("my-custom-header", "the anonymous consumer");
        return request.rxSend(
          io.vertx.rxjava3.core.buffer.Buffer.buffer("request payload")
        );
      })
      .flatMapPublisher(response -> {
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.statusMessage()).isEqualTo("Bad Request");
        return response.toFlowable();
      })
      .test();

    obs.await(30000, TimeUnit.MILLISECONDS);
    obs
      .assertComplete()
      .assertValue(buffer -> {
        assertThat(buffer.toString()).isEqualTo(
          "An other message in a bottle... from the anonymous consumer"
        );
        return true;
      })
      .assertNoErrors();

    wiremock.verify(0, postRequestedFor(urlPathEqualTo("/endpoint")));
  }

  @SneakyThrows
  @Test
  @DisplayName("Should do interrupt on-response")
  @DeployApi("/apis/interrupt-onresponse.json")
  void shouldDoInterruptOnResponse(HttpClient client) {
    final var obs = client
      .rxRequest(HttpMethod.GET, "/test")
      .flatMap(request -> request.rxSend())
      .flatMap(response -> {
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.statusMessage()).isEqualTo("Internal Server Error");
        return response.rxBody();
      })
      .toFlowable()
      .test();

    obs.await(30000, TimeUnit.MILLISECONDS);
    obs
      .assertComplete()
      .assertValue(buffer -> {
        assertThat(buffer.toString()).isEqualTo("Message in a bottle...");
        return true;
      })
      .assertNoErrors();

    wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
  }

  @SneakyThrows
  @Test
  @DisplayName("Should do interrupt on-response with response template")
  @DeployApi("/apis/interrupt-onresponse-responsetemplate.json")
  void shouldDoInterruptOnResponseWithResponseTemplate(HttpClient client) {
    final var obs = client
      .rxRequest(HttpMethod.GET, "/test")
      .flatMap(request -> {
        request.putHeader("my-custom-header", "the anonymous consumer");
        return request.rxSend();
      })
      .flatMap(response -> {
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.statusMessage()).isEqualTo("Bad Request");
        return response.rxBody();
      })
      .toFlowable()
      .test();

    obs.await(30000, TimeUnit.MILLISECONDS);
    obs
      .assertComplete()
      .assertValue(buffer -> {
        assertThat(buffer.toString()).isEqualTo(
          "An other message in a bottle... from the anonymous consumer"
        );
        return true;
      })
      .assertNoErrors();

    wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
  }

  @SneakyThrows
  @Test
  @DisplayName("Should do interrupt on-response-content")
  @DeployApi("/apis/interrupt-onresponsecontent.json")
  void shouldDoInterruptOnResponseContent(HttpClient client) {
    wiremock.stubFor(post("/endpoint").willReturn(ok("response from backend")));

    final var obs = client
      .rxRequest(HttpMethod.POST, "/test")
      .flatMap(request -> request.rxSend())
      .flatMap(response -> {
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.statusMessage()).isEqualTo("Internal Server Error");
        return response.rxBody();
      })
      .toFlowable()
      .test();

    obs.await(30000, TimeUnit.MILLISECONDS);
    obs
      .assertComplete()
      .assertValue(buffer -> {
        assertThat(buffer.toString()).isEqualTo("Message in a bottle...");
        return true;
      })
      .assertNoErrors();

    wiremock.verify(1, postRequestedFor(urlPathEqualTo("/endpoint")));
  }

  @SneakyThrows
  @Test
  @DisplayName("Should do interrupt on-response-content with response template")
  @DeployApi("/apis/interrupt-onresponsecontent-responsetemplate.json")
  void shouldDoInterruptOnResponseContentWithResponseTemplate(
    HttpClient client
  ) {
    wiremock.stubFor(post("/endpoint").willReturn(ok("response from backend")));

    final var obs = client
      .rxRequest(HttpMethod.POST, "/test")
      .flatMap(request -> {
        request.putHeader("my-custom-header", "the anonymous consumer");
        return request.rxSend(
          io.vertx.rxjava3.core.buffer.Buffer.buffer("request payload")
        );
      })
      .flatMapPublisher(response -> {
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.statusMessage()).isEqualTo("Bad Request");
        return response.toFlowable();
      })
      .test();

    obs.await(30000, TimeUnit.MILLISECONDS);
    obs
      .assertComplete()
      .assertValue(buffer -> {
        assertThat(buffer.toString()).isEqualTo(
          "An other message in a bottle... from the anonymous consumer"
        );
        return true;
      })
      .assertNoErrors();

    wiremock.verify(1, postRequestedFor(urlPathEqualTo("/endpoint")));
  }
}
