import { forkJoin, Observable, of } from 'rxjs';

import { inject, Injectable } from '@angular/core';

import { map, switchMap } from 'rxjs/operators';

import {
    ImageMetaData,
    IMG_NOT_FOUND_KEY,
    OpenGraphOptions,
    SEO_LIMITS,
    SEO_MEDIA_TYPES,
    SEO_OPTIONS,
    SeoMediaKeys,
    SeoMetaTags,
    SeoMetaTagsResult,
    SeoRulesResult
} from '@dotcms/dotcms-models';

import { DotMessageService } from '../dot-messages/dot-messages.service';
import { DotSeoMetaTagsUtilService } from '../dot-seo-meta-tags-utils/dot-seo-meta-tags-util.service';

@Injectable()
export class DotSeoMetaTagsService {
    private dotMessageService = inject(DotMessageService);
    private dotSeoMetaTagsUtilService = inject(DotSeoMetaTagsUtilService);

    readMoreValues: Record<SEO_MEDIA_TYPES, string[]>;
    seoMedia: string;

    /**
     * Get the object with the SEO Result,
     * @param pageDocument
     * @returns
     */
    getMetaTagsResults(pageDocument: Document): Observable<SeoMetaTagsResult[]> {
        const metaTagsObject = this.dotSeoMetaTagsUtilService.getMetaTags(pageDocument);
        const ogMap = this.openGraphMap();

        const resolves = SeoMediaKeys.all.map((key) => ogMap[key]?.getItems(metaTagsObject));

        return forkJoin(resolves).pipe(
            map((resolve) => {
                return resolve.map((items, index) => {
                    const keysValues = this.dotSeoMetaTagsUtilService.getKeyValues(items);
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
                getItems: (metaTagsObject: SeoMetaTags) => this.getFaviconItems(metaTagsObject),
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

    private getFaviconItems(metaTagsObject: SeoMetaTags): Observable<SeoRulesResult[]> {
        const favicon = metaTagsObject['favicon'];
        const faviconElements = metaTagsObject['faviconElements'];

        return this.dotSeoMetaTagsUtilService.getImageFileSize(favicon).pipe(
            switchMap((imageMetaData: ImageMetaData) => {
                const items: SeoRulesResult[] = [];
                if (
                    (faviconElements.length <= SEO_LIMITS.MAX_FAVICONS &&
                        this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([favicon])) ||
                    imageMetaData?.url === IMG_NOT_FOUND_KEY
                ) {
                    items.push(
                        this.dotSeoMetaTagsUtilService.getErrorItem(
                            this.dotMessageService.get('seo.rules.favicon.not.found')
                        )
                    );
                }

                if (faviconElements.length > SEO_LIMITS.MAX_FAVICONS) {
                    items.push(
                        this.dotSeoMetaTagsUtilService.getErrorItem(
                            this.dotMessageService.get('seo.rules.favicon.more.one.found')
                        )
                    );
                }

                if (
                    favicon &&
                    faviconElements.length === SEO_LIMITS.MAX_FAVICONS &&
                    imageMetaData?.url !== IMG_NOT_FOUND_KEY
                ) {
                    items.push(
                        this.dotSeoMetaTagsUtilService.getDoneItem(
                            this.dotMessageService.get('seo.rules.favicon.found')
                        )
                    );
                }

                return of(items);
            })
        );
    }

    private getOgDescriptionItems(metaTagsObject: SeoMetaTags): SeoRulesResult[] {
        const result: SeoRulesResult[] = [];
        const ogDescription = metaTagsObject['og:description'];
        const description = metaTagsObject['description'];
        const descriptionElements = metaTagsObject['descriptionElements'];
        const descriptionOgElements = metaTagsObject['descriptionOgElements'];

        if (descriptionOgElements?.length > 1) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.og-description.more.one.found')
                )
            );
        }

        if (
            this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([
                ogDescription,
                descriptionOgElements,
                description,
                descriptionElements
            ])
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.og-description.description.not.found')
                )
            );
        }

        if (description && this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([ogDescription])) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.og-description.not.found')
                )
            );
        }

        if (
            descriptionOgElements?.length >= 1 &&
            this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([ogDescription])
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.og-description.found.empty')
                )
            );
        }

        if (ogDescription?.length < SEO_LIMITS.MIN_OG_DESCRIPTION_LENGTH) {
            result.push(
                this.dotSeoMetaTagsUtilService.getWarningItem(
                    this.dotMessageService.get('seo.rules.og-description.less')
                )
            );
        }

        if (ogDescription?.length > SEO_LIMITS.MAX_OG_DESCRIPTION_LENGTH) {
            result.push(
                this.dotSeoMetaTagsUtilService.getWarningItem(
                    this.dotMessageService.get('seo.rules.og-description.greater')
                )
            );
        }

        if (
            ogDescription &&
            ogDescription?.length >= SEO_LIMITS.MIN_OG_DESCRIPTION_LENGTH &&
            ogDescription?.length <= SEO_LIMITS.MAX_OG_DESCRIPTION_LENGTH
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getDoneItem(
                    this.dotMessageService.get('seo.rules.og-description.found')
                )
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
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.description.more.one.found')
                )
            );
        }

        if (this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([description, descriptionElements])) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.description.not.found')
                )
            );
        }

        if (
            descriptionElements.length >= 1 &&
            this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([description])
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.description.found.empty')
                )
            );
        }

        if (description?.length < SEO_LIMITS.MIN_OG_DESCRIPTION_LENGTH) {
            result.push(
                this.dotSeoMetaTagsUtilService.getWarningItem(
                    this.dotMessageService.get('seo.rules.description.less')
                )
            );
        }

        if (description?.length > SEO_LIMITS.MAX_OG_DESCRIPTION_LENGTH) {
            result.push(
                this.dotSeoMetaTagsUtilService.getWarningItem(
                    this.dotMessageService.get('seo.rules.description.greater')
                )
            );
        }

        if (
            description &&
            description?.length >= SEO_LIMITS.MIN_OG_DESCRIPTION_LENGTH &&
            description?.length <= SEO_LIMITS.MAX_OG_DESCRIPTION_LENGTH
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getDoneItem(
                    this.dotMessageService.get('seo.rules.description.found')
                )
            );
        }

        return result;
    }

    private getTitleItems(metaTagsObject: SeoMetaTags): SeoRulesResult[] {
        const result: SeoRulesResult[] = [];
        const title = metaTagsObject['title'];
        const titleElements = metaTagsObject['titleElements'];

        if (this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([title, titleElements])) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.title.not.found')
                )
            );
        }

        if (
            titleElements?.length >= 1 &&
            this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([title])
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.title.found.empty')
                )
            );
        }

        if (titleElements?.length > 1) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.title.more.one.found')
                )
            );
        }

        if (title?.length > SEO_LIMITS.MAX_OG_TITLE_LENGTH) {
            result.push(
                this.dotSeoMetaTagsUtilService.getWarningItem(
                    this.dotMessageService.get('seo.rules.title.greater')
                )
            );
        }

        if (title?.length < SEO_LIMITS.MIN_OG_TITLE_LENGTH) {
            result.push(
                this.dotSeoMetaTagsUtilService.getWarningItem(
                    this.dotMessageService.get('seo.rules.title.less')
                )
            );
        }

        if (
            titleElements?.length === SEO_LIMITS.MAX_TITLES &&
            title?.length <= SEO_LIMITS.MAX_OG_TITLE_LENGTH &&
            title?.length >= SEO_LIMITS.MIN_OG_TITLE_LENGTH
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getDoneItem(
                    this.dotMessageService.get('seo.rules.title.found')
                )
            );
        }

        return result;
    }

    private getOgTitleItems(metaTagsObject: SeoMetaTags): SeoRulesResult[] {
        const result: SeoRulesResult[] = [];
        const titleOgElements = metaTagsObject['titleOgElements'];
        const titleElements = metaTagsObject['titleElements'];
        const titleOg = metaTagsObject['og:title'];
        const title = metaTagsObject['title'];

        if (
            title &&
            this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([titleOg, titleOgElements])
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.og-title.not.found')
                )
            );
        }

        if (
            this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([
                title,
                titleOg,
                titleElements,
                titleOgElements
            ])
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.og-title.not.found.title')
                )
            );
        }

        if (
            titleOgElements?.length >= 1 &&
            this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([titleOg])
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.og-title.found.empty')
                )
            );
        }

        if (titleOgElements?.length > 1) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.og-title.more.one.found')
                )
            );
        }

        if (titleOg?.length < SEO_LIMITS.MIN_OG_TITLE_LENGTH) {
            result.push(
                this.dotSeoMetaTagsUtilService.getWarningItem(
                    this.dotMessageService.get('seo.rules.og-title.less')
                )
            );
        }

        if (titleOg?.length > SEO_LIMITS.MAX_OG_TITLE_LENGTH) {
            result.push(
                this.dotSeoMetaTagsUtilService.getWarningItem(
                    this.dotMessageService.get('seo.rules.og-title.greater')
                )
            );
        }

        if (
            titleOg &&
            titleOg?.length <= SEO_LIMITS.MAX_OG_TITLE_LENGTH &&
            titleOg?.length >= SEO_LIMITS.MIN_OG_TITLE_LENGTH
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getDoneItem(
                    this.dotMessageService.get('seo.rules.og-title.found')
                )
            );
        }

        return result;
    }

    private getOgImagesItems(metaTagsObject: SeoMetaTags): Observable<SeoRulesResult[]> {
        const imageOgElements = metaTagsObject['imageOgElements'];
        const imageOg = metaTagsObject['og:image'];

        return this.dotSeoMetaTagsUtilService.getImageFileSize(imageOg).pipe(
            switchMap((imageMetaData: ImageMetaData) => {
                const result: SeoRulesResult[] = [];

                if (
                    imageMetaData?.url !== IMG_NOT_FOUND_KEY &&
                    imageMetaData.length <= SEO_LIMITS.MAX_IMAGE_BYTES
                ) {
                    result.push(
                        this.dotSeoMetaTagsUtilService.getDoneItem(
                            this.dotMessageService.get('seo.rules.og-image.found')
                        )
                    );
                }

                if (
                    imageMetaData?.url === IMG_NOT_FOUND_KEY ||
                    this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([imageOgElements, imageOg])
                ) {
                    result.push(
                        this.dotSeoMetaTagsUtilService.getErrorItem(
                            this.dotMessageService.get('seo.rules.og-image.not.found')
                        )
                    );
                }

                if (
                    imageOgElements?.length >= 1 &&
                    this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([imageOg])
                ) {
                    result.push(
                        this.dotSeoMetaTagsUtilService.getErrorItem(
                            this.dotMessageService.get('seo.rules.og-image.more.one.found.empty')
                        )
                    );
                }

                if (imageOgElements?.length > 1) {
                    result.push(
                        this.dotSeoMetaTagsUtilService.getErrorItem(
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

        if (this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([twitterCard, twitterCardElements])) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.twitter-card.not.found')
                )
            );
        }

        if (
            twitterCardElements.length >= 1 &&
            this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([twitterCard])
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.twitter-card.more.one.found.empty')
                )
            );
        }

        if (twitterCardElements?.length > 1) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.twitter-card.more.one.found')
                )
            );
        }

        if (twitterCard) {
            result.push(
                this.dotSeoMetaTagsUtilService.getDoneItem(
                    this.dotMessageService.get('seo.rules.twitter-card.found')
                )
            );
        }

        return result;
    }

    private getTwitterTitleItems(metaTagsObject: SeoMetaTags): SeoRulesResult[] {
        const result: SeoRulesResult[] = [];
        const titleCardElements = metaTagsObject['twitterTitleElements'];
        const titleCard = metaTagsObject['twitter:title'];
        const titleOg = metaTagsObject['og:title'];
        const titleOgElements = metaTagsObject['titleOgElements'];
        const title = metaTagsObject['title'];
        const titleElements = metaTagsObject['titleElements'];

        if (
            (title || titleOg) &&
            this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([titleCard, titleCardElements])
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.twitter-card-title.not.found')
                )
            );
        }

        if (
            this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([
                title,
                titleCard,
                titleElements,
                titleCardElements,
                titleOgElements,
                titleOg
            ])
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.twitter-card-title.title.not.found')
                )
            );
        }

        if (titleCardElements?.length > 1) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.twitter-card-title.more.one.found')
                )
            );
        }

        if (
            titleCardElements.length >= 1 &&
            this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([titleCard])
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.twitter-card-title.found.empty')
                )
            );
        }

        if (titleCard?.length < SEO_LIMITS.MIN_TWITTER_TITLE_LENGTH) {
            result.push(
                this.dotSeoMetaTagsUtilService.getWarningItem(
                    this.dotMessageService.get('seo.rules.twitter-card.title.less')
                )
            );
        }

        if (titleCard?.length > SEO_LIMITS.MAX_TWITTER_TITLE_LENGTH) {
            result.push(
                this.dotSeoMetaTagsUtilService.getWarningItem(
                    this.dotMessageService.get('seo.rules.twitter-card.title.greater')
                )
            );
        }

        if (
            titleCard &&
            titleCard?.length >= SEO_LIMITS.MIN_TWITTER_TITLE_LENGTH &&
            titleCard?.length <= SEO_LIMITS.MAX_TWITTER_TITLE_LENGTH
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getDoneItem(
                    this.dotMessageService.get('seo.rules.twitter-card-title.found')
                )
            );
        }

        return result;
    }

    private getTwitterDescriptionItems(metaTagsObject: SeoMetaTags): SeoRulesResult[] {
        const result: SeoRulesResult[] = [];
        const twitterDescriptionElements = metaTagsObject['twitterDescriptionElements'];
        const twitterDescription = metaTagsObject['twitter:description'];
        const ogDescriptionElements = metaTagsObject['descriptionOgElements'];
        const ogDescription = metaTagsObject['og:description'];
        const descriptionElements = metaTagsObject['descriptionElements'];
        const description = metaTagsObject['description'];

        if (
            (description || ogDescription) &&
            this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([
                twitterDescription,
                twitterDescriptionElements
            ])
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.twitter-card-description.not.found')
                )
            );
        }

        if (
            this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([
                twitterDescription,
                twitterDescriptionElements,
                ogDescriptionElements,
                descriptionElements,
                ogDescription,
                description
            ])
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get(
                        'seo.rules.twitter-card-description.description.not.found'
                    )
                )
            );
        }

        if (twitterDescriptionElements.length > 1) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
                    this.dotMessageService.get('seo.rules.twitter-card-description.more.one.found')
                )
            );
        }

        if (
            twitterDescriptionElements.length >= 1 &&
            this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([twitterDescription])
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getErrorItem(
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
                this.dotSeoMetaTagsUtilService.getWarningItem(
                    this.dotMessageService.get('seo.rules.twitter-card-description.greater')
                )
            );
        }

        if (
            twitterDescription &&
            twitterDescription.length < SEO_LIMITS.MIN_TWITTER_DESCRIPTION_LENGTH
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getWarningItem(
                    this.dotMessageService.get('seo.rules.twitter-card-description.less')
                )
            );
        }

        if (
            twitterDescription &&
            twitterDescription?.length >= SEO_LIMITS.MIN_TWITTER_DESCRIPTION_LENGTH &&
            twitterDescription?.length <= SEO_LIMITS.MAX_TWITTER_DESCRIPTION_LENGTH
        ) {
            result.push(
                this.dotSeoMetaTagsUtilService.getDoneItem(
                    this.dotMessageService.get('seo.rules.twitter-card-description.found')
                )
            );
        }

        return result;
    }

    private getTwitterImageItems(metaTagsObject: SeoMetaTags): Observable<SeoRulesResult[]> {
        const twitterImageElements = metaTagsObject['twitterImageElements'];
        const twitterImage = metaTagsObject['twitter:image'];

        return this.dotSeoMetaTagsUtilService.getImageFileSize(twitterImage).pipe(
            switchMap((imageMetaData: ImageMetaData) => {
                const result: SeoRulesResult[] = [];
                if (
                    imageMetaData?.url !== IMG_NOT_FOUND_KEY &&
                    imageMetaData.length <= SEO_LIMITS.MAX_IMAGE_BYTES
                ) {
                    result.push(
                        this.dotSeoMetaTagsUtilService.getDoneItem(
                            this.dotMessageService.get('seo.rules.twitter-image.found')
                        )
                    );
                }

                if (
                    imageMetaData?.url === IMG_NOT_FOUND_KEY ||
                    this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([
                        twitterImage,
                        twitterImageElements
                    ])
                ) {
                    result.push(
                        this.dotSeoMetaTagsUtilService.getErrorItem(
                            this.dotMessageService.get('seo.rules.twitter-image.not.found')
                        )
                    );
                }

                if (
                    twitterImageElements?.length >= 1 &&
                    this.dotSeoMetaTagsUtilService.areAllFalsyOrEmpty([twitterImage])
                ) {
                    result.push(
                        this.dotSeoMetaTagsUtilService.getErrorItem(
                            this.dotMessageService.get(
                                'seo.rules.twitter-image.more.one.found.empty'
                            )
                        )
                    );
                }

                if (twitterImageElements?.length > 1) {
                    result.push(
                        this.dotSeoMetaTagsUtilService.getErrorItem(
                            this.dotMessageService.get('seo.rules.twitter-image.more.one.found')
                        )
                    );
                }

                if (imageMetaData.length > SEO_LIMITS.MAX_TWITTER_IMAGE_BYTES) {
                    result.push(
                        this.dotSeoMetaTagsUtilService.getDoneItem(
                            this.dotMessageService.get('seo.rules.twitter-image.over')
                        )
                    );
                }

                return of(result);
            })
        );
    }
}
