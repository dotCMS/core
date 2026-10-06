import { Pipe, PipeTransform } from '@angular/core';

/**
 * Extracts the last non-empty path segment (folder / site name).
 *
 * @export
 * @class DotFolderNamePipe
 * @implements {PipeTransform}
 */
@Pipe({
    name: 'dotFolderName',
    pure: true
})
export class DotFolderNamePipe implements PipeTransform {
    transform(value?: string | null): string {
        if (!value) {
            return '';
        }

        return value.split('/').filter(Boolean).pop() || '';
    }
}
