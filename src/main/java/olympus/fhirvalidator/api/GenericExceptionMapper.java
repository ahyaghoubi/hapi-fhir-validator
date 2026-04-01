package olympus.fhirvalidator.api;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import java.util.UUID;
import olympus.fhirvalidator.domain.ValidateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {
  private static final Logger log = LoggerFactory.getLogger(GenericExceptionMapper.class);

  @Override
  public Response toResponse(Exception exception) {
    String requestId = UUID.randomUUID().toString();
    log.error("Unhandled exception requestId={}", requestId, exception);
    ValidateResponse body =
        ValidateResponse.error("internal_error", "unexpected server error", requestId, Map.of("type", exception.getClass().getSimpleName()));
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(body).build();
  }
}
