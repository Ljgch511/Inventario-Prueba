package com.globalr.inventory.pdf;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/pdf")
public class PdfResource {

    @Inject
    PdfGenerationService pdfService;

    @POST
    @Path("/actas/render")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public PdfDtos.RenderActaResponse renderActa(PdfDtos.RenderActaRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body required");
        }
        return pdfService.renderAndStore(request);
    }

    @GET
    @Path("/download")
    @Produces("application/pdf")
    public Response downloadPdf(@QueryParam("key") String key) {
        if (key == null || key.isBlank()) {
            throw new BadRequestException("Missing 'key' parameter");
        }
        byte[] bytes = pdfService.fetchPdf(key);
        return Response.ok(bytes)
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "inline; filename=\"acta.pdf\"")
                .build();
    }
}
