package olympus.fhirvalidator.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.validation.Valid;
import olympus.fhirvalidator.application.ValidationService;
import olympus.fhirvalidator.domain.ValidateRequest;
import olympus.fhirvalidator.domain.ValidateResponse;

@Path("/validate")
@ApplicationScoped
public class ValidateResource {

  @Inject
  ValidationService validationService;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response validate(@Valid ValidateRequest request) {
    ValidateResponse res = validationService.validate(request);
    return Response.ok(res).build();
  }
}

