/**
 * Represent only the properties that define the status of any type of content
 */
export interface DotContentState {
    live: string | boolean;
    working: string | boolean;
    deleted: string | boolean;
    hasLiveVersion: string | boolean;
}
