import { Observable } from 'rxjs';

export enum SEO_OPTIONS {
    FAVICON = 'favicon',
    TITLE = 'title',
    DESCRIPTION = 'description',
    OG_DESCRIPTION = 'og:description',
    OG_TITLE = 'og:title',
    OG_IMAGE = 'og:image',
    TWITTER_CARD = 'twitter:card',
    TWITTER_TITLE = 'twitter:title',
    TWITTER_DESCRIPTION = 'twitter:description',
    TWITTER_IMAGE = 'twitter:image'
}

export enum SEO_RULES_ICONS {
    CHECK_CIRCLE = 'pi-check-circle',
    CHECK = 'pi-check',
    EXCLAMATION = 'pi-exclamation-triangle',
    TIMES = 'pi-times',
    EXCLAMATION_CIRCLE = 'pi-exclamation-circle'
}

export enum SEO_LIMITS {
    MIN_TITLE_LENGTH = 30,
    MAX_TITLE_LENGTH = 60,
    MIN_OG_TITLE_LENGTH = 30,
    MAX_OG_TITLE_LENGTH = 160,
    MIN_OG_DESCRIPTION_LENGTH = 55,
    MAX_OG_DESCRIPTION_LENGTH = 150,
    MAX_FAVICONS = 1,
    MAX_TITLES = 1,
    MAX_IMAGE_BYTES = 8000000,
    MAX_TWITTER_IMAGE_BYTES = 5000000,
    MAX_TWITTER_DESCRIPTION_LENGTH = 200
}

export enum SEO_RULES_COLORS {
    DONE = 'results-seo-tool__result-icon--alert-green',
    ERROR = 'results-seo-tool__result-icon--alert-red',
    WARNING = 'results-seo-tool__result-icon--alert-yellow'
}

export interface SeoRulesResult {
    message: string;
    color: string;
    itemIcon: string;
}

export interface SeoKeyResult {
    keyIcon: string;
    keyColor: string;
}

export interface SeoMetaTagsResult {
    key: string;
    title: string;
    keyIcon: string;
    keyColor: string;
    items: SeoRulesResult[];
    sort: number;
    info?: string;
}

export interface SeoMetaTags {
    favicon?: string;
    title?: string;
    faviconElements?: NodeListOf<Element>;
    titleElements?: NodeListOf<Element>;
    titleOgElements?: NodeListOf<Element>;
    imageOgElements?: NodeListOf<Element>;
    descriptionOgElements?: NodeListOf<Element>;
    description?: string;
    'og:description'?: string;
    'og:image'?: string;
    'og:title'?: string;
    'twitter:card'?: string;
    'twitter:title'?: string;
    'twitter:description'?: string;
    'twitter:image'?: string;
    twitterCardElements?: NodeListOf<Element>;
    twitterTitleElements?: NodeListOf<Element>;
    twitterDescriptionElements?: NodeListOf<Element>;
    twitterImageElements?: NodeListOf<Element>;
}

export const SeoMediaKeys = {
    facebook: [SEO_OPTIONS.OG_DESCRIPTION, SEO_OPTIONS.OG_IMAGE, SEO_OPTIONS.OG_TITLE],
    google: [SEO_OPTIONS.DESCRIPTION, SEO_OPTIONS.FAVICON, SEO_OPTIONS.TITLE],
    twitter: [
        SEO_OPTIONS.TWITTER_CARD,
        SEO_OPTIONS.TWITTER_TITLE,
        SEO_OPTIONS.TWITTER_DESCRIPTION,
        SEO_OPTIONS.TWITTER_IMAGE
    ],
    linkedin: [SEO_OPTIONS.DESCRIPTION, SEO_OPTIONS.OG_IMAGE, SEO_OPTIONS.OG_TITLE],
    all: [
        SEO_OPTIONS.DESCRIPTION,
        SEO_OPTIONS.OG_IMAGE,
        SEO_OPTIONS.OG_TITLE,
        SEO_OPTIONS.FAVICON,
        SEO_OPTIONS.TITLE,
        SEO_OPTIONS.OG_DESCRIPTION,
        SEO_OPTIONS.TWITTER_CARD,
        SEO_OPTIONS.TWITTER_TITLE,
        SEO_OPTIONS.TWITTER_DESCRIPTION,
        SEO_OPTIONS.TWITTER_IMAGE
    ]
};

export interface ImageMetaData {
    length: number;
    url: string;
}

export interface OpenGraphOptions {
    getItems: (object: SeoMetaTags) => Observable<SeoRulesResult[]>;
    sort: number;
    info: string;
}

export interface MetaTagsPreview {
    hostName: string;
    title: string;
    description: string;
    type: string;
    isMobile: boolean;
    image?: string;
    twitterTitle?: string;
    twitterCard?: string;
    twitterDescription?: string;
    twitterImage?: string;
}

export enum SEO_MEDIA_TYPES {
    GOOGLE = 'Google',
    TWITTER = 'Twitter',
    LINKEDIN = 'LinkedIn',
    FACEBOOK = 'Facebook'
}

export const SEO_TAGS = [
    SEO_OPTIONS.OG_DESCRIPTION,
    SEO_OPTIONS.OG_TITLE,
    SEO_OPTIONS.OG_IMAGE,
    SEO_OPTIONS.TWITTER_CARD,
    SEO_OPTIONS.TWITTER_TITLE,
    SEO_OPTIONS.TWITTER_DESCRIPTION,
    SEO_OPTIONS.TWITTER_IMAGE
];
