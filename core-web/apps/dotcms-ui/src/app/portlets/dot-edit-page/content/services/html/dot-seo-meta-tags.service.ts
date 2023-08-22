import { Injectable } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

import {
    SEO_LIMITS,
    SEO_OPTIONS,
    SEO_RULES_COLORS,
    SEO_RULES_ICONS,
    SeoRulesResult
} from '../dot-edit-content-html/models/meta-tags-model';

@Injectable()
export class DotSeoMetaTagsService {
    constructor(private dotMessageService: DotMessageService) {}

    /**
     * Get meta tags
     * @param pageDocument
     * @returns
     */
    public getMetaTags(pageDocument: Document) {
        const metaTags = pageDocument.getElementsByTagName('meta');
        let metaTagsObject = {};

        for (const metaTag of metaTags) {
            metaTagsObject = {
                ...metaTagsObject,
                [metaTag.getAttribute('name') || metaTag.getAttribute('property')]:
                    metaTag.getAttribute('content')
            };
        }

        const favicon = pageDocument.querySelectorAll('link[rel="icon"]');
        const title = pageDocument.querySelectorAll('title');

        metaTagsObject['faviconElements'] = favicon;
        metaTagsObject['titleElements'] = title;
        metaTagsObject['favicon'] = (favicon[0] as HTMLLinkElement).href;
        metaTagsObject['title'] = title[0].innerText;

        return metaTagsObject;
    }

    /**
     * Get getMetaTagsResults
     * @param pageDocument
     * @returns
     */
    public getMetaTagsResults(pageDocument: Document) {
        const result = [];
        const metaTagsObject = this.getMetaTags(pageDocument);
        Object.keys(metaTagsObject).forEach((key) => {
            if (key === SEO_OPTIONS.FAVICON) {
                const items = this.getFaviconItems(metaTagsObject);
                const keyValues = this.getKeyValues(items);
                result.push({
                    key,
                    keyIcon: keyValues.keyIcon,
                    keyColor: keyValues.keyColor,
                    items: items,
                    sort: 1
                });
            }

            if (key === SEO_OPTIONS.OG_DESCRIPTION || key === SEO_OPTIONS.DESCRIPTION) {
                const items = this.getDescriptionItems(metaTagsObject);
                const keyValues = this.getKeyValues(items);
                result.push({
                    key,
                    keyIcon: keyValues.keyIcon,
                    keyColor: keyValues.keyColor,
                    items: items,
                    sort: 2,
                    info: "The length of the description allowed will depend on the reader's device size; on the smallest size only about 110 characters are allowed."
                });
            }

            if (key === SEO_OPTIONS.TITLE) {
                const items = this.getTitleItems(metaTagsObject);
                const keyValues = this.getKeyValues(items);
                result.push({
                    key,
                    keyIcon: keyValues.keyIcon,
                    keyColor: keyValues.keyColor,
                    items: items,
                    sort: 3,
                    info: 'HTML Title content should be between 30 and 60 characters.'
                });
            }
        });

        return result.sort((a, b) => a.sort - b.sort);
    }

    private getFaviconItems(metaTagsObject) {
        const items = [];
        const favicon = metaTagsObject['favicon'];
        const faviconElements = metaTagsObject['faviconElements'];

        if (faviconElements.length === 0) {
            items.push({
                message: 'FavIcon not found!',
                color: SEO_RULES_COLORS.ERROR,
                itemIcon: SEO_RULES_ICONS.TIMES
            });
        }

        if (faviconElements.length > SEO_LIMITS.MAX_FAVICONS) {
            items.push({
                message: 'More than 1 FavIcon found!',
                color: SEO_RULES_COLORS.ERROR,
                itemIcon: SEO_RULES_ICONS.TIMES
            });
        }

        if (favicon && faviconElements.length === SEO_LIMITS.MAX_FAVICONS) {
            items.push({
                message: 'FavIcon found!',
                color: SEO_RULES_COLORS.DONE,
                itemIcon: SEO_RULES_ICONS.CHECK
            });
        }

        return items;
    }

    private getKeyValues(items: SeoRulesResult[]) {
        let keyIcon = SEO_RULES_ICONS.CHECK_CIRCLE;
        let keyColor = SEO_RULES_COLORS.DONE;

        if (items?.some((item) => item.color === SEO_RULES_COLORS.WARNING)) {
            keyIcon = SEO_RULES_ICONS.EXCLAMATION;
            keyColor = SEO_RULES_COLORS.WARNING;
        }

        if (items?.some((item) => item.color === SEO_RULES_COLORS.ERROR)) {
            keyIcon = SEO_RULES_ICONS.EXCLAMATION;
            keyColor = SEO_RULES_COLORS.ERROR;
        }

        return {
            keyIcon,
            keyColor
        };
    }

    private getDescriptionItems(metaTagsObject) {
        const result = [];
        const ogDescription = metaTagsObject['og:description'];
        const description = metaTagsObject['description'];

        if (!ogDescription && description) {
            result.push({
                message: 'Meta Description not found! Showing Description instead.',
                color: SEO_RULES_COLORS.ERROR,
                itemIcon: SEO_RULES_ICONS.TIMES
            });
        }

        if (ogDescription && ogDescription.length === 0) {
            result.push({
                message: 'Meta Description found, but is empty!',
                color: SEO_RULES_COLORS.ERROR,
                itemIcon: SEO_RULES_ICONS.TIMES
            });
        }

        if (ogDescription && ogDescription.length > 0) {
            result.push({
                message: 'Meta Description found!',
                color: SEO_RULES_COLORS.DONE,
                itemIcon: SEO_RULES_ICONS.CHECK
            });
        }

        return result;
    }

    private getTitleItems(metaTagsObject) {
        const result = [];
        const title = metaTagsObject['title'];
        const titleElements = metaTagsObject['titleElements'];

        if (!titleElements) {
            result.push({
                message: 'HTML Title not found!',
                color: SEO_RULES_COLORS.ERROR,
                itemIcon: SEO_RULES_ICONS.TIMES
            });
        }

        if (titleElements?.length > 1) {
            result.push({
                message: 'More than 1 HTML Title found!',
                color: SEO_RULES_COLORS.ERROR,
                itemIcon: SEO_RULES_ICONS.TIMES
            });
        }

        if (title?.length > SEO_LIMITS.MAX_TITLE_LENGTH) {
            result.push({
                message: 'HTML Title found, but but has more than 60 characters.',
                color: SEO_RULES_COLORS.WARNING,
                itemIcon: SEO_RULES_ICONS.EXCLAMATION_CIRCLE
            });
        }

        if (title?.length < SEO_LIMITS.MIN_TITLE_LENGTH) {
            result.push({
                message: 'HTML Title found, but but has fewer than 30 characters of content.',
                color: SEO_RULES_COLORS.WARNING,
                itemIcon: SEO_RULES_ICONS.EXCLAMATION_CIRCLE
            });
        }

        if (
            titleElements?.length === SEO_LIMITS.MAX_TITLES &&
            title?.length > SEO_LIMITS.MAX_TITLE_LENGTH &&
            title?.length < SEO_LIMITS.MIN_TITLE_LENGTH
        ) {
            result.push({
                message: 'HTML Title found, with an appropriate amount of content!',
                color: SEO_RULES_COLORS.DONE,
                itemIcon: SEO_RULES_ICONS.CHECK
            });
        }

        return result;
    }
}
