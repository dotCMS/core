import { Component, OnInit } from '@angular/core';
import { ConfigParams, DotcmsConfigService, DotUiColors } from '@dotcms/dotcms-js';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { take, map } from 'rxjs/operators';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotNavLogoService } from '@services/dot-nav-logo/dot-nav-logo.service';
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
