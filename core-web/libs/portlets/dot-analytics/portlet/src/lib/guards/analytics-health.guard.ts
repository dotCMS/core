import { Observable } from 'rxjs';

import { inject } from '@angular/core';
import { ActivatedRoute, CanMatchFn, Router } from '@angular/router';

import { map, shareReplay } from 'rxjs/operators';

import { DotExperimentsService } from '@dotcms/data-access';
import { HealthStatusTypes } from '@dotcms/dotcms-models';

// Cache global para el health check - compartido entre todas las ejecuciones del guard
let healthCheckCache$: Observable<HealthStatusTypes> | null = null;

/**
 * Guard optimizado que protege las rutas de analytics.
 * Usa shareReplay para evitar múltiples llamadas al health check.
 */
export const analyticsHealthGuard: CanMatchFn = (_route, _segments) => {
    const dotExperimentsService = inject(DotExperimentsService);
    const router = inject(Router);
    const activatedRoute = inject(ActivatedRoute);

    // Si no hay cache, crear uno con shareReplay
    if (!healthCheckCache$) {
        healthCheckCache$ = dotExperimentsService.healthCheck().pipe(
            shareReplay(1) // ← CLAVE: Comparte el último resultado entre múltiples suscriptores
        );
    }

    // Usar el observable cacheado
    return healthCheckCache$.pipe(
        map((healthStatus) => {
            if (healthStatus === HealthStatusTypes.OK) {
                return true; // Allow access to the route
            }

            // Get isEnterprise from route data (resolved at parent level)
            const isEnterprise = activatedRoute.snapshot.data?.['isEnterprise'] ?? true;

            // Redirect to error page with status information
            router.navigate(['/analytics/error'], {
                queryParams: {
                    status: healthStatus,
                    isEnterprise: isEnterprise
                }
            });

            return false; // Block access to the route
        })
    );
};

/**
 * Función para limpiar el cache del health check (útil para testing o forzar revalidación)
 */
export function clearAnalyticsHealthCache(): void {
    healthCheckCache$ = null;
}
