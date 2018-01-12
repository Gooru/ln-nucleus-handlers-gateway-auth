package org.gooru.nucleus.handlers.auth.processors;

import org.gooru.nucleus.handlers.auth.constants.MessageConstants;
import org.gooru.nucleus.handlers.auth.responses.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;

/**
 * Created by ashish on 4/1/16.
 */
class AuthMessageProcessor implements MessageProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageProcessor.class);
    private final ProcessorContext processorContext;
    private Future<MessageResponse> future;
    private DeliveryOptions deliveryOptions;

    AuthMessageProcessor(ProcessorContext pc) {
        this.processorContext = pc;
    }

    @Override
    public void process(Future<MessageResponse> completionFuture) {
        Message<JsonObject> message = processorContext.message();
        Vertx vertx = processorContext.vertx();
        RedisClient redisClient = processorContext.redisClient();
        JsonObject config = processorContext.config();
        this.future = completionFuture;

        String sessionToken = message.headers().get(MessageConstants.MSG_HEADER_TOKEN);
        deliveryOptions = new DeliveryOptions();

        LOGGER.debug("Starting processing of auth request in processor for token: '{}", sessionToken);
        try {
            validateSessionToken();
            validateOperation();
            redisClient.get(sessionToken, redisAsyncResult -> {
                if (redisAsyncResult.succeeded()) {
                    LOGGER.debug("Communication with redis done without exception");
                    final String redisResult = redisAsyncResult.result();
                    LOGGER.debug("Redis responded with '{}'", redisResult);
                    if (redisResult != null) {
                        try {
                            processSuccess(redisResult);
                            // Happening asynchronously, we do not delay sending response
                            renewSessionTokenExpiry(sessionToken, getTokenExpiry(redisResult));
                        } catch (DecodeException de) {
                            LOGGER.error("exception while decoding json for token '{}'", sessionToken, de);
                            processFailure();
                        }
                    } else {
                        LOGGER.info("Session not found. Invalid session");
                        processFailure();
                    }
                } else {
                    LOGGER.error("Redis operation failed", redisAsyncResult.cause());
                    processFailure();
                }
            });
        } catch (ProcessorException e) {
            LOGGER.error("Processing exception, will fail auth");
            processFailure();
        }

    }

    private void validateOperation() {
        String msgOp = processorContext.message().headers().get(MessageConstants.MSG_HEADER_OP);
        if (!msgOp.equalsIgnoreCase(MessageConstants.MSG_OP_AUTH)) {
            LOGGER.error("Invalid command. System does not understand it");
            throw new ProcessorException();
        }
    }

    private void validateSessionToken() {
        String sessionToken = processorContext.message().headers().get(MessageConstants.MSG_HEADER_TOKEN);
        if (sessionToken == null || sessionToken.isEmpty()) {
            LOGGER.error("Unable to authorize. Invalid authorization header");
            throw new ProcessorException();
        }
    }

    private void processSuccess(String redisResult) {
        JsonObject jsonResult;
        jsonResult = new JsonObject(redisResult);
        // If need arises, this is where we shall be doing response transformation
        deliveryOptions.addHeader(MessageConstants.MSG_OP_STATUS, MessageConstants.MSG_OP_STATUS_SUCCESS);
        MessageResponse response = MessageResponse.build(deliveryOptions, jsonResult);
        this.future.complete(response);
    }

    private void renewSessionTokenExpiry(String sessionToken, int sessionTimeout) {
        this.processorContext.expiryUpdaterRedisClient().<Long>expire(sessionToken, sessionTimeout, updateHandler -> {
            if (updateHandler.succeeded()) {
                LOGGER.debug("expiry time of session {} is updated, result : {}", sessionToken, updateHandler.result());
            } else {
                LOGGER.warn("Not able to update expiry for key {}", sessionToken, updateHandler.cause());
            }
        });
    }

    private void processFailure() {
        deliveryOptions.addHeader(MessageConstants.MSG_OP_STATUS, MessageConstants.MSG_OP_STATUS_ERROR);
        MessageResponse response = MessageResponse.build(deliveryOptions, new JsonObject());
        this.future.complete(response);
    }

    private int getTokenExpiry(String redisPacket) {
        JsonObject packet = new JsonObject(redisPacket);
        return packet.getInteger(MessageConstants.ACCESS_TOKEN_VALIDITY,
            processorContext.config().getInteger(MessageConstants.CONFIG_SESSION_TIMEOUT_KEY));
    }
    
    private static class ProcessorException extends RuntimeException {
    }

}
