package estate.myers.naic.endpoint;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class HelloEndpoint {

    @GET
    @Path("/{user}")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@PathParam("user") String user) {
        return "Hello " + user;
    }
}
