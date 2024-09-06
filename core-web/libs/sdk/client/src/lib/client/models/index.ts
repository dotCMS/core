export const ErrorMessages: Record<number, string> = {
    401: 'Unauthorized. Check the token and try again.',
    403: 'Forbidden. Check the permissions and try again.',
    404: 'Not Found. Check the URL and try again.',
    500: 'Internal Server Error. Try again later.',
    502: 'Bad Gateway. Try again later.',
    503: 'Service Unavailable. Try again later.'
};
