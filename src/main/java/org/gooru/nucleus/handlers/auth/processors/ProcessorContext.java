package org.gooru.nucleus.handlers.auth.processors;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;

/**
 * Created by ashish on 4/1/16.
 */
public class ProcessorContext {
  private final Vertx vertx;
  private final RedisClient redisClient;
  private final Message message;
  private final JsonObject config;

  private ProcessorContext(final Vertx vertx, RedisClient redisClient, final Message message, final JsonObject config) {
    this.vertx = vertx;
    this.redisClient = redisClient;
    this.message = message;
    this.config = config;
  }

  public static ProcessorContext build(final Vertx vertx, RedisClient redisClient, final Message message, final JsonObject config) {

    if (vertx == null || redisClient == null || message == null || config == null) {
      throw new IllegalArgumentException("ProcessorContext can't be created with invalid or null values");
    }
    return new ProcessorContext(vertx, redisClient, message, config);
  }

  public Vertx vertx() {
    return this.vertx;
  }

  public RedisClient redisClient() {
    return this.redisClient;
  }

  public Message message() {
    return this.message;
  }

  public JsonObject config() {
    return this.config;
  }
}
