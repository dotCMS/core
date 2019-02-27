export interface DotAppNavItem {
    code?: any;
    folder: string;
    children?: DotAppNavItem[];
    host: string;
    languageId: number;
    href: string;
    title: string;
    type: string;
    hash: number;
    target: string;
    order: number;
}
