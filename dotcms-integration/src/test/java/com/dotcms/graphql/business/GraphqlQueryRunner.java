package com.dotcms.graphql.business;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.schema.GraphQLSchema;
import java.util.List;
import java.util.Map;

/**
 * Runs real GraphQL queries against the live schema from an integration test.
 *
 * <p>Before this existed, no integration test executed a query — {@code GraphqlAPITest} only
 * inspects the shape of the schema, so the only place a query's actual <b>values</b> were
 * asserted was the Postman collection. That left a gap: a change could keep every type and field
 * in place while quietly altering what a field returns, and nothing in the Java test suite would
 * notice.
 *
 * <p>The context matters. {@code FileFieldDataFetcher} and friends read the calling user from
 * {@link DotGraphQLContext} via {@code environment.getContext()}, so a query executed without one
 * resolves nothing. That is why every execution here is given a context carrying the user.
 *
 * <p>Not a test class — named so surefire/failsafe do not try to run it.
 */
public class GraphqlQueryRunner {

    private GraphqlQueryRunner() {
    }

    /**
     * Executes {@code query} as {@code user} against the current schema.
     *
     * @return the raw result, errors included — callers that want a failure to be loud should use
     * {@link #executeAndExpectSuccess(String, User)} instead.
     */
    public static ExecutionResult execute(final String query, final User user) throws Exception {
        final GraphQLSchema schema = APILocator.getGraphqlAPI().getSchema(user);
        final DotGraphQLContext context = DotGraphQLContext.createServletContext()
                .with(user)
                .build();

        final ExecutionInput input = ExecutionInput.newExecutionInput()
                .query(query)
                .context(context)
                .build();

        return GraphQL.newGraphQL(schema).build().execute(input);
    }

    /**
     * Executes {@code query} and returns its {@code data}, failing with the GraphQL errors in the
     * message if the query did not succeed. Use this when the query is expected to work — a bare
     * {@code NullPointerException} on the data map tells you nothing about why.
     */
    public static Map<String, Object> executeAndExpectSuccess(final String query, final User user)
            throws Exception {
        final ExecutionResult result = execute(query, user);
        final List<GraphQLError> errors = result.getErrors();
        if (errors != null && !errors.isEmpty()) {
            throw new AssertionError("GraphQL query failed: " + errors + "\nQuery was:\n" + query);
        }
        return result.getData();
    }

    /**
     * Executes {@code query} expecting it to be rejected, and returns the errors.
     *
     * @throws AssertionError if the query unexpectedly succeeded
     */
    public static List<GraphQLError> executeAndExpectFailure(final String query, final User user)
            throws Exception {
        final ExecutionResult result = execute(query, user);
        final List<GraphQLError> errors = result.getErrors();
        if (errors == null || errors.isEmpty()) {
            throw new AssertionError("Expected the query to be rejected, but it succeeded:\n" + query);
        }
        return errors;
    }
}
