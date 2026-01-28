import { of } from 'rxjs';

import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { catchError, map, take } from 'rxjs/operators';

import {
    DEFAULT_COLORS,
    DotLicenseService,
    DotMessageService,
    DotUiColorsService
} from '@dotcms/data-access';
import { ConfigParams, DotcmsConfigService, DotUiColors } from '@dotcms/dotcms-js';
import { DotLicense } from '@dotcms/dotcms-models';

import { DotNavLogoService } from './api/services/dot-nav-logo/dot-nav-logo.service';
import { DotAlertConfirmComponent } from './view/components/_common/dot-alert-confirm/dot-alert-confirm';

@Component({
    selector: 'dot-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    imports: [RouterOutlet, DotAlertConfirmComponent]
})
export class AppComponent implements OnInit {
    private dotCmsConfigService = inject(DotcmsConfigService);
    private dotUiColors = inject(DotUiColorsService);
    private dotMessageService = inject(DotMessageService);
    private dotNavLogoService = inject(DotNavLogoService);
    private dotLicense = inject(DotLicenseService);

    ngOnInit() {
        this.dotCmsConfigService
            .getConfig()
            .pipe(
                take(1),
                map((config: ConfigParams) => {
                    return {
                        buildDate: config.releaseInfo?.buildDate,
                        colors: config.colors,
                        navBar: config.logos?.navBar,
                        license: config.license
                    };
                }),
                // Handle errors gracefully - use default colors if config fails to load
                // This ensures the app works even if user is not authenticated or endpoint fails
                catchError((error) => {
                    console.warn('Failed to load configuration, using defaults:', error);
                    // Return default values that allow the app to continue functioning
                    return of({
                        buildDate: null,
                        colors: DEFAULT_COLORS,
                        navBar: null,
                        license: null
                    });
                })
            )
            .subscribe(
                ({
                    buildDate,
                    colors,
                    navBar,
                    license
                }: {
                    buildDate: string | null;
                    colors: DotUiColors;
                    navBar: string | null;
                    license: DotLicense | null;
                }) => {
                    // Initialize services with loaded or default values
                    if (buildDate) {
                        this.dotMessageService.init({ buildDate });
                    }

                    if (navBar) {
                        this.dotNavLogoService.setLogo(navBar);
                    }

                    // Always set colors (will use defaults if config failed)
                    // This ensures PrimeNG theme is always initialized
                    const htmlElement = document.querySelector('html') as HTMLElement;
                    if (htmlElement) {
                        this.dotUiColors.setColors(htmlElement, colors);
                    }

                    if (license) {
                        this.dotLicense.setLicense(license);
                    }
                }
            );
    }
}
