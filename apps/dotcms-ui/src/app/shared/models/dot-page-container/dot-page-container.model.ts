/**
 * Represent a Container linked with a page and with content added to it
 */
export interface DotPageContainer {
    identifier: string;
    uuid: string;
    contentletsId?: string[];
    path?: string;
}

export interface DotPageContainerPersonalized extends DotPageContainer {
    personaTag?: string;
}
