import { pluck, map } from 'rxjs/operators';
import { pipe } from 'rxjs/util/pipe';

export const getDrawed = pluck('template', 'drawed');
export const isAdvanced = map((isDrawed: boolean) => !isDrawed);
export const getTemplateTypeFlag = pipe(getDrawed, isAdvanced);
