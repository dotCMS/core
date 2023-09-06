export enum SEO_OPTIONS {
    FAVICON = 'favicon',
    TITLE = 'title',
    DESCRIPTION = 'description',
    OG_DESCRIPTION = 'og:description',
    OG_TITLE = 'og:title',
    OG_IMAGE = 'og:image'
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
    MAX_FAVICONS = 1,
    MAX_TITLES = 1,
    MAX_IMAGE_BYTES = 8000000
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
    description?: string;
    'og:description'?: string;
    'og:image'?: string;
    'og:title'?: string;
}

export const SeoMediaKeys = {
    facebook: [SEO_OPTIONS.DESCRIPTION, SEO_OPTIONS.OG_IMAGE, SEO_OPTIONS.OG_TITLE],
    google: [SEO_OPTIONS.DESCRIPTION, SEO_OPTIONS.FAVICON, SEO_OPTIONS.TITLE],
    linkedin: [],
    twitter: []
};

export interface ImageMetaData {
    length: number;
    url: string;
}
