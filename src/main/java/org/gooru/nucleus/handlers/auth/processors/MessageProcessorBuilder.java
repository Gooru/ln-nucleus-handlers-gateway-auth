package org.gooru.nucleus.handlers.auth.processors;

/**
 * Created by ashish on 4/1/16.
 */
public class MessageProcessorBuilder {

  public MessageProcessor buildDefaultProcessor(ProcessorContext pc) {
    return new AuthMessageProcessor(pc);
  }
}
