import { QueryParamsHandling } from '@angular/router';

export interface DotNavigateToOptions {
    replaceUrl?: boolean;
    queryParamsHandling?: QueryParamsHandling;
    queryParams?: Record<string, string>;
}
