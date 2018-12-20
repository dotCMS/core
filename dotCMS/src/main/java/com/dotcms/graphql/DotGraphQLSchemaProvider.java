package com.dotcms.graphql;

import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.graphql.datafetcher.ContentletDataFetcher;
import com.dotcms.graphql.datafetcher.FieldDataFetcher;
import com.dotcms.graphql.datafetcher.RelationshipFieldDataFetcher;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.HandshakeRequest;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.errors.SchemaProblem;
import graphql.servlet.GraphQLSchemaProvider;

import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
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

            GraphQLInterfaceType contentInterface = GraphQLInterfaceType.newInterface().name("Content")
                .field(newFieldDefinition()
                    .name("identifier")
                    .type(GraphQLID)
                    .dataFetcher(new FieldDataFetcher()))
                .field(newFieldDefinition()
                    .name("title")
                    .type(GraphQLString)
                    .dataFetcher(new FieldDataFetcher()))
                .typeResolver(new ContentResolver())
                .build();

            GraphQLObjectType newsType = newObject()
                .name("News")
                .withInterface(contentInterface)
                .field(newFieldDefinition()
                    .name("identifier")
                    .type(GraphQLID)
                    .dataFetcher(new FieldDataFetcher()))
                .field(newFieldDefinition()
                    .name("title")
                    .type(GraphQLString)
                    .dataFetcher(new FieldDataFetcher()))
                .build();


            GraphQLObjectType queryType = newObject()
                .name("Query")
                .field(newFieldDefinition()
                    .name("search")
                    .argument(GraphQLArgument.newArgument()
                        .name("query")
                        .type(GraphQLString)
                        .build())
                    .type(GraphQLList.list(contentInterface))
                    .dataFetcher(new ContentletDataFetcher()))
                .build();

            Set<GraphQLType> additionalTypes = new HashSet<>(Arrays.asList(newsType));

            return new GraphQLSchema(queryType, null, additionalTypes);
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


            final RuntimeWiring.Builder builder = newRuntimeWiring();
            builder.type("Query", typeWiring ->
                typeWiring.dataFetcher("search", new ContentletDataFetcher()));

            builder.type("Content", typeWiring ->
                typeWiring.typeResolver(new ContentResolver()));

            builder.type("Content", typeWiring ->
                typeWiring.dataFetcher("identifier", new FieldDataFetcher()));

            builder.type("Content", typeWiring ->
                typeWiring.dataFetcher("title", new FieldDataFetcher()));

            // typeWiring for Employee
            final ContentType employeeType = APILocator.getContentTypeAPI(APILocator.systemUser()).find("Employee");
            employeeType.fields().stream().forEach((field) -> {
                builder.type(employeeType.variable(), typeWiring
                    -> typeWiring.dataFetcher(field.variable(), new FieldDataFetcher()));
            } );

            // typeWiring for Youtube
            final ContentType youtubeType = APILocator.getContentTypeAPI(APILocator.systemUser()).find("Youtube");

            youtubeType.fields().stream().forEach((field) -> {
                builder.type(youtubeType.variable(), typeWiring
                    -> typeWiring.dataFetcher(field.variable(), new FieldDataFetcher()));
            } );

            // typeWiring for news
            final ContentType newsType = APILocator.getContentTypeAPI(APILocator.systemUser()).find("News");

            newsType.fields().stream().forEach((field) -> {

                if(field instanceof RelationshipField) {
                    builder.type("News", typeWiring
                        -> typeWiring.dataFetcher(field.variable(), new RelationshipFieldDataFetcher()));
                } else {
                    builder.type("News", typeWiring
                        -> typeWiring.dataFetcher(field.variable(), new FieldDataFetcher()));
                }
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
