// cube.js configuration file
module.exports = {
    /*
    contextToAppId: ({ securityContext }) =>
        `CUBEJS_APP_${securityContext.customerId}`,
    preAggregationsSchema: ({ securityContext }) =>
        `pre_aggregations_${securityContext.customerId}`,
*/

    queryRewrite: (query, { securityContext }) => {


        if (!securityContext) {
            throw new Error('No valid token');
        }

        const tokenData = securityContext["https://dotcms.com/analytics"];

            query.filters.push({
                member: 'Events.clusterId',
                operator: 'equals',
                values: [tokenData.clusterId],
            });


        return query;
    },


};