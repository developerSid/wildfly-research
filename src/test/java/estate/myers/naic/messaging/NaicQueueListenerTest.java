package estate.myers.naic.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Calls onMessage directly (no broker) and captures what it logs. Messages are
 * proxies because the JMS interfaces are too large to implement by hand.
 */
public class NaicQueueListenerTest {

    // the listener's logger, not this test's: a handler on it captures what onMessage logs
    private final Logger listenerLog = Logger.getLogger(NaicQueueListener.class.getName());
    private final List<LogRecord> records = new ArrayList<>();
    private final Handler capture = new Handler() {
        @Override public void publish(LogRecord record) { records.add(record); }
        @Override public void flush() { }
        @Override public void close() { }
    };

    @Before
    public void setUp() {
        listenerLog.addHandler(capture);
    }

    @After
    public void tearDown() {
        listenerLog.removeHandler(capture);
    }

    private static <T extends Message> T message(Class<T> type, String method, Object result) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (p, m, a) -> {
                    if (!m.getName().equals(method)) {
                        throw new UnsupportedOperationException(m.getName());
                    }
                    if (result instanceof JMSException e) {
                        throw e;
                    }
                    return result;
                }));
    }

    @Test
    public void logsTextMessage() {
        new NaicQueueListener().onMessage(message(TextMessage.class, "getText", "hi there"));

        assertFalse(records.isEmpty());
        LogRecord record = records.getFirst();
        assertEquals(Level.INFO, record.getLevel());
        assertEquals("Received: hi there", record.getMessage());
    }

    @Test
    public void warnsOnNonTextMessage() {
        new NaicQueueListener().onMessage(message(Message.class, "getJMSMessageID", "ID:1"));

        assertFalse(records.isEmpty());
        LogRecord record = records.getFirst();
        assertEquals(Level.WARNING, record.getLevel());
        assertEquals("Received non-text message: ID:1", record.getMessage());
    }

    @Test(expected = IllegalStateException.class)
    public void wrapsJmsException() {
        new NaicQueueListener().onMessage(message(TextMessage.class, "getText", new JMSException("boom")));
    }
}
