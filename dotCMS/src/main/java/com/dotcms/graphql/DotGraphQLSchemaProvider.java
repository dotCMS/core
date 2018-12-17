package com.dotcms.graphql;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.graphql.datafetcher.ContentletDataFetcher;
import com.dotcms.graphql.datafetcher.FieldDataFetcher;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.HandshakeRequest;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.errors.SchemaProblem;
import graphql.servlet.GraphQLSchemaProvider;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

public class DotGraphQLSchemaProvider implements GraphQLSchemaProvider {
    @Override
    public GraphQLSchema getSchema(HttpServletRequest request) {
        return getSchema();
    }

    @Override
    public GraphQLSchema getSchema(HandshakeRequest request) {
        return getSchema();
    }

    @Override
    public GraphQLSchema getSchema() {
        try {
            final File schemaFile = loadSchema("com/dotcms/graphql/schema.graphqls");

            SchemaParser schemaParser = new SchemaParser();
            TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schemaFile);

            RuntimeWiring wiring = buildRuntimeWiring();
            SchemaGenerator schemaGenerator = new SchemaGenerator();
            return schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, wiring);
        } catch(SchemaProblem e) {
            Logger.error(this, "Error with schema", e);
            throw e;
        }
    }

    @Override
    public GraphQLSchema getReadOnlySchema(HttpServletRequest request) {
        return null;
    }

    private RuntimeWiring buildRuntimeWiring() {

        try {


            // typeWiring for Employee
            final ContentType employeeType = APILocator.getContentTypeAPI(APILocator.systemUser()).find("Employee");

            final RuntimeWiring.Builder builder = newRuntimeWiring();
            builder.type("Query", typeWiring ->
                typeWiring.dataFetcher("search", new ContentletDataFetcher()));

            builder.type("Content", typeWiring ->
                typeWiring.typeResolver(new ContentResolver()));

            builder.type("Content", typeWiring ->
                typeWiring.dataFetcher("identifier", new FieldDataFetcher()));

            builder.type("Content", typeWiring ->
                typeWiring.dataFetcher("title", new FieldDataFetcher()));

            employeeType.fields().stream().forEach((field) -> {
                builder.type("Employee", typeWiring
                    -> typeWiring.dataFetcher(field.variable(), new FieldDataFetcher()));
            } );

            // typeWiring for news
            final ContentType newsType = APILocator.getContentTypeAPI(APILocator.systemUser()).find("News");

            newsType.fields().stream().forEach((field) -> {
                builder.type("News", typeWiring
                    -> typeWiring.dataFetcher(field.variable(), new FieldDataFetcher()));
            } );



            return  builder.build();

//            return newRuntimeWiring()
//                .type("Query", typeWiring ->
//                    typeWiring.dataFetcher("content", new ContentletDataFetcher()))
//                .type("Content", typeWiring ->
//                    typeWiring.dataFetcher("map", new FieldDataFetcher()))
//                .build();
        } catch(DotSecurityException | DotDataException e) {
            Logger.error(this, "Error building runtime wiring", e);
        }

        return null;
    }

    private File loadSchema(final String fileName) {
        Path path;
        try {
            path = Paths.get(getClass().getClassLoader()
                .getResource(fileName).toURI());
        } catch (URISyntaxException e) {
            throw new DotRuntimeException("Unable to load schema", e);
        }
        return path.toFile();
    }
}
