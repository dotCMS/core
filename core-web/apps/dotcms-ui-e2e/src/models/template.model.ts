export interface Template {
    identifier: string;
    title: string;
    friendlyName: string;
    image: string;
    theme: string;
    layout?: Record<string, unknown>;
}
