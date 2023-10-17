import { Observable, forkJoin, from, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { catchError, map, mergeMap, switchMap } from 'rxjs/operators';

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
    OpenGraphOptions,
    SEO_TAGS,
    SEO_MEDIA_TYPES,
    IMG_NOT_FOUND_KEY
} from '../dot-edit-content-html/models/meta-tags-model';

@Injectable()
export class DotSeoMetaTagsService {
    readMoreValues: Record<SEO_MEDIA_TYPES, string[]>;
    seoMedia: string;

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
        const descriptionOgElements = pageDocument.querySelectorAll(
            'meta[property="og:description"]'
        );
        const descriptionElements = pageDocument.querySelectorAll('meta[name="description"]');
        const twitterCardElements = pageDocument.querySelectorAll('meta[name="twitter:card"]');
        const twitterTitleElements = pageDocument.querySelectorAll('meta[name="twitter:title"]');
        const twitterImageElements = pageDocument.querySelectorAll('meta[name="twitter:image"]');
        const twitterDescriptionElements = pageDocument.querySelectorAll(
            'meta[name="twitter:description"]'
        );

        metaTagsObject['faviconElements'] = favicon;
        metaTagsObject['titleElements'] = title;
        metaTagsObject['favicon'] = (favicon[0] as HTMLLinkElement)?.href;
        metaTagsObject['title'] = title[0]?.innerText;
        metaTagsObject['titleOgElements'] = titleOgElements;
        metaTagsObject['imageOgElements'] = imagesOgElements;
        metaTagsObject['twitterCardElements'] = twitterCardElements;
        metaTagsObject['twitterTitleElements'] = twitterTitleElements;
        metaTagsObject['twitterDescriptionElements'] = twitterDescriptionElements;
        metaTagsObject['twitterImageElements'] = twitterImageElements;
        metaTagsObject['descriptionOgElements'] = descriptionOgElements;
        metaTagsObject['descriptionElements'] = descriptionElements;

