import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import {
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { dotcmsContentTypeFieldBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { ContentTypeFieldsTabComponent } from '.';

import { DOTTestBed } from '../../../../../../test/dot-test-bed';

const tabField: DotCMSContentTypeField = {
    ...dotcmsContentTypeFieldBasicMock,
    clazz: DotCMSClazzes.TAB_DIVIDER,
    name: 'fieldTab-1'
};
const mockFieldTab: DotCMSContentTypeLayoutRow = {
    divider: tabField
};

@Component({
    selector: 'dot-test-host',
    template: '<dot-content-type-fields-tab [fieldTab]="data"></dot-content-type-fields-tab>',
    standalone: false
})
class DotTestHostComponent {
    data: DotCMSContentTypeLayoutRow;

    setData(data: DotCMSContentTypeLayoutRow): void {
        this.data = data;
    }
}

describe('ContentTypeFieldsTabComponent', () => {
    let hostFixture: ComponentFixture<DotTestHostComponent>;
    let hostDe: DebugElement;
    let hostComp: DotTestHostComponent;

    let comp: ContentTypeFieldsTabComponent;
    let de: DebugElement;
    let dotDialogService: DotAlertConfirmService;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.action.delete': 'delete text',
        'contenttypes.confirm.message.delete.field': 'delete confirm text',
        'contenttypes.content.field': 'field text',
        'contenttypes.action.cancel': 'cancel text'
    });

    beforeEach(waitForAsync(() => {
        DOTTestBed.configureTestingModule({
            declarations: [ContentTypeFieldsTabComponent, DotTestHostComponent],
            imports: [TooltipModule, ButtonModule, DotMessagePipe],
            providers: [
                DotAlertConfirmService,
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ]
        });

        hostFixture = DOTTestBed.createComponent(DotTestHostComponent);
        hostComp = hostFixture.componentInstance;
        hostDe = hostFixture.debugElement;
        de = hostDe.query(By.css('dot-content-type-fields-tab'));
        comp = de.componentInstance;
        dotDialogService = de.injector.get(DotAlertConfirmService);
    }));

    beforeEach(() => {
        hostComp.setData(mockFieldTab);
        hostFixture.detectChanges();
    });

    it('should render component', () => {
        const deleteBtn = de.query(By.css('p-button')).componentInstance;
        const labelInput = de.query(By.css('div')).nativeElement;

        expect(deleteBtn).toBeTruthy();
        expect(labelInput.outerHTML).toContain(tabField.name);
    });

    it('should emit change evt with onBlur & keyUp.enter', () => {
        spyOn(comp.editTab, 'emit');
        const preventDefaultSpy = jasmine.createSpy('spy');
        const stopPropagationSpy = jasmine.createSpy('spy');
        const labelInput = de.query(By.css('.tab__label'));

        labelInput.triggerEventHandler('keydown.enter', {
            preventDefault: preventDefaultSpy,
            stopPropagation: stopPropagationSpy,
            target: {
                textContent: 'hello world'
            }
        });

        expect(comp.editTab.emit).toHaveBeenCalledWith({
            ...tabField,
            name: 'hello world'
        });

        labelInput.triggerEventHandler('blur', {
            preventDefault: preventDefaultSpy,
            stopPropagation: stopPropagationSpy,
            target: {
                textContent: 'hello world changed'
            }
        });

        expect(comp.editTab.emit).toHaveBeenCalledWith({
            ...tabField,
            name: 'hello world changed'
        });

        expect(preventDefaultSpy).toHaveBeenCalledTimes(2);
        expect(stopPropagationSpy).toHaveBeenCalledTimes(2);
    });

    it('should emit delete evt', () => {
        spyOn(dotDialogService, 'confirm').and.callFake((conf) => {
            conf.accept();
        });
        spyOn(comp.removeTab, 'emit');
        const deleteButton = de.query(By.css('p-button')).nativeElement;
        deleteButton.click();
        expect(comp.removeTab.emit).toHaveBeenCalledWith(mockFieldTab);
    });
});
