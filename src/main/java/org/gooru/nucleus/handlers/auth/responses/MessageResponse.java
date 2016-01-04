package org.gooru.nucleus.handlers.auth.responses;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;

/**
 * Created by ashish on 4/1/16.
 */
public class MessageResponse {
  private final DeliveryOptions deliveryOptions;
  private final JsonObject message;

  private MessageResponse(DeliveryOptions options, JsonObject messageBody) {
    deliveryOptions = options;
    message = messageBody;
  }

  public DeliveryOptions getDeliveryOptions() {
    return deliveryOptions;
  }

  public JsonObject getMessage() {
    return message;
  }

  public static MessageResponse build(DeliveryOptions options, JsonObject message) {
    if (options == null) {
      throw new IllegalArgumentException("MessageResponse can't be created with invalid options");
    }
    return new MessageResponse(options, message);
  }
}
