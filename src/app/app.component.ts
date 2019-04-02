import { Component, OnInit, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { NotLicensedService } from '@services/not-licensed-service';
import { DotcmsConfig } from 'dotcms-js';
import { DotUiColors, DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { take, pluck } from 'rxjs/operators';
import { initDotCMS } from '../../projects/dotcms/src/public_api';

@Component({
    selector: 'dot-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, AfterViewInit {
    @ViewChild('custom') custom: ElementRef;
    dotcms: any;
    constructor(
        notLicensedService: NotLicensedService,
        private dotCmsConfig: DotcmsConfig,
        private dotUiColors: DotUiColorsService
    ) {
        notLicensedService.init();
    }

    async ngOnInit() {
        this.dotcms = initDotCMS({
            token:
                // tslint:disable-next-line:max-line-length
                'eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJlYWJmNDc1MS1iZDQ3LTRhYWEtODJhYy1kZmU5MjAzOGQ4NGUiLCJ4bW9kIjoxMjA0ODI0OTYxMDAwLCJzdWIiOiJkb3RjbXMub3JnLjEiLCJpYXQiOjE1NTM3ODY5NTEsImlzcyI6ImE3MTVlYzAwLTRkZmUtNDUwNC1hZDgwLTRkNDczMTMwZDNlNiIsImV4cCI6MTU1Mzg3MzM1MX0.NYW9AD1-2oBL6zmuBypF5DAOOFbUI8vgkMGDP8RV-Io'
        });

        const dotCMSFormConfig = {
            identifier: '7fcedef5-1048-433d-9f67-bbec281d9c81',
            fields: ['text1', 'select2', 'checkbox3']
        };

        const formBuilder = await this.dotcms.form.get(dotCMSFormConfig);
        formBuilder.render(this.custom.nativeElement);

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

    ngAfterViewInit(): void {}
}
