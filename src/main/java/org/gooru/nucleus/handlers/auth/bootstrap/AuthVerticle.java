package org.gooru.nucleus.handlers.auth.bootstrap;

import org.gooru.nucleus.handlers.auth.constants.MessageConstants;
import org.gooru.nucleus.handlers.auth.constants.MessagebusEndpoints;
import org.gooru.nucleus.handlers.auth.processors.MessageProcessorBuilder;
import org.gooru.nucleus.handlers.auth.processors.ProcessorContext;
import org.gooru.nucleus.handlers.auth.responses.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

public class AuthVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthVerticle.class);
    private RedisClient redisClient;
    private RedisClient expiryUpdaterRedisClient;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        EventBus eb = vertx.eventBus();

        Future<Void> verticleInitializationFuture = Future.future();
        initializeVerticle(verticleInitializationFuture);
        Future<Void> eventBusInitializationFuture = Future.future();
        initializeEventBus(eventBusInitializationFuture, eb);

        CompositeFuture completionFuture =
            CompositeFuture.all(verticleInitializationFuture, eventBusInitializationFuture);

        completionFuture.setHandler(ar -> {
            if (ar.succeeded()) {
                startFuture.complete();
            } else {
                startFuture.fail("Initialization failed");
            }
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        finalizeVerticle(stopFuture);
    }

    private void initializeEventBus(Future<Void> eventBusInitializationFuture, EventBus eb) {
        eb.<JsonObject>consumer(MessagebusEndpoints.MBEP_AUTH, message -> {
            ProcessorContext pc =
                ProcessorContext.build(vertx, redisClient, expiryUpdaterRedisClient, message, config());
            Future<MessageResponse> blockingFuture = Future.future();
            MessageProcessorBuilder.buildDefaultProcessor(pc).process(blockingFuture);

            blockingFuture.setHandler(asyncResult -> {
                if (asyncResult.succeeded()) {
                    MessageResponse response = asyncResult.result();
                    message.reply(response.getMessage(), response.getDeliveryOptions());
                } else {
                    LOGGER.error("Auth processing failed");
                }
            });
        }).completionHandler(result -> {
            if (result.succeeded()) {
                LOGGER.info("Auth end point ready to listen");
                eventBusInitializationFuture.complete();
            } else {
                LOGGER.error("Error registering the auth handler. Halting the auth machinery");
                eventBusInitializationFuture.fail("Error registering the auth handler. Halting the auth machinery");
                Runtime.getRuntime().halt(1);
            }
        });
    }

    private void initializeVerticle(Future<Void> verticleInitializationFuture) {
        try {
            redisClient = createRedisClient();
            expiryUpdaterRedisClient = createRedisClient();

            Future<Void> readConnectionFuture = verifyRedisConnection(redisClient);
            Future<Void> writeConnectionFuture = verifyRedisConnection(expiryUpdaterRedisClient);

            CompositeFuture compositeFuture = CompositeFuture.all(readConnectionFuture, writeConnectionFuture);

            compositeFuture.setHandler(ar -> {
                if (compositeFuture.succeeded()) {
                    verticleInitializationFuture.complete();
                } else {
                    verticleInitializationFuture.fail("Not able to verify redis connections");
                }
            });

        } catch (Throwable throwable) {
            LOGGER.error("Not able to continue initialization.", throwable);
            verticleInitializationFuture.fail(throwable);
        }
    }

    private RedisClient createRedisClient() {
        JsonObject configuration = config().getJsonObject(MessageConstants.CONFIG_REDIS_CONFIGURATION_KEY);
        RedisOptions options = new RedisOptions(configuration);
        return RedisClient.create(vertx, options);
    }

    private Future<Void> verifyRedisConnection(RedisClient client) {
        Future<Void> resultFuture = Future.future();
        redisClient.get("NonExistingKey", initHandler -> {
            if (initHandler.succeeded()) {
                LOGGER.info("Initial connection check with Redis done");
                resultFuture.complete();
            } else {
                LOGGER.warn("Not able to verify redis connection", initHandler.cause());
                resultFuture.fail(initHandler.cause());
            }
        });
        return resultFuture;
    }

    private void finalizeVerticle(Future<Void> stopFuture) {
        closeRedisClient(redisClient);
        closeRedisClient(expiryUpdaterRedisClient);
    }

    private void closeRedisClient(RedisClient client) {
        if (client != null) {
            client.close(redisCloseAsyncHandler -> {
                if (redisCloseAsyncHandler.succeeded()) {
                    LOGGER.info("Redis client has been closed successfully");
                } else {
                    LOGGER.error("Error in closing redis client", redisCloseAsyncHandler.cause());
                }
            });
        }
    }

}
