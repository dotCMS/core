import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { TemplateContainersCacheService } from './../../../../template-containers-cache.service';
import { DotEditLayoutService } from './../../../../shared/services/dot-edit-layout.service';
import { DotEditLayoutGridModule } from './../../../components/dot-edit-layout-grid/dot-edit-layout-grid.module';
import { DotActionButtonModule } from './../../../../../../view/components/_common/dot-action-button/dot-action-button.module';
import { mockDotRenderedPage } from '../../../../../../test/dot-page-render.mock';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

import { DotLayoutDesignerComponent } from './dot-layout-designer.component';
import { FormBuilder } from '@angular/forms';
import { DOTTestBed } from '../../../../../../test/dot-test-bed';
import { DotSidebarPropertiesModule } from '../../../components/dot-sidebar-properties/dot-sidebar-properties.module';
import { LoginServiceMock } from '../../../../../../test/login-service.mock';
import { LoginService } from 'dotcms-js';
import { DotEditLayoutSidebarModule } from '../../../components/dot-edit-layout-sidebar/dot-edit-layout-sidebar.module';
import { MockDotMessageService } from '../../../../../../test/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

describe('DotLayoutDesignerComponent', () => {
    let component: DotLayoutDesignerComponent;
    let fixture: ComponentFixture<DotLayoutDesignerComponent>;

    beforeEach(async(() => {
        const messageServiceMock = new MockDotMessageService({
            'editpage.layout.designer.header': 'HEADER',
            'editpage.layout.designer.footer': 'FOOTER'
        });

        DOTTestBed.configureTestingModule({
            imports: [
                DotSidebarPropertiesModule,
                DotActionButtonModule,
                DotEditLayoutGridModule,
                BrowserAnimationsModule,
                DotEditLayoutSidebarModule
            ],
            declarations: [DotLayoutDesignerComponent],
            providers: [
                DotEditLayoutService,
                TemplateContainersCacheService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotLayoutDesignerComponent);
        component = fixture.componentInstance;
    });

    describe('default', () => {
        beforeEach(() => {
            component.group = new FormBuilder().group({
                ...mockDotRenderedPage.layout,
                sidebar: new FormBuilder().group({
                    location: '',
                    containers: [],
                    width: 'small'
                })
            });
            fixture.detectChanges();
        });

        it('should NOT show header in the template', () => {
            const headerElem: DebugElement = fixture.debugElement.query(
                By.css('.dot-layout-designer__header')
            );
            expect(headerElem).toBe(null);
        });

        it('should NOT show footer in the template', () => {
            const footerElem: DebugElement = fixture.debugElement.query(
                By.css('.dot-layout-designer__footer')
            );
            expect(footerElem).toBe(null);
        });

        it('should NOT show a sidebar', () => {
            const sidebar: DebugElement = fixture.debugElement.query(
                By.css('[class^="dot-layout-designer__sidebar"]')
            );
            expect(sidebar).toBe(null);
        });

        describe('dot-edit-layout-grid', () => {
            let gridLayout: DebugElement;

            beforeEach(() => {
                gridLayout = fixture.debugElement.query(By.css('dot-edit-layout-grid'));
            });

            it('should show dot-edit-layout-grid', () => {
                expect(gridLayout).toBeTruthy();
            });

            it('should pass body as form control', () => {
                expect(gridLayout.attributes.formControlName).toBe('body');
            });
        });
    });

    describe('filled', () => {
        describe('header and footer', () => {
            beforeEach(() => {
                component.group = new FormBuilder().group({
                    header: true,
                    footer: true,
                    body: mockDotRenderedPage.layout.body,
                    sidebar: {
                        location: '',
                        containers: [],
                        width: 'small'
                    }
                });
                fixture.detectChanges();
            });

            it('should show header in the template', () => {
                const headerElem: DebugElement = fixture.debugElement.query(
                    By.css('.dot-layout-designer__header')
                );
                expect(headerElem).toBeTruthy();
            });

            it('should show footer in the template', () => {
                const footerElem: DebugElement = fixture.debugElement.query(
                    By.css('.dot-layout-designer__footer')
                );
                expect(footerElem).toBeTruthy();
            });

            it('should have the right label for the Header', () => {
                const headerSelector = fixture.debugElement.query(
                    By.css('.dot-layout-designer__header')
                );
                expect(headerSelector.nativeElement.outerText).toBe('HEADER');
            });

            it('should have the right label for the Footer', () => {
                const headerSelector = fixture.debugElement.query(
                    By.css('.dot-layout-designer__footer')
                );
                expect(headerSelector.nativeElement.outerText).toBe('FOOTER');
            });

            describe('sidebar size and position', () => {
                beforeEach(() => {
                    component.group = new FormBuilder().group({
                        ...mockDotRenderedPage.layout,
                        sidebar: {
                            location: 'left',
                            containers: [],
                            width: 'small'
                        }
                    });
                    fixture.detectChanges();
                });

                it('should show', () => {
                    const sidebar: DebugElement = fixture.debugElement.query(
                        By.css('.dot-layout-designer__sidebar--left')
                    );
                    expect(sidebar).toBeTruthy();
                });

                it('should show sidebar position correctly', () => {
                    const positions = ['left', 'right'];
                    positions.forEach((position) => {
                        component.group.get('sidebar').value.location = position;
                        fixture.detectChanges();
                        const sidebar: DebugElement = fixture.debugElement.query(
                            By.css(`.dot-layout-designer__sidebar--${position}`)
                        );
                        expect(sidebar).toBeTruthy(position);
                    });
                });

                it('it should set sidebar size correctly', () => {
                    const sizes = ['small', 'medium', 'large'];

                    sizes.forEach((size) => {
                        component.group.get('sidebar').value.width = size;
                        fixture.detectChanges();
                        const sidebar: DebugElement = fixture.debugElement.query(
                            By.css(`.dot-layout-designer__sidebar--${size}`)
                        );
                        expect(sidebar).toBeTruthy(size);
                    });
                });
            });
        });
    });
});
