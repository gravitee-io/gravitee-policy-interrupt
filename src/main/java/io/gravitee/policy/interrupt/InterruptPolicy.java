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

import io.gravitee.common.util.Maps;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.stream.TransformableRequestStreamBuilder;
import io.gravitee.gateway.api.stream.BufferedReadWriteStream;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.SimpleReadWriteStream;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.interrupt.configuration.InterruptPolicyConfiguration;
import io.gravitee.policy.interrupt.configuration.PolicyScope;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InterruptPolicy {

  private final InterruptPolicyConfiguration configuration;

  public InterruptPolicy(InterruptPolicyConfiguration configuration) {
    this.configuration = configuration;
  }

  @OnRequest
  public void onRequest(ExecutionContext context, PolicyChain chain) {
    if (
      configuration.getScope() == null ||
      configuration.getScope() == PolicyScope.REQUEST
    ) {
      chain.failWith(buildPolicyResult(context));
    } else {
      chain.doNext(context.request(), context.response());
    }
  }

  @OnRequestContent
  public ReadWriteStream<?> onRequestContent(
    ExecutionContext context,
    Request request,
    PolicyChain chain
  ) {
    if (configuration.getScope() == PolicyScope.REQUEST_CONTENT) {
      return new BufferedReadWriteStream() {
        @Override
        public SimpleReadWriteStream<Buffer> write(Buffer content) {
          super.write(content);

          return this;
        }

        @Override
        public void end() {
          chain.streamFailWith(buildPolicyResult(context));

          super.end();
        }
      };
    }

    return null;
  }

  private PolicyResult buildPolicyResult(ExecutionContext context) {
    Map<String, Object> variables;

    // Look at variables and set the value depending on the EL
    if (
      configuration.getVariables() == null ||
      configuration.getVariables().isEmpty()
    ) {
      variables = Collections.emptyMap();
    } else {
      Maps.MapBuilder<String, Object> builder = Maps.builder();
      configuration
        .getVariables()
        .forEach(variable -> {
          try {
            context.getTemplateEngine();
            String value = context
              .getTemplateEngine()
              .getValue(variable.getValue(), String.class);
            builder.put(variable.getName(), value);
          } catch (Exception ex) {
            // Do nothing
          }
        });

      variables = builder.build();
    }

    return PolicyResult.failure(
      configuration.getErrorKey(),
      configuration.getMessage(),
      variables
    );
  }
}
