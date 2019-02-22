import { Component, OnInit } from '@angular/core';
import { NotLicensedService } from '@services/not-licensed-service';
import { DotcmsConfig } from 'dotcms-js';
import { DotUiColors, DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { take, pluck } from 'rxjs/operators';

import {initDotCMS} from 'dotcms';

@Component({
    selector: 'dot-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
    constructor(
        notLicensedService: NotLicensedService,
        private dotCmsConfig: DotcmsConfig,
        private dotUiColors: DotUiColorsService
    ) {
        notLicensedService.init();
    }

    ngOnInit() {
        const dotcms = initDotCMS({
            host: 'http://localhost:8080',
            token: 'eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIyNzFlYWFkZS1mYjhiLTQwY2MtODc4Yi1kYzE1MGZhMGE3N2MiLCJpYXQiOjE1NTA4NDE2MDIsInVwZGF0ZWRfYXQiOjEyMDQ4MjQ5NjEwMDAsInN1YiI6ImRvdGNtcy5vcmcuMSIsImlzcyI6IjJhOWNmYmY3LTUyNDktNGM0Yi05NWE4LTkxMzI5YzE5MTY4MyIsImV4cCI6MTU1MTcwNTYwMn0.dsdGPbzrq9G2i7UIJQRwq-6w6JVWCeNzPzEGgdaQClM',
            environment: 'development'
        });

        dotcms.page.get({
            url: '/about-us'
        }).then(data => {
            console.log(data);
        });

        this.dotCmsConfig
            .getConfig()
            .pipe(
                take(1),
                pluck('colors')
            )
            .subscribe((colors: DotUiColors) => {
                this.dotUiColors.setColors(document.querySelector('html'), colors);
            });
    }
}
