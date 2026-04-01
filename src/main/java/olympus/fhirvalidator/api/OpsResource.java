package olympus.fhirvalidator.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import olympus.fhirvalidator.application.OpsService;
import olympus.fhirvalidator.domain.WarmupRequest;

@Path("/v1")
@ApplicationScoped
public class OpsResource {
  @Inject
  OpsService opsService;

  @GET
  @Path("/capabilities")
  @Produces(MediaType.APPLICATION_JSON)
  public Response capabilities() {
    return Response.ok(opsService.capabilities()).build();
  }

  @POST
  @Path("/warmup")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response warmup(@Valid WarmupRequest request) {
    return Response.ok(opsService.warmup(request)).build();
  }

  @GET
  @Path("/ready")
  @Produces(MediaType.APPLICATION_JSON)
  public Response ready() {
    var ready = opsService.ready();
    if (ready.ready) {
      return Response.ok(ready).build();
    }
    return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(ready).build();
  }

  @GET
  @Path("/config")
  @Produces(MediaType.APPLICATION_JSON)
  public Response config() {
    return Response.ok(opsService.config()).build();
  }
}
