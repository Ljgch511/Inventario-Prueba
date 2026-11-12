package com.globalr.inventory.media;

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
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("/api/v1/media")
public class MediaResource {

    @Inject
    MinioMediaService mediaService;

    @POST
    @Path("/compress-and-store")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public MediaDtos.MediaUploadResponse upload(
            @RestForm("file") FileUpload file,
            @RestForm("countryCode") String countryCode,
            @RestForm("assetId") String assetId,
            @RestForm("stage") String stage
    ) {
        if (file == null || file.uploadedFile() == null) {
            throw new BadRequestException("Missing 'file' part");
        }
        return mediaService.compressAndStore(file.uploadedFile(), countryCode, assetId, stage);
    }

    @GET
    @Path("/object")
    @Produces("image/jpeg")
    public Response download(@QueryParam("key") String key) {
        if (key == null || key.isBlank()) {
            throw new BadRequestException("Missing 'key' query parameter");
        }
        byte[] bytes = mediaService.fetchObject(key);
        return Response.ok(bytes).header("Content-Type", "image/jpeg").build();
    }
}
