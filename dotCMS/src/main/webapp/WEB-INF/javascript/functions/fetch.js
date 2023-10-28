/**
 *
 * @param resource
 * @param options
 * @returns {Promise<unknown>}
 */
function fetch(resource, options) {

    return  new Promise(function (myResolve, myReject) {

        try {

            const fetchResponse = options ? fetchtool.fetch(resource, options) : fetchtool.fetch(resource);
            if (fetchResponse.ok()) {
                myResolve(fetchResponse);
            } else {
                myReject(fetchResponse);
            }
        } catch (e) {
            myReject(e);
        }
    });
}
