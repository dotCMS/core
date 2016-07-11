import {Component, Inject, ViewEncapsulation} from '@angular/core';

// Custom Components
import {LoginComponent} from './common/login-component/login-component';
import {MainComponent} from './common/main-component/main-component';


@Component({
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    selector: 'app',
    styleUrls: ['app.css'],
    template: `
        <div *ngIf="login">
            <dot-login-component></dot-login-component>
        </div>
        <div  *ngIf="!login">
            <dot-main-component></dot-main-component>
        </div>
    `,
    providers: [],
    directives: [MainComponent,LoginComponent],
    encapsulation: ViewEncapsulation.Emulated
})

/**
 * Display the login component or the main component if
 * there is a navigation menu set
 */
export class AppComponent {
    login:boolean=true;
    constructor(@Inject('menuItems') private menuItems:any[]) {
        if(menuItems.navigationItems.length == 0) {
            this.login=true;
        }else{
            this.login=false;
        }
    }
}

