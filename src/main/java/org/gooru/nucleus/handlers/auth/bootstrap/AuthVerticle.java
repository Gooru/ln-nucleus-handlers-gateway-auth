package org.gooru.nucleus.handlers.auth.bootstrap;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import org.gooru.nucleus.handlers.auth.constants.MessageConstants;
import org.gooru.nucleus.handlers.auth.constants.MessagebusEndpoints;
import org.gooru.nucleus.handlers.auth.processors.MessageProcessorBuilder;
import org.gooru.nucleus.handlers.auth.processors.ProcessorContext;
import org.gooru.nucleus.handlers.auth.responses.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthVerticle.class);
  private RedisClient redisClient;

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    EventBus eb = vertx.eventBus();

    initializeVerticle(startFuture);

    if (startFuture.failed()) {
      return;
    }
    eb.consumer(MessagebusEndpoints.MBEP_AUTH, message -> {
      ProcessorContext pc = ProcessorContext.build(vertx, redisClient, message, config());
      vertx.executeBlocking(blockingFuture -> MessageProcessorBuilder.buildDefaultProcessor(pc).process(blockingFuture), asyncResult -> {
        if (asyncResult.succeeded()) {
          MessageResponse response = (MessageResponse) asyncResult.result();
          message.reply(response.getMessage(), response.getDeliveryOptions());
        } else {
          LOGGER.error("Auth processing failed");
        }
      });
    }).completionHandler(result -> {
      if (result.succeeded()) {
        LOGGER.info("Auth end point ready to listen");
        startFuture.complete();
      } else {
        LOGGER.error("Error registering the auth handler. Halting the auth machinery");
        startFuture.fail("Error registering the auth handler. Halting the auth machinery");
        Runtime.getRuntime().halt(1);
      }
    });
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    finalizeVerticle(stopFuture);
  }

  private void initializeVerticle(Future<Void> startFuture) {
    try {
      JsonObject configuration = config().getJsonObject(MessageConstants.CONFIG_REDIS_CONFIGURATION_KEY);
      RedisOptions options = new RedisOptions(configuration);
      redisClient = RedisClient.create(vertx, options);
      redisClient.get("NonExistingKey", initHandler -> {
        if (initHandler.succeeded()) {
          LOGGER.info("Initial connection check with Redis done");
        } else {
          startFuture.fail(initHandler.cause());
        }
      });
    } catch (Throwable throwable) {
      LOGGER.error("Not able to continue initialization.", throwable);
      startFuture.fail(throwable);
    }
  }

  private void finalizeVerticle(Future<Void> stopFuture) {
    if (redisClient != null) {
      redisClient.close(redisCloseAsyncHandler -> {
        if (redisCloseAsyncHandler.succeeded()) {
          LOGGER.info("Redis client has been closed successfully");
        } else {
          LOGGER.error("Error in closing redis client", redisCloseAsyncHandler.cause());
        }
        // Does not matter if we fail or succeed, We are closing down.
        stopFuture.complete();
      });
    }
  }

}
