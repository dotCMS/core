import { dotCMSClient } from '@/lib/dotCMSClient';
import type { PageExtraContent } from '@/types/content';
import { blogQuery, destinationQuery, fragmentNav, navigationQuery } from '@/utils/queries';

/**
 * Fetch a dotCMS page plus the extra GraphQL content (blogs, destinations,
 * navigation) the layout needs, in a single round-trip.
 *
 * On failure it returns `{ error }` so callers can branch without try/catch;
 * use the guards in `@/utils/pageResponse` to narrow the result.
 */
export async function getDotCMSPage(path: string) {
    try {
        return await dotCMSClient.page.get<{ content: PageExtraContent }>(path, {
            graphql: {
                content: {
                    blogs: blogQuery,
                    destinations: destinationQuery,
                    navigation: navigationQuery
                },
                fragments: [fragmentNav]
            }
        });
    } catch (error) {
        return { error };
    }
}
