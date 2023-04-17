/**
 * Represent only the properties that define the status of any type of content
 */
export interface DotContentState {
    archived?: string | boolean;
    deleted?: string | boolean;
    live: string | boolean;
    working: string | boolean;
    hasLiveVersion: string | boolean;
}
