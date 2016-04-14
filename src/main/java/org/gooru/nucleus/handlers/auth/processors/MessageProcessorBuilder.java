package org.gooru.nucleus.handlers.auth.processors;

/**
 * Created by ashish on 4/1/16.
 */
public final class MessageProcessorBuilder {

    private MessageProcessorBuilder() {
        throw new AssertionError();
    }

    public static MessageProcessor buildDefaultProcessor(ProcessorContext pc) {
        return new AuthMessageProcessor(pc);
    }
}
