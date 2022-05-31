import { Component } from '@angular/core';
import { LoginService } from '@dotcms/dotcms-js';

@Component({
    selector: 'app-rules',
    styles: [':host { display: flex; width:100%; min-height: 100%; height: 100% }'],
    template:
        '<cw-rule-engine-container class="rules__engine-container" *ngIf="this.loginService.auth"></cw-rule-engine-container>'
})
export class AppRulesComponent {
    constructor(public loginService: LoginService) {}
}
