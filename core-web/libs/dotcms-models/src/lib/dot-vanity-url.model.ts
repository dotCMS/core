export interface VanityUrl {
    pattern: string;
    vanityUrlId: string;
    url: string;
    siteId: string;
    languageId: number;
    forwardTo: string;
    response: number;
    order: number;
    temporaryRedirect: boolean;
    permanentRedirect: boolean;
    forward: boolean;
}
