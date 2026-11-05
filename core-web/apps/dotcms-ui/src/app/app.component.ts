import { concat, of } from 'rxjs';

import { Component, DestroyRef, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';

import { catchError, map } from 'rxjs/operators';

import {
    DEFAULT_COLORS,
    DotLicenseService,
    DotNavLogoService,
    DotUiColorsService
} from '@dotcms/data-access';
import { DotcmsConfigService } from '@dotcms/dotcms-js';
import { DotLicense } from '@dotcms/dotcms-models';

import { DotAlertConfirmComponent } from './view/components/_common/dot-alert-confirm/dot-alert-confirm';

@Component({
    selector: 'dot-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [RouterOutlet, DotAlertConfirmComponent]
})
export class AppComponent implements OnInit {
    private dotCmsConfigService = inject(DotcmsConfigService);
    private dotUiColors = inject(DotUiColorsService);
    private dotNavLogoService = inject(DotNavLogoService);
    private dotLicense = inject(DotLicenseService);
    private destroyRef = inject(DestroyRef);

    ngOnInit() {
        this.dotCmsConfigService
            .getConfig()
            .pipe(
                catchError((error) => {
                    console.warn('Failed to load configuration, using defaults:', error);
                    // Keep listening: a post-login retry can restore branding without a page refresh.
                    return concat(of(null), this.dotCmsConfigService.getConfigUpdates());
                }),
                map((config) => ({
                    colors: config?.colors ?? DEFAULT_COLORS,
                    navBar: config?.logos?.navBar,
                    license: config?.license
                })),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe(({ colors, navBar, license }) => {
                // AuthGuardService loads messages after authentication, before activating backend routes.
                if (navBar) {
                    this.dotNavLogoService.setLogo(navBar);
                }

                const htmlElement = document.querySelector('html') as HTMLElement;
                if (htmlElement) {
                    this.dotUiColors.setColors(htmlElement, colors);
                }

                if (license) {
                    this.dotLicense.setLicense(license as DotLicense);
                }
            });
    }
}
