import { Observable, from, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { catchError, map, switchMap } from 'rxjs/operators';

import { DotMessageService, DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';

import {
    IMG_NOT_FOUND_KEY,
    ImageMetaData,
    SEO_MEDIA_TYPES,
    SEO_RULES_COLORS,
    SEO_RULES_ICONS,
    SEO_TAGS,
    SeoKeyResult,
    SeoMediaKeys,
    SeoMetaTagsResult,
    SeoRulesResult
} from '../dot-edit-content-html/models/meta-tags-model';

@Injectable()
export class DotSeoMetaTagsUtilService {
    constructor(
        private dotMessageService: DotMessageService,
        private dotUploadService: DotUploadService
    ) {}

    /**
     * Getthe error item
     * @param message
     * @returns
     */

    getErrorItem(message: string): SeoRulesResult {
        return {
            message: this.addHTMLTag(message),
            color: SEO_RULES_COLORS.ERROR,
            itemIcon: SEO_RULES_ICONS.TIMES
        };
    }

    /**
     * Get the warning item
     * @param message
     * @returns
     */
    getWarningItem(message: string): SeoRulesResult {
        return {
            message: this.addHTMLTag(message),
            color: SEO_RULES_COLORS.WARNING,
            itemIcon: SEO_RULES_ICONS.EXCLAMATION_CIRCLE
        };
    }

    /**
     * Get the done item
     * @param message
     * @returns
     */

    getDoneItem(message: string): SeoRulesResult {
        return {
            message: this.addHTMLTag(message),
            color: SEO_RULES_COLORS.DONE,
            itemIcon: SEO_RULES_ICONS.CHECK
        };
    }

    /**
     * Add HTML tag to the SEO tags
     * @param message
     * @returns
     */

    addHTMLTag(message: string): string {
        const regexPattern = new RegExp(SEO_TAGS.map((option) => `\\b${option}\\b`).join('|'), 'g');

        return message.replace(regexPattern, '<code>$&</code>');
    }

    getReadMore(): Record<SEO_MEDIA_TYPES, string[]> {
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
     * Validate if all the values are falsy or empty
     * @param values
     * @returns
     */
    areAllFalsyOrEmpty(values: (string | NodeListOf<Element>)[]): boolean {
        return values.every(
            (value) => value === null || value === undefined || value?.length === 0
        );
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
            switchMap((response) => {
                if (response.status === 404) {
                    imageUrl = IMG_NOT_FOUND_KEY;
                }

                return response.clone().blob();
            }),
            map(({ size }) => {
                return {
                    length: size,
                    url: imageUrl
                };
            }),
            catchError(() => {
                return from(this.dotUploadService.uploadFile({ file: imageUrl })).pipe(
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

    /**
     * Get the key values
     * @param items
     * @returns
     */
    getKeyValues(items: SeoRulesResult[]): SeoKeyResult {
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
}
