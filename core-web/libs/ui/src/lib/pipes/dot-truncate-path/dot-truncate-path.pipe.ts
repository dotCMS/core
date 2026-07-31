import { Pipe, PipeTransform } from '@angular/core';

import { FolderNamePipe } from '../dot-folder-name/dot-folder-name.pipe';

/**
 * @deprecated Use {@link FolderNamePipe} (`folderName`) instead. Kept as a thin
 * alias so existing call sites keep working during migration.
 *
 * @export
 * @class DotTruncatePathPipe
 * @implements {PipeTransform}
 */
@Pipe({
    name: 'dotTruncatePath',
    pure: true
})
export class DotTruncatePathPipe implements PipeTransform {
    private readonly folderNamePipe = new FolderNamePipe();

    transform(value: string): string {
        return this.folderNamePipe.transform(value);
    }
}
