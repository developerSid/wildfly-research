package estate.myers.naic.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Queue;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;
import org.junit.Before;
import org.junit.Test;

/**
 * Same in-memory dispatcher as {@link HelloEndpointTest}. There's no CDI, so
 * the JMSContext and Queue are hand-made proxies that record what was sent.
 */
public class MessageEndpointTest {

    private Dispatcher dispatcher;
    private final Queue queue = proxy(Queue.class, (p, m, a) -> m.getName().equals("toString") ? "queue" : null);
    private final List<Object[]> sent = new ArrayList<>();

    @Before
    public void setUp() {
        JMSProducer producer = proxy(JMSProducer.class, (p, m, a) -> {
            if (m.getName().equals("send")) {
                sent.add(a);
            }
            return p;
        });
        MessageEndpoint endpoint = new MessageEndpoint();
        endpoint.context = proxy(JMSContext.class, (p, m, a) -> producer);
        endpoint.queue = queue;

        dispatcher = MockDispatcherFactory.createDispatcher();
        dispatcher.getRegistry().addSingletonResource(endpoint);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private MockHttpResponse invoke(MockHttpRequest request) {
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        return response;
    }

    @Test
    public void postSendsBodyToQueue() throws Exception {
        MockHttpRequest request = MockHttpRequest.post("/messages")
                .contentType(MediaType.TEXT_PLAIN_TYPE).content("hi there".getBytes());

        assertEquals(202, invoke(request).getStatus());
        assertFalse(sent.isEmpty());
        Object[] sendArgs = sent.getFirst();
        assertSame(queue, sendArgs[0]);
        assertEquals("hi there", sendArgs[1]);
    }

    @Test
    public void postRejectsNonTextBody() throws Exception {
        MockHttpRequest request = MockHttpRequest.post("/messages")
                .contentType(MediaType.APPLICATION_JSON_TYPE).content("{}".getBytes());

        assertEquals(415, invoke(request).getStatus());
        assertTrue(sent.isEmpty());
    }

    @Test
    public void getIsNotAllowed() throws Exception {
        assertEquals(405, invoke(MockHttpRequest.get("/messages")).getStatus());
    }
}
