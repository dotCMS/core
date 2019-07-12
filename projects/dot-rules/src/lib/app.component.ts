import { Component } from '@angular/core';
import { LoginService } from 'dotcms-js';

@Component({
    selector: 'app-rules',
    styles: [':host { display: block; width: 100%; height: 100% }'],
    template:
        '<cw-rule-engine-container class="rules__engine-container" *ngIf="this.loginService.auth"></cw-rule-engine-container>'
})
export class AppRulesComponent {
    constructor(public loginService: LoginService) {}
}
