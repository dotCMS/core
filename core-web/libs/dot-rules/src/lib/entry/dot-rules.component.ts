import { Component, inject } from '@angular/core';

import { LoginService } from '@dotcms/dotcms-js';

import { DotRuleEngineContainerComponent } from '../features/rule-engine/container/dot-rule-engine-container.component';

@Component({
    selector: 'dot-rules',
    templateUrl: './dot-rules.component.html',
    imports: [DotRuleEngineContainerComponent],
    host: {
        class: 'flex w-full min-h-full h-full'
    }
})
export class DotRulesComponent {
    loginService = inject(LoginService);
}
