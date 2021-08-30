import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'dotFilter'
})
export class DotFilterPipe implements PipeTransform {
    transform<T>(value: T[], keys: string, term: string): T[] {
        if (!term) return value;
        return (value || []).filter((item: any) =>
            keys.split(',').some((key) => {
                return item.hasOwnProperty(key) && new RegExp(term, 'gi').test(item[key]);
            })
        );
    }
}
