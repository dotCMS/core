import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, DebugElement, Input } from '@angular/core';
import { By } from '@angular/platform-browser';

import { of } from 'rxjs';

import { DotFormBuilderComponent } from './dot-form-builder.component';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: 'dot-unlicensed-porlet',
    template: ''
})
export class DotUnlicensedPorletComponentMock {
    @Input() data;

    constructor() {}
}
@Component({
    selector: 'dot-content-types',
    template: ''
})
export class DotContentTypesPortletComponentMock {
    constructor() {}
}

describe('DotFormBuilderComponent', () => {
    let fixture: ComponentFixture<DotFormBuilderComponent>;
    let de: DebugElement;
    let router: ActivatedRoute;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [
                    DotFormBuilderComponent,
                    DotUnlicensedPorletComponentMock,
                    DotContentTypesPortletComponentMock
                ],
                providers: [
                    {
                        provide: ActivatedRoute,
                        useValue: {
                            get data() {
                                return '';
                            }
                        }
                    }
                ]
            });
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotFormBuilderComponent);
        de = fixture.debugElement;
        router = TestBed.inject(ActivatedRoute);
    });

    it('should show unlicense portlet', () => {
        spyOnProperty(router, 'data').and.returnValue(
            of({
                unlicensed: {
                    title: 'Title',
                    description: 'Description',
                    links: [
                        {
                            text: 'text',
                            link: 'link'
                        }
                    ]
                }
            })
        );
        fixture.detectChanges();
        const unlicensed = de.query(By.css('dot-unlicensed-porlet'));
        const contentTypes = de.query(By.css('dot-content-types'));
        expect(unlicensed.componentInstance.data).toEqual({
            title: 'Title',
            description: 'Description',
            links: [
                {
                    text: 'text',
                    link: 'link'
                }
            ]
        });
        expect(contentTypes).toBeNull();
    });

    it('should show dot-content-types', () => {
        spyOnProperty(router, 'data').and.returnValue(
            of({
                unlicensed: null
            })
        );
        fixture.detectChanges();
        const unlicensed = de.query(By.css('dot-unlicensed-porlet'));
        const contentTypes = de.query(By.css('dot-content-types'));
        expect(unlicensed).toBeNull();
        expect(contentTypes).toBeDefined();
    });
});
