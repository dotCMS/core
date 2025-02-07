import { DotCMSContentlet } from '../../types';

/**
 * Helper function that returns an object containing the dotCMS data attributes.
 */
export function getDotContentletAttributes(
    contentlet: DotCMSContentlet,
    container: string
): Record<string, any> {
    return {
        'data-dot-identifier': contentlet?.identifier,
        'data-dot-basetype': contentlet?.baseType,
        'data-dot-title': contentlet?.widgetTitle || contentlet?.title,
        'data-dot-inode': contentlet?.inode,
        'data-dot-type': contentlet?.contentType,
        'data-dot-container': container,
        'data-dot-on-number-of-pages': contentlet?.onNumberOfPages
    };
}
