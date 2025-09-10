import { Component, inject } from '@angular/core';

import { LoginService } from '@dotcms/dotcms-js';

@Component({
    selector: 'app-rules',
    styles: [
        ':host { display: flex; width:100%; min-height: 100%; height: 100%; margin-right: 80px; }'
    ],
    template:
        '<cw-rule-engine-container class="rules__engine-container" *ngIf="this.loginService.auth"></cw-rule-engine-container>',
    standalone: false
})
export class AppRulesComponent {
    loginService = inject(LoginService);
}
