export interface Mark {
    type: string;
    attrs: Record<string, string>;
}

export interface ContentNode {
    type: string;
    content: ContentNode[];
    attrs?: Record<string, string>;
    marks?: Mark[];
    text?: string;
}
