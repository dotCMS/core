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
    MIN_OG_TITLE_LENGTH = 30,
    MAX_OG_TITLE_LENGTH = 60,
    MIN_OG_DESCRIPTION_LENGTH = 55,
    MAX_OG_DESCRIPTION_LENGTH = 150,
    MAX_FAVICONS = 1,
    MAX_TITLES = 1,
    MAX_IMAGE_BYTES = 8000000,
    MAX_TWITTER_IMAGE_BYTES = 5000000,
    MAX_TWITTER_DESCRIPTION_LENGTH = 200,
    MIN_TWITTER_DESCRIPTION_LENGTH = 30,
    MIN_TWITTER_TITLE_LENGTH = 30,
    MAX_TWITTER_TITLE_LENGTH = 70
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
    descriptionElements?: NodeListOf<Element>;
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
    linkedin: [SEO_OPTIONS.OG_DESCRIPTION, SEO_OPTIONS.OG_IMAGE, SEO_OPTIONS.OG_TITLE],
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

export enum SEO_SOCIAL_MEDIA {
    FACEBOOK = 'Facebook',
    TWITTER = 'Twitter',
    LINKEDIN = 'LinkedIn'
}

export enum SEO_SEARCH_ENGINE {
    GOOGLE = 'Google'
}

export enum SEO_MEDIA_TYPES {
    FACEBOOK = SEO_SOCIAL_MEDIA.FACEBOOK,
    TWITTER = SEO_SOCIAL_MEDIA.TWITTER,
    LINKEDIN = SEO_SOCIAL_MEDIA.LINKEDIN,
    GOOGLE = SEO_SEARCH_ENGINE.GOOGLE
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

export const SOCIAL_MEDIA_TILES: Record<SEO_SOCIAL_MEDIA, SocialMediaOption> = {
    [SEO_SOCIAL_MEDIA.FACEBOOK]: {
        label: 'Facebook',
        value: SEO_MEDIA_TYPES.FACEBOOK,
        icon: 'pi pi-facebook',
        description: 'seo.rules.media.preview.tile'
    },
    [SEO_SOCIAL_MEDIA.TWITTER]: {
        label: 'X (Formerly Twitter)',
        value: SEO_MEDIA_TYPES.TWITTER,
        icon: 'pi pi-twitter',
        description: 'seo.rules.media.preview.tile'
    },
    [SEO_SOCIAL_MEDIA.LINKEDIN]: {
        label: 'Linkedin',
        value: SEO_MEDIA_TYPES.LINKEDIN,
        icon: 'pi pi-linkedin',
        description: 'seo.rules.media.preview.tile'
    }
};

export const SEARCH_ENGINE_TILES: Record<SEO_SEARCH_ENGINE, SocialMediaOption> = {
    [SEO_SEARCH_ENGINE.GOOGLE]: {
        label: 'Google',
        value: SEO_MEDIA_TYPES.GOOGLE,
        icon: 'pi pi-google',
        description: 'seo.rules.media.search.engine'
    }
};

export const SEO_TILES: Record<SEO_MEDIA_TYPES, SocialMediaOption> = {
    ...SOCIAL_MEDIA_TILES,
    ...SEARCH_ENGINE_TILES
};

export interface SocialMediaOption {
    label: string;
    value: SEO_MEDIA_TYPES;
    icon: string;
    description: string;
}

export const IMG_NOT_FOUND_KEY = 'not-found';
