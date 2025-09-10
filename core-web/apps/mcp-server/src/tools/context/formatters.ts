import { getCacheAge } from './cache';

import { ContentType } from '../../types/contentype';
import { Site } from '../../types/site';
import { WorkflowScheme } from '../../types/workflow';
import { formatContentTypesAsText } from '../content-types/formatters';

/**
 * Formats site information for context response
 */
export function formatSiteInfo(site: Site): string {
    const dotcmsUrl = process.env.DOTCMS_URL || '';

    if (!dotcmsUrl) {
        return `Current Site: ${site.name} (${site.hostname})

IMPORTANT: When creating or updating content, use this current site's identifier (${site.identifier}) for any host or site fields. This ensures content is properly associated with the current site.

TIPS:

1. To enable content editing URLs, set the DOTCMS_URL environment variable to your dotCMS instance URL (e.g., https://demo.dotcms.com)

2. Once DOTCMS_URL is set, you'll be able to access:
   - Content editing: https://<your-dotcms-url>/dotAdmin/#/c/content/<content-inode>/
   - Content type editing: https://<your-dotcms-url>/dotAdmin/#/content-types-angular/edit/<content-type-identifier>`;
    }

    return `Current Site: ${site.name} (${site.hostname})

IMPORTANT: When creating or updating content, use this current site's identifier (${site.identifier}) for any host or site fields. This ensures content is properly associated with the current site.

TIPS:

1. Url to edit a piece of content after creation:
https://${dotcmsUrl}/dotAdmin/#/c/content/<content-inode>/

2. Url to edit a content type after creation:
https://${dotcmsUrl}/dotAdmin/#/content-types-angular/edit/<content-type-identifier>
`;
}

/**
 * Formats workflow schemes for context response
 */
export function formatWorkflowSchemesAsText(workflowSchemes: WorkflowScheme[]): string {
    if (workflowSchemes.length === 0) {
        return 'No workflow schemes found.';
    }

    const schemesText = workflowSchemes
        .map((scheme) => {
            const status = scheme.archived ? '[ARCHIVED]' : '[ACTIVE]';
            const systemFlag = scheme.system ? ' (SYSTEM)' : '';
            const defaultFlag = scheme.defaultScheme ? ' (DEFAULT)' : '';

            return `${status} ${scheme.name}${systemFlag}${defaultFlag}
  ID: ${scheme.id}
  Variable Name: ${scheme.variableName}
  Description: ${scheme.description || 'No description'}
  Mandatory: ${scheme.mandatory}
  Creation Date: ${new Date(scheme.creationDate).toISOString()}
  Modified Date: ${new Date(scheme.modDate).toISOString()}`;
        })
        .join('\n\n');

    return `WORKFLOW SCHEMES (${workflowSchemes.length} total):

${schemesText}`;
}

/**
 * Creates the complete context response text
 */
export function createResponseText(
    contentTypes: ContentType[],
    site: Site,
    workflowSchemes: WorkflowScheme[],
    isCached: boolean,
    cacheAge?: number
): string {
    const formattedText = formatContentTypesAsText(contentTypes);
    const workflowText = formatWorkflowSchemesAsText(workflowSchemes);
    const siteInfo = formatSiteInfo(site);
    const cacheInfo = isCached
        ? `[CACHED RESPONSE] (cached for ${cacheAge}s)`
        : `[FRESH RESPONSE] (cached for ${getCacheAge()}s)`;

    return `${cacheInfo} ${siteInfo}:\n\n${formattedText}\n\n${workflowText}`;
}
