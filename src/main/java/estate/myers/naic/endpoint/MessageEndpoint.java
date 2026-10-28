package estate.myers.naic.endpoint;

import estate.myers.naic.messaging.NaicQueueListener;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/messages")
@RequestScoped
public class MessageEndpoint {

    // package-private so tests can supply them without CDI
    @Inject
    JMSContext context;

    @Resource(lookup = NaicQueueListener.QUEUE_JNDI)
    Queue queue;

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response send(String body) {
        context.createProducer().send(queue, body);
        return Response.accepted().build();
    }
}
