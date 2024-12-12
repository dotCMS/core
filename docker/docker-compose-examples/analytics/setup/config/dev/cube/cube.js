const fetch = require('node-fetch');

// cube.js configuration file
module.exports = {
    /*
    contextToAppId: ({ securityContext }) =>
        `CUBEJS_APP_${securityContext.customerId}`,
    preAggregationsSchema: ({ securityContext }) =>
        `pre_aggregations_${securityContext.customerId}`,
*/


    queryRewrite: (query, { securityContext }) => {

        console.log('HERE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!', securityContext);
        if (!securityContext) {
            throw new Error('No valid token');
        }

        const tokenData = securityContext["https://dotcms.com/analytics"];

        const isRequestQuery = (query.measures + query.dimensions).includes("request.");

        if (isRequestQuery) {
            query.filters.push({
                member: 'request.clusterId',
                operator: 'equals',
                values: [tokenData.clusterId],
            });

            query.filters.push({
                member: 'request.customerId',
                operator: 'equals',
                values: [tokenData.customerId],
            });
        }


        return query;
    },


};