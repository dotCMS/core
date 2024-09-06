import { permanentRedirect, redirect } from "next/navigation";

/**
 * Vanity URL handler
 * This function will handle the vanity URL redirect
 *
 * More info about Vanity URL: https://dotcms.com/docs/latest/vanity-urls
 * NextJS Navigation:
 *  - permanentRedirect: https://nextjs.org/docs/app/building-your-application/routing/redirecting#permanentredirect-function
 *  - redirect: https://nextjs.org/docs/app/building-your-application/routing/redirecting#redirect-function
 *
 * @param {*} vanityUrl
 */
export function handleVanityUrlRedirect({
    forwardTo,
    temporaryRedirect,
    permanentRedirect: isPermanentRedirect,
}) {
    if (temporaryRedirect) {
        redirect(forwardTo);
    } else if (isPermanentRedirect) {
        permanentRedirect(forwardTo, "replace");
    }
}
