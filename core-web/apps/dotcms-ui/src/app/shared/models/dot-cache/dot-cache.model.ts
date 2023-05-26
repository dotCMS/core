export interface DotCacheProvider {
    distributed?: boolean;
    groups: string[];
    initialized: boolean;
    key: string;
    name: string;
}
