import { Pipe, PipeTransform } from '@angular/core';

import { EmaPageDropzoneItem } from '../../types';

@Pipe({
    name: 'dotPosition',
    standalone: true
})
export class DotPositionPipe implements PipeTransform {
    transform(item: EmaPageDropzoneItem): Record<string, string> {
        return {
            position: 'absolute',
            left: `${item.x}px`,
            top: `${item.y}px`,
            width: `${item.width}px`,
            height: `${item.height}px`
        };
    }
}
