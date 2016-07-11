import {Component, Inject, ViewEncapsulation} from '@angular/core';

// Custom Components
import {LoginComponent} from './common/login-component/login-component';
import {MainComponent} from './common/main-component/main-component';

@Component({
    directives: [MainComponent, LoginComponent],
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

    login: boolean= true;
    _menuItems: Array<any>;

    constructor(@Inject('menuItems') private menuItems: Array<any>) {
        if (menuItems.navigationItems.length === 0) {
            this.login = true;
        }else {
            this.login = false;
        }

        this._menuItems = menuItems;
    }

   toggleMain(change: boolean): void {
       this.login = change;
   }

}
