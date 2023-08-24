export enum SEO_OPTIONS {
    FAVICON = 'favicon',
    TITLE = 'title',
    DESCRIPTION = 'description',
    OG_DESCRIPTION = 'og:description'
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
    MAX_FAVICONS = 1,
    MAX_TITLES = 1
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
    [key: string]: string | NodeListOf<Element> | string[] | undefined;
}
