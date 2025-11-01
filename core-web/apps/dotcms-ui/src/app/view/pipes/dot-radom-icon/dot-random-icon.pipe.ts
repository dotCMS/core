import { Pipe, PipeTransform } from '@angular/core';

export const LABEL_IMPORTANT_ICON = 'label_important';

// the 'label_important' icon is not supported as a valid icon, instead the system will randomly assign a new icon
// The reference of the icon comes from this file: Task210316UpdateLayoutIcons.java
@Pipe({
    name: 'dotRandomIcon',
    standalone: true
})
export class DotRandomIconPipe implements PipeTransform {
    iconsArray = [
        'grid_view',
        'settings',
        'format_align_left',
        'folder_open',
        'file_copy',
        'settings_ethernet',
        'dashboard',
        'double_arrow',
        'window',
        'adjust',
        'api',
        'apps',
        'blur_on',
        'code',
        'equalizer',
        'wysiwyg',
        'stream',
        'storage',
        'schema',
        'poll'
    ];

    private hasCode(value: string): number {
        let hash = 0;
        [...value].forEach((char) => {
            hash = (hash << 5) - hash + char.charCodeAt(0);
            hash |= 0;
        });

        return Math.abs(hash) % 19;
    }

    transform(value: string): string {
        return this.iconsArray[this.hasCode(value)];
    }
}
