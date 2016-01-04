package org.gooru.nucleus.handlers.auth.processors;

import io.vertx.core.Future;
import org.gooru.nucleus.handlers.auth.responses.MessageResponse;

/**
 * Created by ashish on 4/1/16.
 */
public interface MessageProcessor {
  void process(Future<Object> future);
}
