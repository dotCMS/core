import { Pipe, PipeTransform } from '@angular/core';

/**
 * This pipe extracts the folder name from a path
 *
 * @export
 * @class TruncatePathPipe
 * @implements {PipeTransform}
 */
@Pipe({
    name: 'folderName',
    standalone: true,
    pure: true
})
export class FolderNamePipe implements PipeTransform {
    transform(value: string): string {
        return value.split('/').filter(Boolean).pop() || '';
    }
}
