import { Pipe, PipeTransform } from '@angular/core';

import { EmaPageDropzoneItem } from '../../types';

@Pipe({
    name: 'dotPosition'
})
export class DotPositionPipe implements PipeTransform {
    transform(item: EmaPageDropzoneItem, isError = false): Record<string, string> {
        return {
            position: 'absolute',
            left: `${isError ? '0' : item.x}px`,
            top: `${isError ? '0' : item.y}px`,
            width: `${item.width}px`,
            height: `${item.height}px`
        };
    }
}
