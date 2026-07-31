import { Pipe, PipeTransform } from '@angular/core';

/**
 * Extracts the last non-empty path segment (folder / site name).
 *
 * @export
 * @class FolderNamePipe
 * @implements {PipeTransform}
 */
@Pipe({
    name: 'folderName',
    pure: true
})
export class FolderNamePipe implements PipeTransform {
    transform(value: string): string {
        return value.split('/').filter(Boolean).pop() || '';
    }
}
