package olympus.fhirvalidator.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import olympus.fhirvalidator.domain.UploadedIgResponse;
import olympus.fhirvalidator.infrastructure.IgUploadStore;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("/v1/igs/upload")
@ApplicationScoped
public class IgUploadResource {
  @Inject
  IgUploadStore uploadStore;

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response upload(@RestForm("file") FileUpload file) throws Exception {
    String requestId = UUID.randomUUID().toString();
    String reference = uploadStore.stage(file);
    UploadedIgResponse out = new UploadedIgResponse();
    out.requestId = requestId;
    out.reference = reference;
    out.filename = file == null ? null : file.fileName();
    return Response.ok(out).build();
  }
}
