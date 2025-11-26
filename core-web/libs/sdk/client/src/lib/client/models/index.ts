/**
 * A record of HTTP status codes and their corresponding error messages.
 *
 * @type {Record<number, string>}
 * @property {string} 401 - Unauthorized. Check the token and try again.
 * @property {string} 403 - Forbidden. Check the permissions and try again.
 * @property {string} 404 - Not Found. Check the URL and try again.
 * @property {string} 500 - Internal Server Error. Try again later.
 * @property {string} 502 - Bad Gateway. Try again later.
 * @property {string} 503 - Service Unavailable. Try again later.
 */
export const ErrorMessages: Record<number, string> = {
    401: 'Unauthorized. Check the token and try again.',
    403: 'Forbidden. Check the permissions and try again.',
    404: 'Not Found. Check the URL and try again.',
    500: 'Internal Server Error. Try again later.',
    502: 'Bad Gateway. Try again later.',
    503: 'Service Unavailable. Try again later.'
};
