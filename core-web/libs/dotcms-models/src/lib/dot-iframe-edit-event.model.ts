import { DotCopyContent } from './dot-copy-content.model';

export interface DotIframeEditEvent<T = { [key: string]: DOMStringMap }> {
    container?: DOMStringMap;
    dataset?: DOMStringMap;
    data?: T;
    name: string;
    target?: unknown;
    copyContent?: DotCopyContent;
}
