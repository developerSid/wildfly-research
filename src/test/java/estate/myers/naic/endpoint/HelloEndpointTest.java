package estate.myers.naic.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;
import org.junit.Before;
import org.junit.Test;

/**
 * Runs requests through the RESTEasy dispatcher in memory (no server), so
 * routing, parameter binding and content negotiation are exercised like a
 * real request. Paths are relative to the @ApplicationPath ("/api").
 */
public class HelloEndpointTest {

    private Dispatcher dispatcher;

    @Before
    public void setUp() {
        dispatcher = MockDispatcherFactory.createDispatcher();
        dispatcher.getRegistry().addPerRequestResource(HelloEndpoint.class);
    }

    private MockHttpResponse invoke(MockHttpRequest request) {
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        return response;
    }

    @Test
    public void getHelloGreetsUser() throws Exception {
        MockHttpResponse response = invoke(MockHttpRequest.get("/hello/gary"));

        assertEquals(200, response.getStatus());
        assertEquals("Hello gary", response.getContentAsString());
        assertTrue(response.getOutputHeaders().getFirst(HttpHeaders.CONTENT_TYPE)
                .toString().startsWith(MediaType.TEXT_PLAIN));
    }

    @Test
    public void getHelloDecodesPathParam() throws Exception {
        MockHttpResponse response = invoke(MockHttpRequest.get("/hello/Mary%20Ann"));

        assertEquals(200, response.getStatus());
        assertEquals("Hello Mary Ann", response.getContentAsString());
    }

    @Test
    public void getHelloWithoutUserIsNotFound() throws Exception {
        assertEquals(404, invoke(MockHttpRequest.get("/hello")).getStatus());
    }

    @Test
    public void postHelloIsNotAllowed() throws Exception {
        assertEquals(405, invoke(MockHttpRequest.post("/hello/gary")).getStatus());
    }

    @Test
    public void getHelloRejectsUnsupportedAccept() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/hello/gary").accept(MediaType.APPLICATION_JSON);

        assertEquals(406, invoke(request).getStatus());
    }
}
