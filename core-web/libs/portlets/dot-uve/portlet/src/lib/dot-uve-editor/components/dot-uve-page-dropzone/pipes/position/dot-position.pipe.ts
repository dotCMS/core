import { Pipe, PipeTransform } from '@angular/core';

import { DotUvePageDropzoneItem } from '../../types';

@Pipe({
    name: 'dotPosition'
})
export class DotPositionPipe implements PipeTransform {
    transform(item: DotUvePageDropzoneItem, isError = false): Record<string, string> {
        return {
            position: 'absolute',
            left: `${isError ? '0' : item.x}px`,
            top: `${isError ? '0' : item.y}px`,
            width: `${item.width}px`,
            height: `${item.height}px`
        };
    }
}
