/* eslint-disable @typescript-eslint/no-explicit-any */

import { of as observableOf, Observable } from 'rxjs';
import { ComponentFixture } from '@angular/core/testing';
import { DebugElement, Component } from '@angular/core';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { PushPublishEnvSelectorComponent } from './dot-push-publish-env-selector.component';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { UntypedFormGroup, UntypedFormControl } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

export class PushPublishServiceMock {
    _lastEnvironmentPushed: string[];

    pushPublishContent(): Observable<any> {
        return observableOf([]);
    }

    getEnvironments(): Observable<any> {
        return observableOf([
            {
                id: '22e332',
                name: 'my environment'
            },
            {
                id: 'joa08',
                name: 'my environment 2'
            }
        ]);
    }

    get lastEnvironmentPushed(): string[] {
        return this._lastEnvironmentPushed;
    }
}

@Component({
    selector: 'dot-test-host-component',
    template: `<form [formGroup]="group">
        <dot-push-publish-env-selector
            showList="true"
            formControlName="environment"
        ></dot-push-publish-env-selector>
    </form>`
})
class TestHostComponent {
    group: UntypedFormGroup;
    constructor() {
        this.group = new UntypedFormGroup({
            environment: new UntypedFormControl('')
        });
    }
}

describe('PushPublishEnvSelectorComponent', () => {
    let comp: PushPublishEnvSelectorComponent;
    let fixture: ComponentFixture<PushPublishEnvSelectorComponent>;
    let de: DebugElement;
    let hostComponentfixture: ComponentFixture<TestHostComponent>;
    let pushPublishServiceMock: PushPublishServiceMock;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.content.push_publish.select_environment': 'Select environment'
    });

    beforeEach(() => {
        pushPublishServiceMock = new PushPublishServiceMock();

        DOTTestBed.configureTestingModule({
            declarations: [PushPublishEnvSelectorComponent, TestHostComponent],
            imports: [BrowserAnimationsModule],
            providers: [
                PushPublishService,
                { provide: PushPublishService, useValue: pushPublishServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });

        fixture = DOTTestBed.createComponent(PushPublishEnvSelectorComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
    });

    it('should propagate change on value change', () => {
        comp.selectedEnvironmentIds = [];
        expect(comp.selectedEnvironmentIds).toEqual([]);

        spyOn(comp, 'propagateChange');
        comp.valueChange(new Event('MouseEvent'), [
            {
                id: '22e332',
                name: 'my environment'
            }
        ]);

        expect(comp.selectedEnvironmentIds).toEqual(['22e332']);
        expect(comp.propagateChange).toHaveBeenCalled();
    });

    it('should propagate change on remove environment item', () => {
        comp.selectedEnvironments = [
            { id: '22e332', name: 'my environment' },
            { id: '832l', name: 'my environment 2' },
            { id: 'se232', name: 'my environment 3' }
        ];
        comp.removeEnvironmentItem({ id: '832l', name: 'my environment 2' });

        expect(comp.selectedEnvironmentIds).toEqual(['22e332', 'se232']);
    });

    it('should set the selectedEnvironments value when multiselect value changes', () => {
        hostComponentfixture = DOTTestBed.createComponent(TestHostComponent);
        de = hostComponentfixture.debugElement.query(By.css('dot-push-publish-env-selector'));
        const component: PushPublishEnvSelectorComponent = de.componentInstance;
        comp.selectedEnvironmentIds = [];

        spyOn(component, 'writeValue');
        comp.valueChange(new Event('MouseEvent'), [
            {
                id: '12345ab',
                name: 'my environment'
            },
            {
                id: '6789bc',
                name: 'my environment 2'
            }
        ]);
        hostComponentfixture.detectChanges();

        expect<any>(component.writeValue).toHaveBeenCalledWith('');
        expect(comp.selectedEnvironmentIds).toEqual(['12345ab', '6789bc']);
    });

    it('should populate the environments if there is just one option', () => {
        const environment = [
            {
                id: '22e332',
                name: 'my environment'
            }
        ];
        spyOn(pushPublishServiceMock, 'getEnvironments').and.returnValue(observableOf(environment));
        spyOn(comp, 'propagateChange');
        comp.ngOnInit();
        expect(comp.selectedEnvironments).toEqual(environment);
        expect(comp.pushEnvironments).toEqual(environment);
        expect(comp.propagateChange).toHaveBeenCalled();
    });

    it('should populate the environments previously selected by the user', () => {
        spyOnProperty(pushPublishServiceMock, 'lastEnvironmentPushed', 'get').and.returnValue([
            '22e332',
            'joa08'
        ]);
        spyOn(comp, 'propagateChange');
        comp.ngOnInit();
        expect(comp.selectedEnvironments).toEqual([
            {
                id: '22e332',
                name: 'my environment'
            },
            {
                id: 'joa08',
                name: 'my environment 2'
            }
        ]);
        expect(comp.propagateChange).toHaveBeenCalled();
    });

    it('should hide enviroment selector list', () => {
        comp.showList = false;
        fixture.detectChanges();
        expect(document.querySelector('.environment-selector__list')).toBeNull();
    });
});
