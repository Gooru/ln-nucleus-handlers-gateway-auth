package org.gooru.nucleus.handlers.auth.processors;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;
import org.gooru.nucleus.handlers.auth.constants.MessageConstants;
import org.gooru.nucleus.handlers.auth.responses.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ashish on 4/1/16.
 */
class AuthMessageProcessor implements MessageProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageProcessor.class);
  private final ProcessorContext processorContext;

  AuthMessageProcessor(ProcessorContext pc) {
    this.processorContext = pc;
  }

  @Override
  public void process(Future<Object> future) {
    Message message = processorContext.message();
    Vertx vertx = processorContext.vertx();
    RedisClient redisClient = processorContext.redisClient();
    JsonObject config = processorContext.config();

    String msgOp = message.headers().get(MessageConstants.MSG_HEADER_OP);
    String sessionToken = message.headers().get(MessageConstants.MSG_HEADER_TOKEN);
    final DeliveryOptions deliveryOptions = new DeliveryOptions();
    int sessionTimeout = config.getInteger(MessageConstants.CONFIG_SESSION_TIMEOUT_KEY);

    LOGGER.debug("Starting processing of auth request in processor for token: '{}", sessionToken);
    if (sessionToken != null && !sessionToken.isEmpty()) {
      if (msgOp.equalsIgnoreCase(MessageConstants.MSG_OP_AUTH_WITH_PREFS)) {
        redisClient.get(sessionToken, redisAsyncResult -> {
          JsonObject jsonResult = null;
          if (redisAsyncResult.succeeded()) {
            LOGGER.debug("Communication with redis done without exception");
            final String redisResult = redisAsyncResult.result();
            LOGGER.debug("Redis responded with '{}'", redisResult);
            if (redisResult != null) {
              processSuccess(deliveryOptions, future, redisResult);
              // Happening asynchronously, we do not delay sending response
              renewSessionTokenExpiry(vertx, redisClient, sessionToken, sessionTimeout);
            } else {
              LOGGER.info("Session not found. Invalid session");
              processFailure(deliveryOptions, future);
            }
          } else {
            LOGGER.error("Redis operation failed", redisAsyncResult.cause());
            processFailure(deliveryOptions, future);
          }
        });
      } else {
        LOGGER.error("Invalid command. System does not understand it");
        processFailure(deliveryOptions, future);
      }
    } else {
      LOGGER.error("Unable to authorize. Invalid authorization header");
      processFailure(deliveryOptions, future);
    }
  }

  private void processSuccess(DeliveryOptions deliveryOptions, Future<Object> future, String redisResult) {
    JsonObject jsonResult;
    jsonResult = new JsonObject(redisResult);
    // If need arises, this is where we shall be doing response transformation
    deliveryOptions.addHeader(MessageConstants.MSG_OP_STATUS, MessageConstants.MSG_OP_STATUS_SUCCESS);
    MessageResponse response = MessageResponse.build(deliveryOptions, jsonResult);
    future.complete(response);
  }

  private void renewSessionTokenExpiry(Vertx vertx, RedisClient redisClient, String sessionToken, int sessionTimeout) {
    redisClient.expire(sessionToken, sessionTimeout, updateHandler -> {
      if (updateHandler.succeeded()) {
        LOGGER.debug("expiry time of session {} is updated", sessionToken);
      } else {
        LOGGER.warn("Not able to update expiry for key {}", sessionToken, updateHandler.cause());
      }
    });
  }

  private void processFailure(DeliveryOptions deliveryOptions, Future<Object> future) {
    deliveryOptions.addHeader(MessageConstants.MSG_OP_STATUS, MessageConstants.MSG_OP_STATUS_ERROR);
    MessageResponse response = MessageResponse.build(deliveryOptions, new JsonObject());
    future.complete(response);
  }
}
