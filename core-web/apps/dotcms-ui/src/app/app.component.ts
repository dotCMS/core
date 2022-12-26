import { Component, OnInit } from '@angular/core';
import { take, map } from 'rxjs/operators';

import { ConfigParams, DotcmsConfigService, DotUiColors } from '@dotcms/dotcms-js';
import { DotMessageService } from '@dotcms/data-access';
import { DotUiColorsService } from '@dotcms/app/api/services/dot-ui-colors/dot-ui-colors.service';
import { DotNavLogoService } from './api/services/dot-nav-logo/dot-nav-logo.service';

@Component({
    selector: 'dot-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
    constructor(
        private dotCmsConfigService: DotcmsConfigService,
        private dotUiColors: DotUiColorsService,
        private dotMessageService: DotMessageService,
        private dotNavLogoService: DotNavLogoService
    ) {}

    ngOnInit() {
        this.dotCmsConfigService
            .getConfig()
            .pipe(
                take(1),
                map((config: ConfigParams) => {
                    return {
                        buildDate: config.releaseInfo?.buildDate,
                        colors: config.colors,
                        navBar: config.logos?.navBar
                    };
                })
            )
            .subscribe(
                ({
                    buildDate,
                    colors,
                    navBar
                }: {
                    buildDate: string;
                    colors: DotUiColors;
                    navBar: string;
                }) => {
                    this.dotMessageService.init({ buildDate });
                    this.dotNavLogoService.setLogo(navBar);
                    this.dotUiColors.setColors(document.querySelector('html'), colors);
                }
            );
    }
}
