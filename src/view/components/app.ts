import {Component, ViewEncapsulation, Input} from '@angular/core';
import {DotcmsConfig} from '../../api/services/system/dotcms-config';
import {LoginService} from '../../api/services/login-service';

@Component({
    directives: [],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [],
    selector: 'app',
    styleUrls: ['app.css'],
    templateUrl: ['app.html']
})

/**
 * Display the login component or the main component if
 * there is a navigation menu set
 */
export class AppComponent {
    constructor(private dotcmsConfig: DotcmsConfig) {
    }

    ngOnInit(): void {

        this.dotcmsConfig.getConfig().subscribe(() => {

        });
    }
}
