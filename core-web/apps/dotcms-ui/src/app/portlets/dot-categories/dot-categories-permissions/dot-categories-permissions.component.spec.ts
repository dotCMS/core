import { Component, DebugElement, ElementRef, Input, SimpleChange, ViewChild } from '@angular/core';
import { ComponentFixture, ComponentFixtureAutoDetect, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DotCategoriesPermissionsComponent } from './dot-categories-permissions.component';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';

@Component({
    selector: 'dot-iframe',
    template: ''
})
export class IframeMockComponent {
    @Input() src: string;
    @ViewChild('iframeElement') iframeElement: ElementRef;
}

describe('CategoriesPermissionsComponent', () => {
    let component: DotCategoriesPermissionsComponent;
    let fixture: ComponentFixture<DotCategoriesPermissionsComponent>;
    let de: DebugElement;
    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotCategoriesPermissionsComponent, IframeMockComponent],
            imports: [DotPortletBaseModule],
            providers: [{ provide: ComponentFixtureAutoDetect, useValue: true }]
        }).compileComponents();

        fixture = TestBed.createComponent(DotCategoriesPermissionsComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        component.categoryId = '123';
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('permissions', () => {
        beforeEach(() => {
            component.categoryId = '123';
            component.ngOnChanges({
                categoryId: new SimpleChange(null, component.categoryId, true)
            });
            fixture.detectChanges();
            de = fixture.debugElement;
        });

        it('should set iframe permissions url', () => {
            const permissions = de.query(By.css('[data-testId="permissionsIframe"]'));
            expect(permissions.componentInstance.src).toBe(
                '/html/categories/permissions.jsp?categoryId=123'
            );
        });
    });
});
