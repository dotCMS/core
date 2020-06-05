import { Component, OnInit } from '@angular/core';
import { NotLicensedService } from '@services/not-licensed-service';
import { DotcmsConfigService } from 'dotcms-js';
import { DotUiColors, DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { take, pluck } from 'rxjs/operators';
import { DotMessageService } from '@services/dot-messages-service';

@Component({
    selector: 'dot-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
    constructor(
        private notLicensedService: NotLicensedService,
        private dotCmsConfigService: DotcmsConfigService,
        private dotUiColors: DotUiColorsService,
        private dotMessageService: DotMessageService
    ) {}

    ngOnInit() {
        this.dotMessageService.init();
        this.notLicensedService.init();
        this.dotCmsConfigService
            .getConfig()
            .pipe(take(1), pluck('colors'))
            .subscribe((colors: DotUiColors) => {
                this.dotUiColors.setColors(document.querySelector('html'), colors);
            });
    }
}
