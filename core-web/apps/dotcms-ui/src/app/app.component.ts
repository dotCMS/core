import { Component, OnInit, inject } from '@angular/core';

import { map, take } from 'rxjs/operators';

import { DotLicenseService, DotMessageService } from '@dotcms/data-access';
import { ConfigParams, DotcmsConfigService, DotUiColors } from '@dotcms/dotcms-js';
import { DotLicense } from '@dotcms/dotcms-models';

import { DotNavLogoService } from './api/services/dot-nav-logo/dot-nav-logo.service';
import { DotUiColorsService } from './api/services/dot-ui-colors/dot-ui-colors.service';

@Component({
    selector: 'dot-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    standalone: false
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
                })
            )
            .subscribe(
                ({
                    buildDate,
                    colors,
                    navBar,
                    license
                }: {
                    buildDate: string;
                    colors: DotUiColors;
                    navBar: string;
                    license: DotLicense;
                }) => {
                    this.dotMessageService.init({ buildDate });
                    this.dotNavLogoService.setLogo(navBar);
                    this.dotUiColors.setColors(document.querySelector('html'), colors);
                    this.dotLicense.setLicense(license);
                }
            );
    }
}
