package org.gooru.nucleus.handlers.auth.processors;

import org.gooru.nucleus.handlers.auth.responses.MessageResponse;

import io.vertx.core.Future;

/**
 * Created by ashish on 4/1/16.
 */
public interface MessageProcessor {
    void process(Future<MessageResponse> future);
}
