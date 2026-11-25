import { Component, DestroyRef, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';

import { map } from 'rxjs/operators';

import {
    DEFAULT_COLORS,
    DotLicenseService,
    DotNavLogoService,
    DotUiColorsService
} from '@dotcms/data-access';
import { DotLicense } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotAlertConfirmComponent } from './view/components/_common/dot-alert-confirm/dot-alert-confirm';

@Component({
    selector: 'dot-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [RouterOutlet, DotAlertConfirmComponent]
})
export class AppComponent implements OnInit {
    readonly #systemConfig$ = toObservable(inject(GlobalStore).systemConfig);
    private dotUiColors = inject(DotUiColorsService);
    private dotNavLogoService = inject(DotNavLogoService);
    private dotLicense = inject(DotLicenseService);
    private destroyRef = inject(DestroyRef);

    ngOnInit() {
        this.#systemConfig$
            .pipe(
                map((config) => ({
                    colors: config?.colors ?? DEFAULT_COLORS,
                    navBar: config?.logos?.navBar,
                    license: config?.license
                })),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe(({ colors, navBar, license }) => {
                // GlobalStore owns post-login configuration and translation loading.
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
