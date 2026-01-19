import { Component, inject } from '@angular/core';

import { LoginService } from '@dotcms/dotcms-js';

import { DotRuleEngineContainerComponent } from '../features/rule-engine/dot-rule-engine-container.component';

@Component({
    selector: 'dot-rules',
    templateUrl: './dot-rules.component.html',
    styleUrls: ['./dot-rules.component.scss'],
    imports: [DotRuleEngineContainerComponent]
})
export class DotRulesComponent {
    loginService = inject(LoginService);
}
