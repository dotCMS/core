import { Observable, from, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { map, switchMap, toArray } from 'rxjs/operators';

import { DotMessageService, DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';

import {
    SeoMetaTags,
    SEO_LIMITS,
    SEO_OPTIONS,
    SEO_RULES_COLORS,
    SEO_RULES_ICONS,
    SeoKeyResult,
    SeoRulesResult,
    SeoMetaTagsResult,
    SeoMediaKeys,
    ImageMetaData,
    OpenGraphOptions
} from '../dot-edit-content-html/models/meta-tags-model';

@Injectable()
export class DotSeoMetaTagsService {
    constructor(
        private dotMessageService: DotMessageService,
        private dotUploadService: DotUploadService
    ) {}

    /**
     * Get meta tags from the document
     * @param pageDocument
     * @returns
     */
    getMetaTags(pageDocument: Document): SeoMetaTags {
        const metaTags = pageDocument.getElementsByTagName('meta');
        const metaTagsObject = {};

        for (const metaTag of metaTags) {
            const name = metaTag.getAttribute('name');
            const property = metaTag.getAttribute('property');
            const content = metaTag.getAttribute('content');

            const key = name ?? property;

            if (key) {
                metaTagsObject[key] = content;
            }
        }

        const favicon = pageDocument.querySelectorAll('link[rel="icon"]');
        const title = pageDocument.querySelectorAll('title');
        const titleOgElements = pageDocument.querySelectorAll('meta[property="og:title"]');
        const imagesOgElements = pageDocument.querySelectorAll('meta[property="og:image"]');

        metaTagsObject['faviconElements'] = favicon;
        metaTagsObject['titleElements'] = title;
        metaTagsObject['favicon'] = (favicon[0] as HTMLLinkElement)?.href;
        metaTagsObject['title'] = title[0]?.innerText;
        metaTagsObject['titleOgElements'] = titleOgElements;
        metaTagsObject['imageOgElements'] = imagesOgElements;

        return metaTagsObject;
    }

    /**
     * Get the object with the SEO Result
     * @param pageDocument
     * @returns
     */
    getMetaTagsResults(pageDocument: Document): Observable<SeoMetaTagsResult[]> {
        const metaTagsObject = this.getMetaTags(pageDocument);
        const ogMap = this.openGraphMap();

        return from(SeoMediaKeys.all).pipe(
            switchMap((key) => {
                const itemsObservable = ogMap[key]?.getItems(metaTagsObject);

                return itemsObservable?.pipe(
                    map((items) => {
                        const keysValues = this.getKeyValues(items);

                        return {
                            key,
                            keyIcon: keysValues.keyIcon,
                            keyColor: keysValues.keyColor,
                            items: items,
                            sort: ogMap[key]?.sort,
                            info: ogMap[key]?.info
                        };
                    })
                );
            }),
            toArray()
        );
    }

    /**
     * Filter and sorted the meta tags by media
     * @param results
     * @param seoMedia
     * @returns
     */
    getFilteredMetaTagsByMedia(
        results: SeoMetaTagsResult[],
        seoMedia: string
    ): SeoMetaTagsResult[] {
        return results
            .filter((result) =>
                SeoMediaKeys[seoMedia.toLowerCase()].includes(result.key.toLowerCase())
            )
            .sort((a, b) => a.sort - b.sort);
    }

    /**
     * This returns the map of the open graph elements and their properties.
     */
    private openGraphMap(): OpenGraphOptions {
        return {
            [SEO_OPTIONS.FAVICON]: {
                getItems: (metaTagsObject: SeoMetaTags) => of(this.getFaviconItems(metaTagsObject)),
                sort: 1,
                info: ''
            },
            [SEO_OPTIONS.DESCRIPTION]: {
                getItems: (metaTagsObject: SeoMetaTags) =>
                    of(this.getDescriptionItems(metaTagsObject)),
                sort: 2,
                info: this.dotMessageService.get('seo.rules.description.info')
            },
            [SEO_OPTIONS.OG_DESCRIPTION]: {
                getItems: (metaTagsObject: SeoMetaTags) =>
                    of(this.getDescriptionItems(metaTagsObject)),
                sort: 3,
                info: this.dotMessageService.get('seo.rules.description.info')
            },
            [SEO_OPTIONS.TITLE]: {
                getItems: (metaTagsObject: SeoMetaTags) => of(this.getTitleItems(metaTagsObject)),
                sort: 4,
                info: this.dotMessageService.get('seo.rules.title.info')
            },
            [SEO_OPTIONS.OG_TITLE]: {
                getItems: (metaTagsObject: SeoMetaTags) => of(this.getOgTitleItems(metaTagsObject)),
                sort: 5,
                info: this.dotMessageService.get('seo.rules.title.info')
            },
            [SEO_OPTIONS.OG_IMAGE]: {
                getItems: (metaTagsObject: SeoMetaTags) => this.getOgImagesItems(metaTagsObject),
                sort: 6,
                info: ''
            }
        };
    }

    private getFaviconItems(metaTagsObject: SeoMetaTags): SeoRulesResult[] {
        const items: SeoRulesResult[] = [];
        const favicon = metaTagsObject['favicon'];
        const faviconElements = metaTagsObject['faviconElements'];

        if (faviconElements.length === 0) {
            items.push({
                message: this.dotMessageService.get('seo.rules.favicon.not.found'),
                color: SEO_RULES_COLORS.ERROR,
                itemIcon: SEO_RULES_ICONS.TIMES
            });
        }

        if (faviconElements.length > SEO_LIMITS.MAX_FAVICONS) {
            items.push({
                message: this.dotMessageService.get('seo.rules.favicon.more.one.found'),
                color: SEO_RULES_COLORS.ERROR,
                itemIcon: SEO_RULES_ICONS.TIMES
            });
        }

        if (favicon && faviconElements.length === SEO_LIMITS.MAX_FAVICONS) {
            items.push({
                message: this.dotMessageService.get('seo.rules.favicon.found'),
                color: SEO_RULES_COLORS.DONE,
                itemIcon: SEO_RULES_ICONS.CHECK
            });
        }

        return items;
    }

    private getKeyValues(items: SeoRulesResult[]): SeoKeyResult {
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

    private getDescriptionItems(metaTagsObject: SeoMetaTags): SeoRulesResult[] {
        const result: SeoRulesResult[] = [];
        const ogDescription = metaTagsObject['og:description'];
        const description = metaTagsObject['description'];

        if (!ogDescription && description) {
            result.push({
                message: this.dotMessageService.get('seo.rules.description.not.found'),
                color: SEO_RULES_COLORS.ERROR,
                itemIcon: SEO_RULES_ICONS.TIMES
            });
        }

        if (ogDescription && ogDescription.length === 0) {
            result.push({
                message: this.dotMessageService.get('seo.rules.description.found.empty'),
                color: SEO_RULES_COLORS.ERROR,
                itemIcon: SEO_RULES_ICONS.TIMES
            });
        }

        if (ogDescription && ogDescription.length > 0) {
            result.push({
                message: this.dotMessageService.get('seo.rules.description.found'),
                color: SEO_RULES_COLORS.DONE,
                itemIcon: SEO_RULES_ICONS.CHECK
            });
        }

        return result;
    }

    private getTitleItems(metaTagsObject: SeoMetaTags): SeoRulesResult[] {
        const result: SeoRulesResult[] = [];
        const title = metaTagsObject['title'];
        const titleElements = metaTagsObject['titleElements'];

        if (!titleElements) {
            result.push({
                message: this.dotMessageService.get('seo.rules.title.not.found'),
                color: SEO_RULES_COLORS.ERROR,
                itemIcon: SEO_RULES_ICONS.TIMES
            });
        }

        if (titleElements?.length > 1) {
            result.push({
                message: this.dotMessageService.get('seo.rules.title.more.one.found'),
                color: SEO_RULES_COLORS.ERROR,
                itemIcon: SEO_RULES_ICONS.TIMES
            });
        }

        if (title?.length > SEO_LIMITS.MAX_TITLE_LENGTH) {
            result.push({
                message: this.dotMessageService.get('seo.rules.title.greater'),
                color: SEO_RULES_COLORS.WARNING,
                itemIcon: SEO_RULES_ICONS.EXCLAMATION_CIRCLE
            });
        }

        if (title?.length < SEO_LIMITS.MIN_TITLE_LENGTH) {
            result.push({
                message: this.dotMessageService.get('seo.rules.title.less'),
                color: SEO_RULES_COLORS.WARNING,
                itemIcon: SEO_RULES_ICONS.EXCLAMATION_CIRCLE
            });
        }

        if (
            titleElements?.length === SEO_LIMITS.MAX_TITLES &&
            title?.length < SEO_LIMITS.MAX_TITLE_LENGTH &&
            title?.length > SEO_LIMITS.MIN_TITLE_LENGTH
        ) {
            result.push({
                message: this.dotMessageService.get('seo.rules.title.found'),
                color: SEO_RULES_COLORS.DONE,
                itemIcon: SEO_RULES_ICONS.CHECK
            });
        }

        return result;
    }

    private getOgTitleItems(metaTagsObject: SeoMetaTags): SeoRulesResult[] {
        const result: SeoRulesResult[] = [];
        const titleOgElements = metaTagsObject['titleOgElements'];
        const titleOg = metaTagsObject['og:title'];

        if (!titleOgElements) {
            result.push({
                message: this.dotMessageService.get('seo.rules.image.not.found'),
                color: SEO_RULES_COLORS.ERROR,
                itemIcon: SEO_RULES_ICONS.TIMES
            });
        }

        if (titleOgElements?.length > 1) {
            result.push({
                message: this.dotMessageService.get('seo.rules.og-title.more.one.found'),
                color: SEO_RULES_COLORS.ERROR,
                itemIcon: SEO_RULES_ICONS.TIMES
            });
        }

        if (titleOg?.length < SEO_LIMITS.MIN_OG_TITLE_LENGTH) {
            result.push({
                message: this.dotMessageService.get('seo.rules.og-title.less'),
                color: SEO_RULES_COLORS.WARNING,
                itemIcon: SEO_RULES_ICONS.EXCLAMATION_CIRCLE
            });
        }

        if (titleOg?.length > SEO_LIMITS.MAX_OG_TITLE_LENGTH) {
            result.push({
                message: this.dotMessageService.get('seo.rules.og-title.greater'),
                color: SEO_RULES_COLORS.WARNING,
                itemIcon: SEO_RULES_ICONS.EXCLAMATION_CIRCLE
            });
        }

        if (
            titleOg &&
            titleOg?.length < SEO_LIMITS.MAX_OG_TITLE_LENGTH &&
            titleOg?.length > SEO_LIMITS.MIN_OG_TITLE_LENGTH
        ) {
            result.push({
                message: this.dotMessageService.get('seo.rules.og-image.found'),
                color: SEO_RULES_COLORS.DONE,
                itemIcon: SEO_RULES_ICONS.CHECK
            });
        }

        return result;
    }

    private getOgImagesItems(metaTagsObject: SeoMetaTags): Observable<SeoRulesResult[]> {
        const imageOgElements = metaTagsObject['imageOgElements'];
        const imageOg = metaTagsObject['og:image'];

        return this.getImageFileSize(imageOg).pipe(
            switchMap((imageMetaData) => {
                const result: SeoRulesResult[] = [];

                if (imageOg && imageMetaData.length <= SEO_LIMITS.MAX_IMAGE_BYTES) {
                    result.push({
                        message: this.dotMessageService.get('seo.rules.og-image.found'),
                        color: SEO_RULES_COLORS.DONE,
                        itemIcon: SEO_RULES_ICONS.CHECK
                    });
                }

                if (!imageOgElements) {
                    result.push({
                        message: this.dotMessageService.get('seo.rules.og-image.not.found'),
                        color: SEO_RULES_COLORS.ERROR,
                        itemIcon: SEO_RULES_ICONS.TIMES
                    });
                }

                if (imageOgElements?.length > 1) {
                    result.push({
                        message: this.dotMessageService.get('seo.rules.og-image.more.one.found'),
                        color: SEO_RULES_COLORS.ERROR,
                        itemIcon: SEO_RULES_ICONS.TIMES
                    });
                }

                return of(result);
            })
        );
    }

    /**
     * This uploads the image temporaly to get the file size, only if it is external
     * @param imageUrl string
     * @returns
     */
    getImageFileSize(imageUrl: string): Observable<DotCMSTempFile | ImageMetaData> {
        return from(
            fetch(imageUrl)
                .then((response) => response.blob())
                .then((blob) => {
                    return {
                        length: blob.size,
                        url: imageUrl
                    };
                })
                .catch((error) => {
                    console.warn(
                        'Getting the file size from an external URL failed, so we upload it to the server:',
                        error
                    );

                    return this.dotUploadService.uploadFile({ file: imageUrl });
                })
        );
    }
}
