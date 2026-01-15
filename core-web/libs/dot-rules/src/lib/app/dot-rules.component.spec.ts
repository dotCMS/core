import { TestBed, waitForAsync } from '@angular/core/testing';

import { LoginService } from '@dotcms/dotcms-js';

import { DotRulesComponent } from './dot-rules.component';
import { DotRuleEngineContainerComponent } from '../dot-rule-engine-container/dot-rule-engine-container.component';

describe('DotRulesComponent', () => {
    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [DotRulesComponent],
            providers: [
                {
                    provide: LoginService,
                    useValue: {}
                }
            ]
        })
            .overrideComponent(DotRulesComponent, {
                remove: { imports: [DotRuleEngineContainerComponent] },
                add: { imports: [] }
            })
            .compileComponents();
    }));

    it('should create the app', waitForAsync(() => {
        const fixture = TestBed.createComponent(DotRulesComponent);
        const app = fixture.debugElement.componentInstance;
        expect(app).toBeTruthy();
    }));
});
