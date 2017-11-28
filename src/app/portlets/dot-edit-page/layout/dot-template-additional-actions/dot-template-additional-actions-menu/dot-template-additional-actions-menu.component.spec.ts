import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { IFrameModule } from '../../../../../view/components/_common/iframe/iframe.module';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { Component, Input } from '@angular/core';
import { MockMessageService } from '../../../../../test/message-service.mock';
import { MessageService } from '../../../../../api/services/messages-service';
import { By } from '@angular/platform-browser';
import { MenuItem } from 'primeng/components/common/menuitem';
import { DotTemplateAdditionalActionsMenuComponent } from './dot-template-additional-actions-menu.component';

@Component({
    selector: 'p-menu',
    template: ''
})
class MockPrimeNGMenuComponent {
    @Input() model: MenuItem[];
    @Input() popup: boolean;
}

describe('DotLegacyAdditionalActionsMenuComponent', () => {

    let component: DotTemplateAdditionalActionsMenuComponent;
    let fixture: ComponentFixture<DotTemplateAdditionalActionsMenuComponent>;

    beforeEach(() => {

        const messageServiceMock = new MockMessageService({
            'template.action.additional.permissions': 'permissions',
            'template.action.additional.history': 'history',
            'template.action.additional.properties': 'properties'
        });

        DOTTestBed.configureTestingModule({
            declarations: [
                DotTemplateAdditionalActionsMenuComponent,
                MockPrimeNGMenuComponent
            ],
            providers: [
                { provide: MessageService, useValue: messageServiceMock }
            ]
        });

        fixture = DOTTestBed.createComponent(DotTemplateAdditionalActionsMenuComponent);
        component = fixture.componentInstance;
        component.templateId = '1';
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
