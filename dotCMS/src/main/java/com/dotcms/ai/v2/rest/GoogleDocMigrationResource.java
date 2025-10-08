package com.dotcms.ai.v2.rest;

import com.dotcms.ai.v2.api.ConversationAPI;
import com.dotcms.ai.v2.api.provider.config.ModelConfig;
import com.dotcms.ai.v2.api.provider.config.ModelConfigFactory;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DoesNotExistException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Path("/v2/ai/migration/googledoc")
@Produces(MediaType.APPLICATION_JSON)
public class GoogleDocMigrationResource {

    private final ConversationAPI conversationAPI;
    private final ModelConfigFactory modelConfigFactory;


    @Inject
    public GoogleDocMigrationResource(final ConversationAPI conversationAPI,
                                final ModelConfigFactory modelConfigFactory) {
        this.conversationAPI = conversationAPI;
        this.modelConfigFactory = modelConfigFactory;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public String migrateGoogleDoc(@Context final HttpServletRequest req, // this should be async respone
                                     @Context final HttpServletResponse response,
                                     final MigrationRequestForm migrationRequest) throws IOException, InterruptedException {

        final String docUrl = migrationRequest.getGoogleDocUrl();
        final String typeVar = migrationRequest.getContentTypeVarname();
        final String fieldVar = migrationRequest.getFieldVariableName();
        final String docHtmlContent = fetchGoogleDocAsHtml(docUrl);

        if (docHtmlContent == null || docHtmlContent.isEmpty()) {
            throw new DoesNotExistException("Document " + docUrl + " not found");
        }

        final String providerKey = "openai/gpt-4o-mini"; // todo: hardcoded by now for testing
        final ModelConfig modelConfig = this.modelConfigFactory.get(providerKey);
        return this.conversationAPI.migration(typeVar, fieldVar, docHtmlContent, providerKey, modelConfig);
    }

    private String fetchGoogleDocAsHtml(final String docUrl) throws IOException, InterruptedException {

        final String exportUrl = docUrl.replace("/edit", "/export?format=html");

        // todo: change for the circuit
        final HttpClient client = HttpClient.newBuilder().build();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(exportUrl))
                .header("User-Agent", "dotCMS-GoogleDoc-Migration-Agent")
                .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body();
        }

        throw new DotStateException("Could not retrieve the docUrl: " + docUrl + ", status code: " + response.statusCode());
    }
}
