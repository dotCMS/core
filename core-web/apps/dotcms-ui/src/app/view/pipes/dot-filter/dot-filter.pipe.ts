import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'dotFilter'
})
export class DotFilterPipe implements PipeTransform {
    transform<T>(value: T[], keys: string, term: string): T[] {
        if (!term) return value;

        return (value || []).filter((item: T) =>
            keys.split(',').some((key) => {
                // The caller names the keys to match on, so a row is only known by key here.
                const row = item as Record<string, unknown>;

                return key in row && new RegExp(term, 'gi').test(String(row[key]));
            })
        );
    }
}
