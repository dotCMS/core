// cube.js configuration file

function resolveToken(securityContext) {
    if (!securityContext) {
        const hasFallback = !!process.env.CUBEJS_OVERRIDE_CUSTOMER && !!process.env.CUBEJS_OVERRIDE_CLUSTER;
        if (!hasFallback) {
            throw new Error('No valid token');
        }

        return {
            clusterId: process.env.CUBEJS_OVERRIDE_CUSTOMER,
            customerId: process.env.CUBEJS_OVERRIDE_CLUSTER
        };
    }

    return securityContext["https://dotcms.com/analytics"];
}

module.exports = {
    /*contextToAppId: ({ securityContext }) =>
        `CUBEJS_APP_${securityContext.customerId}`,
    preAggregationsSchema: ({ securityContext }) =>
        `pre_aggregations_${securityContext.customerId}`,*/

    orchestratorOptions: {
        continueWaitTimeout: 30
    },

    queryRewrite: (query, { securityContext }) => {
        const tokenData = resolveToken(securityContext);
        console.log(`tokenData: ${JSON.stringify(tokenData, null, 2)}`);
        const isRequestQuery = (query.measures + query.dimensions).includes("request.");

        if (isRequestQuery) {
            if (tokenData.clusterId) {
                query.filters.push({
                    member: 'request.clusterId',
                    operator: 'equals',
                    values: [tokenData.clusterId],
                });
            }

            query.filters.push({
                member: 'request.customerId',
                operator: 'equals',
                values: [tokenData.customerId],
            });
        }

        console.log(`query: ${JSON.stringify(query, null, 2)}`);

        return query;
    },
};
