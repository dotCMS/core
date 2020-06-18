import { ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../../test/dot-test-bed';
import { Component, Input } from '@angular/core';
import { MockDotMessageService } from '../../../../../../test/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { By } from '@angular/platform-browser';
import { MenuItem } from 'primeng/components/common/menuitem';
import { DotTemplateAdditionalActionsMenuComponent } from './dot-template-additional-actions-menu.component';

@Component({
    // tslint:disable-next-line:component-selector
    selector: 'p-menu',
    template: ''
})
class MockPrimeNGMenuComponent {
    @Input()
    model: MenuItem[];
    @Input()
    popup: boolean;
}

describe('DotLegacyAdditionalActionsMenuComponent', () => {
    let component: DotTemplateAdditionalActionsMenuComponent;
    let fixture: ComponentFixture<DotTemplateAdditionalActionsMenuComponent>;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'template.action.additional.permissions': 'permissions',
            'template.action.additional.history': 'history',
            'template.action.additional.properties': 'properties'
        });

        DOTTestBed.configureTestingModule({
            declarations: [DotTemplateAdditionalActionsMenuComponent, MockPrimeNGMenuComponent],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });

        fixture = DOTTestBed.createComponent(DotTemplateAdditionalActionsMenuComponent);
        component = fixture.componentInstance;
        component.inode = '1';
    });

    it('should has a p-menu', () => {
        const pMenu = fixture.debugElement.query(By.css('p-menu'));

        expect(pMenu).toBeDefined();
        expect(component.items).toBe(pMenu.componentInstance.model);
    });

    it('should has a button', () => {
        const button = fixture.debugElement.query(By.css('button'));
        expect(button).toBeDefined();
    });

    it('should has a items attributes', () => {
        const itemsExpected = [
            {
                label: 'properties',
                routerLink: 'template/1/properties'
            },
            {
                label: 'permissions',
                routerLink: 'template/1/permissions'
            },
            {
                label: 'history',
                routerLink: 'template/1/history'
            }
        ];

        fixture.detectChanges();

        expect(component.items).toEqual(itemsExpected);
    });
});
