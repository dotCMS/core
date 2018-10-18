import { Component, OnInit } from '@angular/core';
import { LoginService } from  'dotcms-js/dotcms-js' ;
import { environment } from '../environments/environment';

@Component({
    selector: 'app-rules',
    template: '<cw-rule-engine-container class="rules__engine-container" *ngIf="this.loginService.auth"></cw-rule-engine-container>'
})
export class AppRulesComponent implements OnInit {
    constructor(public loginService: LoginService) {}

    ngOnInit() {
        // TODO: need to find a better wat to login
        if (!this.loginService.auth) {
            this.loginService.loginUser('admin@dotcms.com', 'admin', false, 'en-US').subscribe(res => {
                console.log('fake login', res);
            });
        }
    }
}
