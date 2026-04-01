package olympus.fhirvalidator.api;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import java.util.UUID;
import olympus.fhirvalidator.domain.ValidateResponse;

@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
  @Override
  public Response toResponse(IllegalArgumentException exception) {
    String requestId = UUID.randomUUID().toString();
    ValidateResponse body =
        ValidateResponse.error("bad_request", exception.getMessage(), requestId, Map.of("type", "IllegalArgumentException"));
    return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(body).build();
  }
}
