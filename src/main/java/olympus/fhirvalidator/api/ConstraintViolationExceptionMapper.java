package olympus.fhirvalidator.api;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import olympus.fhirvalidator.domain.ValidateResponse;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
  @Override
  public Response toResponse(ConstraintViolationException exception) {
    String requestId = UUID.randomUUID().toString();
    String messages = exception.getConstraintViolations().stream().map(v -> v.getMessage())
        .collect(Collectors.joining("; "));
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("violations", exception.getConstraintViolations().stream()
        .map(v -> Map.of("path", String.valueOf(v.getPropertyPath()), "message", v.getMessage())).collect(Collectors.toList()));
    ValidateResponse body = ValidateResponse.error("validation_error", messages, requestId, details);
    return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(body).build();
  }
}
