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
 * Runs a GraphQL query against the schema built for a {@link User}, for integration tests.
 * A trimmed copy of the helper on main.
 */
public class GraphqlQueryRunner {

    private GraphqlQueryRunner() {
    }

    /**
     * Executes the query as the given user and returns the raw result.
     *
     * @param query the GraphQL query
     * @param user  the user to run it as
     * @return the {@link ExecutionResult}
     * @throws Exception if the schema cannot be built
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
     * Executes the query and fails the test when it returns any GraphQL error.
     *
     * @param query the GraphQL query
     * @param user  the user to run it as
     * @return the data of the result
     * @throws Exception if the schema cannot be built
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
}