        return metaTagsObject;
    }

    /**
     * Get the object with the SEO Result,
     * @param pageDocument
     * @returns
     */
    getMetaTagsResults(pageDocument: Document): Observable<SeoMetaTagsResult[]> {
        const metaTagsObject = this.getMetaTags(pageDocument);
        const ogMap = this.openGraphMap();

        const resolves = SeoMediaKeys.all.map((key) => ogMap[key]?.getItems(metaTagsObject));

        return forkJoin(resolves).pipe(
            map((resolve) => {
                return resolve.map((items, index) => {
                    const keysValues = this.getKeyValues(items);
                    const key = SeoMediaKeys.all[index];

                    return {
                        key,
                        title: key.replace('og:', '').replace('twitter:', ''),
                        keyIcon: keysValues.keyIcon,
                        keyColor: keysValues.keyColor,
                        items: items,
                        sort: ogMap[key]?.sort
                    };
                });
            })
        );
    }
    /**
     * This returns the map of the open graph elements and their properties.
     */
    private openGraphMap(): Record<SEO_OPTIONS, OpenGraphOptions> {
        return {
            [SEO_OPTIONS.FAVICON]: {
                getItems: (metaTagsObject: SeoMetaTags) => of(this.getFaviconItems(metaTagsObject)),
                sort: 1
            },
            [SEO_OPTIONS.DESCRIPTION]: {
                getItems: (metaTagsObject: SeoMetaTags) =>
                    of(this.getDescriptionItems(metaTagsObject)),
                sort: 3
            },
            [SEO_OPTIONS.OG_DESCRIPTION]: {
                getItems: (metaTagsObject: SeoMetaTags) =>
                    of(this.getOgDescriptionItems(metaTagsObject)),
                sort: 4
            },
            [SEO_OPTIONS.TITLE]: {
                getItems: (metaTagsObject: SeoMetaTags) => of(this.getTitleItems(metaTagsObject)),
                sort: 2
            },
            [SEO_OPTIONS.OG_TITLE]: {
                getItems: (metaTagsObject: SeoMetaTags) => of(this.getOgTitleItems(metaTagsObject)),
                sort: 2
            },
            [SEO_OPTIONS.OG_IMAGE]: {
                getItems: (metaTagsObject: SeoMetaTags) => this.getOgImagesItems(metaTagsObject),
                sort: 6
            },
            [SEO_OPTIONS.TWITTER_CARD]: {
                getItems: (metaTagsObject: SeoMetaTags) =>
                    of(this.getTwitterCardItems(metaTagsObject)),
                sort: 2
            },
            [SEO_OPTIONS.TWITTER_TITLE]: {
                getItems: (metaTagsObject: SeoMetaTags) =>
                    of(this.getTwitterTitleItems(metaTagsObject)),
                sort: 1
            },
            [SEO_OPTIONS.TWITTER_DESCRIPTION]: {
                getItems: (metaTagsObject: SeoMetaTags) =>
                    of(this.getTwitterDescriptionItems(metaTagsObject)),
                sort: 3
            },
            [SEO_OPTIONS.TWITTER_IMAGE]: {
                getItems: (metaTagsObject: SeoMetaTags) =>
                    this.getTwitterImageItems(metaTagsObject),
                sort: 4
            }
        };
    }

    /**
     * Filter the results by the seoMedia
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

    private getFaviconItems(metaTagsObject: SeoMetaTags): SeoRulesResult[] {
        const items: SeoRulesResult[] = [];
        const favicon = metaTagsObject['favicon'];
        const faviconElements = metaTagsObject['faviconElements'];

        if (faviconElements.length === 0) {
            items.push(
                this.getErrorItem(this.dotMessageService.get('seo.rules.favicon.not.found'))
            );
        }

        if (faviconElements.length > SEO_LIMITS.MAX_FAVICONS) {
            items.push(
                this.getErrorItem(this.dotMessageService.get('seo.rules.favicon.more.one.found'))
            );
        }

        if (favicon && faviconElements.length === SEO_LIMITS.MAX_FAVICONS) {
            items.push(this.getDoneItem(this.dotMessageService.get('seo.rules.favicon.found')));
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

    private getOgDescriptionItems(metaTagsObject: SeoMetaTags): SeoRulesResult[] {
        const result: SeoRulesResult[] = [];
        const ogDescription = metaTagsObject['og:description'];
        const description = metaTagsObject['description'];
        const descriptionElements = metaTagsObject['descriptionElements'];
        const descriptionOgElements = metaTagsObject['descriptionOgElements'];

        if (descriptionOgElements?.length > 1) {
            result.push(
                this.getErrorItem(
                    this.dotMessageService.get('seo.rules.og-description.more.one.found')
                )
            );
        }

        if (
            this.areAllFalsyOrEmpty([
                ogDescription,
                descriptionOgElements,
                description,
                descriptionElements
            ])
        ) {
            result.push(
                this.getErrorItem(
                    this.dotMessageService.get('seo.rules.og-description.description.not.found')
                )
            );
        }

        if (description && this.areAllFalsyOrEmpty([ogDescription])) {
            result.push(
                this.getErrorItem(this.dotMessageService.get('seo.rules.og-description.not.found'))
            );
        }

        if (descriptionOgElements?.length >= 1 && this.areAllFalsyOrEmpty([ogDescription])) {
            result.push(
                this.getErrorItem(
                    this.dotMessageService.get('seo.rules.og-description.found.empty')
                )
            );
        }

        if (ogDescription?.length < SEO_LIMITS.MIN_OG_DESCRIPTION_LENGTH) {
            result.push(
                this.getWarningItem(this.dotMessageService.get('seo.rules.og-description.less'))
            );
        }

        if (ogDescription?.length > SEO_LIMITS.MAX_OG_DESCRIPTION_LENGTH) {
            result.push(
                this.getWarningItem(this.dotMessageService.get('seo.rules.og-description.greater'))
            );
        }

        if (
            ogDescription &&
            ogDescription?.length > SEO_LIMITS.MIN_OG_DESCRIPTION_LENGTH &&
            ogDescription?.length < SEO_LIMITS.MAX_OG_DESCRIPTION_LENGTH
        ) {
            result.push(
                this.getDoneItem(this.dotMessageService.get('seo.rules.og-description.found'))
            );
        }

        return result;
    }

    private getDescriptionItems(metaTagsObject: SeoMetaTags): SeoRulesResult[] {
        const result: SeoRulesResult[] = [];
        const description = metaTagsObject['description'];
        const descriptionElements = metaTagsObject['descriptionElements'];

        if (descriptionElements?.length > 1) {
            result.push(
                this.getErrorItem(
                    this.dotMessageService.get('seo.rules.description.more.one.found')
                )
            );
        }

        if (this.areAllFalsyOrEmpty([description, descriptionElements])) {
            result.push(
                this.getErrorItem(this.dotMessageService.get('seo.rules.description.not.found'))
            );
        }

        if (descriptionElements.length >= 1 && this.areAllFalsyOrEmpty([description])) {
            result.push(
                this.getErrorItem(this.dotMessageService.get('seo.rules.description.found.empty'))
            );
        }

        if (description?.length < SEO_LIMITS.MIN_DESCRIPTION_LENGTH) {
            result.push(
                this.getWarningItem(this.dotMessageService.get('seo.rules.description.less'))
            );
        }

        if (description?.length > SEO_LIMITS.MAX_DESCRIPTION_LENGTH) {
            result.push(
                this.getWarningItem(this.dotMessageService.get('seo.rules.description.greater'))
            );
        }

        if (
            description &&
            description?.length > SEO_LIMITS.MIN_OG_DESCRIPTION_LENGTH &&
            description?.length < SEO_LIMITS.MAX_OG_DESCRIPTION_LENGTH
        ) {
            result.push(
                this.getDoneItem(this.dotMessageService.get('seo.rules.description.found'))
            );
        }

        return result;
    }

    private getTitleItems(metaTagsObject: SeoMetaTags): SeoRulesResult[] {
        const result: SeoRulesResult[] = [];
        const title = metaTagsObject['title'];
        const titleElements = metaTagsObject['titleElements'];

        if (this.areAllFalsyOrEmpty([title, titleElements])) {
            result.push(this.getErrorItem(this.dotMessageService.get('seo.rules.title.not.found')));
        }

        if (titleElements?.length >= 1 && this.areAllFalsyOrEmpty([title])) {
            result.push(
                this.getErrorItem(this.dotMessageService.get('seo.rules.title.found.empty'))
            );
        }

        if (titleElements?.length > 1) {
            result.push(
                this.getErrorItem(this.dotMessageService.get('seo.rules.title.more.one.found'))
            );
        }

        if (title?.length > SEO_LIMITS.MAX_TITLE_LENGTH) {
            result.push(this.getWarningItem(this.dotMessageService.get('seo.rules.title.greater')));
        }

        if (title?.length < SEO_LIMITS.MIN_TITLE_LENGTH) {
            result.push(this.getWarningItem(this.dotMessageService.get('seo.rules.title.less')));
        }

        if (
            titleElements?.length === SEO_LIMITS.MAX_TITLES &&
            title?.length < SEO_LIMITS.MAX_TITLE_LENGTH &&
            title?.length > SEO_LIMITS.MIN_TITLE_LENGTH
        ) {
            result.push(this.getDoneItem(this.dotMessageService.get('seo.rules.title.found')));
        }

        return result;
    }

    private getOgTitleItems(metaTagsObject: SeoMetaTags): SeoRulesResult[] {
        const result: SeoRulesResult[] = [];
        const titleOgElements = metaTagsObject['titleOgElements'];
        const titleElements = metaTagsObject['titleElements'];
        const titleOg = metaTagsObject['og:title'];
        const title = metaTagsObject['title'];

        if (title && this.areAllFalsyOrEmpty([titleOg, titleOgElements])) {
            result.push(
                this.getErrorItem(this.dotMessageService.get('seo.rules.og-title.not.found'))
            );
        }

        if (this.areAllFalsyOrEmpty([title, titleOg, titleElements, titleOgElements])) {
            result.push(
                this.getErrorItem(this.dotMessageService.get('seo.rules.og-title.not.found.title'))
            );
        }

        if (titleOgElements?.length >= 1 && this.areAllFalsyOrEmpty([titleOg])) {
            result.push(
                this.getErrorItem(this.dotMessageService.get('seo.rules.og-title.found.empty'))
            );
        }

        if (titleOgElements?.length > 1) {
            result.push(
                this.getErrorItem(this.dotMessageService.get('seo.rules.og-title.more.one.found'))
            );
        }

        if (titleOg?.length < SEO_LIMITS.MIN_OG_TITLE_LENGTH) {
            result.push(this.getWarningItem(this.dotMessageService.get('seo.rules.og-title.less')));
        }

        if (titleOg?.length > SEO_LIMITS.MAX_OG_TITLE_LENGTH) {
            result.push(
                this.getWarningItem(this.dotMessageService.get('seo.rules.og-title.greater'))
            );
        }

        if (
            titleOg &&
            titleOg?.length < SEO_LIMITS.MAX_OG_TITLE_LENGTH &&
            titleOg?.length > SEO_LIMITS.MIN_OG_TITLE_LENGTH
        ) {
            result.push(this.getDoneItem(this.dotMessageService.get('seo.rules.og-title.found')));
        }

        return result;
    }

    private getOgImagesItems(metaTagsObject: SeoMetaTags): Observable<SeoRulesResult[]> {
        const imageOgElements = metaTagsObject['imageOgElements'];
        const imageOg = metaTagsObject['og:image'];

        return this.getImageFileSize(imageOg).pipe(
            switchMap((imageMetaData: ImageMetaData) => {
                const result: SeoRulesResult[] = [];

                if (
                    imageMetaData?.url !== IMG_NOT_FOUND_KEY &&
                    imageMetaData.length <= SEO_LIMITS.MAX_IMAGE_BYTES
                ) {
                    result.push(
                        this.getDoneItem(this.dotMessageService.get('seo.rules.og-image.found'))
                    );
                }

                if (
                    imageMetaData?.url === IMG_NOT_FOUND_KEY ||
                    this.areAllFalsyOrEmpty([imageOgElements, imageOg])
                ) {
                    result.push(
                        this.getErrorItem(
                            this.dotMessageService.get('seo.rules.og-image.not.found')
                        )
                    );
                }

                if (imageOgElements?.length >= 1 && this.areAllFalsyOrEmpty([imageOg])) {
                    result.push(
                        this.getErrorItem(
                            this.dotMessageService.get('seo.rules.og-image.more.one.found.empty')
                        )
                    );
                }

                if (imageOgElements?.length > 1) {
                    result.push(
                        this.getErrorItem(
                            this.dotMessageService.get('seo.rules.og-image.more.one.found')
                        )
                    );
                }

                return of(result);
            })
        );
    }

    private getTwitterCardItems(metaTagsObject: SeoMetaTags): SeoRulesResult[] {
        const result: SeoRulesResult[] = [];
        const twitterCardElements = metaTagsObject['twitterCardElements'];
        const twitterCard = metaTagsObject['twitter:card'];

        if (this.areAllFalsyOrEmpty([twitterCard, twitterCardElements])) {
            result.push(
                this.getErrorItem(this.dotMessageService.get('seo.rules.twitter-card.not.found'))
            );
        }

        if (twitterCardElements.length >= 1 && this.areAllFalsyOrEmpty([twitterCard])) {
            result.push(
                this.getErrorItem(
                    this.dotMessageService.get('seo.rules.twitter-card.more.one.found.empty')
                )
            );
        }

        if (twitterCardElements?.length > 1) {
            result.push(
                this.getErrorItem(
                    this.dotMessageService.get('seo.rules.twitter-card.more.one.found')
                )
            );
        }

        if (twitterCard) {
            result.push(
                this.getDoneItem(this.dotMessageService.get('seo.rules.twitter-card.found'))
            );
        }

        return result;
    }

    private getTwitterTitleItems(metaTagsObject: SeoMetaTags): SeoRulesResult[] {
        const result: SeoRulesResult[] = [];
        const titleCardElements = metaTagsObject['twitterTitleElements'];
        const titleCard = metaTagsObject['twitter:title'];

        if (this.areAllFalsyOrEmpty([titleCard, titleCardElements])) {
            result.push(
                this.getErrorItem(
                    this.dotMessageService.get('seo.rules.twitter-card-title.not.found')
                )
            );
        }

        if (titleCardElements?.length > 1) {
            result.push(
                this.getErrorItem(
                    this.dotMessageService.get('seo.rules.twitter-card-title.more.one.found')
                )
            );
        }

        if (titleCardElements.length >= 1 && this.areAllFalsyOrEmpty([titleCard])) {
            result.push(
                this.getErrorItem(
                    this.dotMessageService.get('seo.rules.twitter-card-title.found.empty')
                )
            );
        }

        if (titleCard?.length < SEO_LIMITS.MIN_TWITTER_TITLE_LENGTH) {
            result.push(
                this.getWarningItem(this.dotMessageService.get('seo.rules.twitter-card.title.less'))
            );
        }

        if (titleCard?.length > SEO_LIMITS.MAX_TWITTER_TITLE_LENGTH) {
            result.push(
                this.getWarningItem(
                    this.dotMessageService.get('seo.rules.twitter-card.title.greater')
                )
            );
        }

        if (
            titleCard &&
            titleCard?.length > SEO_LIMITS.MIN_TWITTER_TITLE_LENGTH &&
            titleCard?.length < SEO_LIMITS.MAX_TWITTER_TITLE_LENGTH
        ) {
            result.push(
                this.getDoneItem(this.dotMessageService.get('seo.rules.twitter-card-title.found'))
            );
        }

        return result;
    }

    private getTwitterDescriptionItems(metaTagsObject: SeoMetaTags): SeoRulesResult[] {
        const result: SeoRulesResult[] = [];
        const twitterDescriptionElements = metaTagsObject['twitterDescriptionElements'];
        const twitterDescription = metaTagsObject['twitter:description'];

        if (this.areAllFalsyOrEmpty([twitterDescription, twitterDescriptionElements])) {
            result.push(
                this.getErrorItem(
                    this.dotMessageService.get('seo.rules.twitter-card-description.not.found')
                )
            );
        }

        if (twitterDescriptionElements.length > 1) {
            result.push(
                this.getErrorItem(
                    this.dotMessageService.get('seo.rules.twitter-card-description.more.one.found')
                )
            );
        }

        if (
            twitterDescriptionElements.length >= 1 &&
            this.areAllFalsyOrEmpty([twitterDescription])
        ) {
            result.push(
                this.getErrorItem(
                    this.dotMessageService.get(
                        'seo.rules.twitter-card-description.more.one.found.empty'
                    )
                )
            );
        }

        if (
            twitterDescription &&
            twitterDescription.length > SEO_LIMITS.MAX_TWITTER_DESCRIPTION_LENGTH
        ) {
            result.push(
                this.getWarningItem(
                    this.dotMessageService.get('seo.rules.twitter-card-description.greater')
                )
            );
        }

        if (
            twitterDescription &&
            twitterDescription.length < SEO_LIMITS.MAX_TWITTER_DESCRIPTION_LENGTH
        ) {
            result.push(
                this.getDoneItem(
                    this.dotMessageService.get('seo.rules.twitter-card-description.found')
                )
            );
        }

        return result;
    }

    private getTwitterImageItems(metaTagsObject: SeoMetaTags): Observable<SeoRulesResult[]> {
        const twitterImageElements = metaTagsObject['twitterImageElements'];
        const twitterImage = metaTagsObject['twitter:image'];

        return this.getImageFileSize(twitterImage).pipe(
            switchMap((imageMetaData) => {
                const result: SeoRulesResult[] = [];

                if (twitterImage && imageMetaData.length <= SEO_LIMITS.MAX_IMAGE_BYTES) {
                    result.push(
                        this.getDoneItem(
                            this.dotMessageService.get('seo.rules.twitter-image.found')
                        )
                    );
                }

                if (this.areAllFalsyOrEmpty([twitterImage, twitterImageElements])) {
                    result.push(
                        this.getErrorItem(
                            this.dotMessageService.get('seo.rules.twitter-image.not.found')
                        )
                    );
                }

                if (twitterImageElements?.length > 1) {
                    result.push(
                        this.getErrorItem(
                            this.dotMessageService.get('seo.rules.twitter-image.more.one.found')
                        )
                    );
                }

                if (imageMetaData.length > SEO_LIMITS.MAX_TWITTER_IMAGE_BYTES) {
                    result.push(
                        this.getDoneItem(this.dotMessageService.get('seo.rules.twitter-image.over'))
                    );
                }

                return of(result);
            })
        );
    }

    private areAllFalsyOrEmpty(values: (string | NodeListOf<Element>)[]): boolean {
        return values.every(
            (value) => value === null || value === undefined || value?.length === 0
        );
    }

    private getErrorItem(message: string): SeoRulesResult {
        return {
            message: this.addHTMLTag(message),
            color: SEO_RULES_COLORS.ERROR,
            itemIcon: SEO_RULES_ICONS.TIMES
        };
    }

    private getWarningItem(message: string): SeoRulesResult {
        return {
            message: this.addHTMLTag(message),
            color: SEO_RULES_COLORS.WARNING,
            itemIcon: SEO_RULES_ICONS.EXCLAMATION_CIRCLE
        };
    }

    private getDoneItem(message: string): SeoRulesResult {
        return {
            message: this.addHTMLTag(message),
            color: SEO_RULES_COLORS.DONE,
            itemIcon: SEO_RULES_ICONS.CHECK
        };
    }

    private addHTMLTag(message: string): string {
        const regexPattern = new RegExp(SEO_TAGS.map((option) => `\\b${option}\\b`).join('|'), 'g');

        return message.replace(regexPattern, '<code>$&</code>');
    }

    public getReadMore(): Record<SEO_MEDIA_TYPES, string[]> {
        return {
            [SEO_MEDIA_TYPES.FACEBOOK]: [
                this.dotMessageService.get('seo.rules.read-more.facebook.learn'),
                this.dotMessageService.get('seo.rules.read-more.facebook.sharing'),
                this.dotMessageService.get('seo.rules.read-more.facebook.title'),
                this.dotMessageService.get('seo.rules.read-more.facebook.title.unique'),
                this.dotMessageService.get('seo.rules.read-more.facebook.title.sizes'),
                this.dotMessageService.get('seo.rules.read-more.facebook.og-image'),
                this.dotMessageService.get('seo.rules.read-more.facebook.social')
            ],
            [SEO_MEDIA_TYPES.TWITTER]: [
                this.dotMessageService.get('seo.rules.read-more.twitter.learn'),
                this.dotMessageService.get('seo.rules.read-more.twitter.suggest'),
                this.dotMessageService.get('seo.rules.read-more.twitter.twitter-title'),
                this.dotMessageService.get('seo.rules.read-more.twitter.twitter-title.content'),
                this.dotMessageService.get('seo.rules.read-more.twitter.length'),
                this.dotMessageService.get('seo.rules.read-more.twitter.twitter-image'),
                this.dotMessageService.get('seo.rules.read-more.twitter.twitter-image.aspect'),
                this.dotMessageService.get('seo.rules.read-more.twitter.twitter-image.content'),
                this.dotMessageService.get('seo.rules.read-more.twitter.twitter-image.social')
            ],
            [SEO_MEDIA_TYPES.LINKEDIN]: [
                this.dotMessageService.get('seo.rules.read-more.linkedin.learn'),
                this.dotMessageService.get('seo.rules.read-more.linkedin.meta'),
                this.dotMessageService.get('seo.rules.read-more.linkedin.summary')
            ],
            [SEO_MEDIA_TYPES.GOOGLE]: [
                this.dotMessageService.get('seo.rules.read-more.google.favicons'),
                this.dotMessageService.get('seo.rules.read-more.google.title'),
                this.dotMessageService.get('seo.rules.read-more.google.title.unique'),
                this.dotMessageService.get('seo.rules.read-more.google.description'),
                this.dotMessageService.get('seo.rules.read-more.google.length'),
                this.dotMessageService.get('seo.rules.read-more.google.meta-tags'),
                this.dotMessageService.get('seo.rules.read-more.google.meta-description'),
                this.dotMessageService.get('seo.rules.read-more.google.image-sizes')
            ]
        };
    }

    /**
     * This uploads the image temporaly to get the file size, only if it is external.
     * Checks if the imageUrl has been sent.
     * @param imageUrl string
     * @returns
     */
    getImageFileSize(imageUrl: string): Observable<DotCMSTempFile | ImageMetaData> {
        if (!imageUrl) {
            return of({
                length: 0,
                url: IMG_NOT_FOUND_KEY
            });
        }

        return from(fetch(imageUrl)).pipe(
            mergeMap((response) => {
                if (response.status === 404) {
                    return of({
                        size: 0,
                        url: IMG_NOT_FOUND_KEY
                    });
                }

                return response.clone().blob();
            }),
            mergeMap((response) => {
                return of({
                    length: response.size,
                    url: imageUrl
                });
            }),
            catchError(() => {
                return from(this.dotUploadService.uploadFile({ file: imageUrl })).pipe(
                    mergeMap((uploadedFile) => {
                        if (uploadedFile) {
                            return of(uploadedFile);
                        }
                    }),
                    catchError((uploadError) => {
                        console.warn('Error while uploading:', uploadError);

                        return of({
                            length: 0,
                            url: IMG_NOT_FOUND_KEY
                        });
                    })
                );
            })
        );
    }
}
