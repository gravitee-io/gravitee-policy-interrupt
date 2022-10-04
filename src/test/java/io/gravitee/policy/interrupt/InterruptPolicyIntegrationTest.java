/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import io.reactivex.observers.TestObserver;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
public class InterruptPolicyIntegrationTest
  extends AbstractPolicyTest<InterruptPolicy, InterruptPolicyConfiguration> {

  @Test
  @DisplayName("Should do interrupt on-request")
  @DeployApi("/apis/interrupt.json")
  void shouldDoInterruptOnRequest(WebClient client) {
    final TestObserver<HttpResponse<Buffer>> obs = client
      .get("/test")
      .rxSend()
      .test();

    awaitTerminalEvent(obs);
    obs
      .assertComplete()
      .assertValue(response -> {
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.bodyAsString()).isEqualTo("Message in a bottle...");
        return true;
      })
      .assertNoErrors();

    wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
  }

  @Test
  @DisplayName("Should do interrupt on-request with response template")
  @DeployApi("/apis/interrupt-responsetemplate.json")
  void shouldDoInterruptOnRequestWithResponseTemplate(WebClient client) {
    final TestObserver<HttpResponse<Buffer>> obs = client
      .get("/test")
      .putHeader("my-custom-header", "the anonymous consumer")
      .rxSend()
      .test();

    awaitTerminalEvent(obs);
    obs
      .assertComplete()
      .assertValue(response -> {
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.bodyAsString())
          .isEqualTo(
            "An other message in a bottle... from the anonymous consumer"
          );
        return true;
      })
      .assertNoErrors();

    wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
  }

  @Test
  @DisplayName("Should do interrupt on-request-content")
  @DeployApi("/apis/interrupt-onrequestcontent.json")
  void shouldDoInterruptOnRequestContent(WebClient client) {
    wiremock.stubFor(post("/endpoint").willReturn(ok("response from backend")));

    final TestObserver<HttpResponse<Buffer>> test = client
      .post("/test")
      .rxSend()
      .test();

    awaitTerminalEvent(test);
    test
      .assertComplete()
      .assertValue(response -> {
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.bodyAsString()).isEqualTo("Message in a bottle...");
        return true;
      })
      .assertNoErrors();

    wiremock.verify(0, postRequestedFor(urlPathEqualTo("/endpoint")));
  }

  @Test
  @DisplayName("Should do interrupt on-request-content with response template")
  @DeployApi("/apis/interrupt-onrequestcontent-responsetemplate.json")
  void shouldDoInterruptOnRequestContentWithResponseTemplate(WebClient client) {
    wiremock.stubFor(post("/endpoint").willReturn(ok("response from backend")));

    final TestObserver<HttpResponse<Buffer>> obs = client
      .post("/test")
      .putHeader("my-custom-header", "the anonymous consumer")
      .rxSendBuffer(Buffer.buffer("request payload"))
      .test();

    awaitTerminalEvent(obs);
    obs
      .assertComplete()
      .assertValue(response -> {
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.bodyAsString())
          .isEqualTo(
            "An other message in a bottle... from the anonymous consumer"
          );
        return true;
      })
      .assertNoErrors();

    wiremock.verify(0, postRequestedFor(urlPathEqualTo("/endpoint")));
  }

  @Test
  @DisplayName("Should do interrupt on-response")
  @DeployApi("/apis/interrupt-onresponse.json")
  void shouldDoInterruptOnResponse(WebClient client) {
    final TestObserver<HttpResponse<Buffer>> obs = client
      .get("/test")
      .rxSend()
      .test();

    awaitTerminalEvent(obs);
    obs
      .assertComplete()
      .assertValue(response -> {
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.bodyAsString()).isEqualTo("Message in a bottle...");
        return true;
      })
      .assertNoErrors();

    wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
  }

  @Test
  @DisplayName("Should do interrupt on-response with response template")
  @DeployApi("/apis/interrupt-onresponse-responsetemplate.json")
  void shouldDoInterruptOnResponseWithResponseTemplate(WebClient client) {
    final TestObserver<HttpResponse<Buffer>> obs = client
      .get("/test")
      .putHeader("my-custom-header", "the anonymous consumer")
      .rxSend()
      .test();

    awaitTerminalEvent(obs);
    obs
      .assertComplete()
      .assertValue(response -> {
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.bodyAsString())
          .isEqualTo(
            "An other message in a bottle... from the anonymous consumer"
          );
        return true;
      })
      .assertNoErrors();

    wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
  }

  @Test
  @DisplayName("Should do interrupt on-response-content")
  @DeployApi("/apis/interrupt-onresponsecontent.json")
  void shouldDoInterruptOnResponseContent(WebClient client) {
    wiremock.stubFor(post("/endpoint").willReturn(ok("response from backend")));

    final TestObserver<HttpResponse<Buffer>> test = client
      .post("/test")
      .rxSend()
      .test();

    awaitTerminalEvent(test);
    test
      .assertComplete()
      .assertValue(response -> {
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.bodyAsString()).isEqualTo("Message in a bottle...");
        return true;
      })
      .assertNoErrors();

    wiremock.verify(1, postRequestedFor(urlPathEqualTo("/endpoint")));
  }

  @Test
  @DisplayName("Should do interrupt on-response-content with response template")
  @DeployApi("/apis/interrupt-onresponsecontent-responsetemplate.json")
  void shouldDoInterruptOnResponseContentWithResponseTemplate(
    WebClient client
  ) {
    wiremock.stubFor(post("/endpoint").willReturn(ok("response from backend")));

    final TestObserver<HttpResponse<Buffer>> obs = client
      .post("/test")
      .putHeader("my-custom-header", "the anonymous consumer")
      .rxSendBuffer(Buffer.buffer("request payload"))
      .test();

    awaitTerminalEvent(obs);
    obs
      .assertComplete()
      .assertValue(response -> {
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.bodyAsString())
          .isEqualTo(
            "An other message in a bottle... from the anonymous consumer"
          );
        return true;
      })
      .assertNoErrors();

    wiremock.verify(1, postRequestedFor(urlPathEqualTo("/endpoint")));
  }
}
