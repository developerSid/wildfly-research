package estate.myers.naic.messaging;

import java.util.logging.Logger;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

/**
 * Logs every message put on the naic queue. The queue itself is created on
 * deployment by the {@link JMSDestinationDefinition} below, in WildFly's
 * embedded Artemis broker.
 */
@JMSDestinationDefinition(
        name = NaicQueueListener.QUEUE_JNDI,
        interfaceName = "jakarta.jms.Queue",
        destinationName = "naic")
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = NaicQueueListener.QUEUE_JNDI),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue")})
public class NaicQueueListener implements MessageListener {

    public static final String QUEUE_JNDI = "java:/queue/naic";

    private static final Logger LOG = Logger.getLogger(NaicQueueListener.class.getName());

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage text) {
                LOG.info("Received: " + text.getText());
            } else {
                LOG.warning("Received non-text message: " + message.getJMSMessageID());
            }
        } catch (JMSException e) {
            throw new IllegalStateException("Could not read JMS message", e);
        }
    }
}
