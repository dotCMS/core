import {Component, ViewEncapsulation} from '@angular/core';
import {DotcmsConfig} from './api/services/system/dotcms-config';
import {NotLicensedService} from './api/services/not-licensed-service';

@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [],
    selector: 'app',
    // TODO: make primeng into main.scss
    styleUrls: ['app.scss'],
    templateUrl: 'app.html'
})

/**
 * Display the login component or the main component if
 * there is a navigation menu set
 */
export class AppComponent {
    constructor(private dotcmsConfig: DotcmsConfig, notLicensedService: NotLicensedService) {
        notLicensedService.init();
        document.ondragover = document.ondrop = (ev) => {
            ev.preventDefault();
        };
    }
}
