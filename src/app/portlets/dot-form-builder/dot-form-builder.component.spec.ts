import { async, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { LoginService } from 'dotcms-js';
import { of } from 'rxjs';

import { DOTTestBed } from '@tests/dot-test-bed';
import { DotContentTypesListingModule } from '@portlets/shared/dot-content-types-listing';
import { DotFormBuilderComponent } from './dot-form-builder.component';
import { DotUnlicensedPorletModule } from '@portlets/shared/dot-unlicensed-porlet';
import { LoginServiceMock } from '@tests/login-service.mock';

let routeDatamock = {
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
};
class ActivatedRouteMock {
    get data() {
        return of(routeDatamock);
    }
}

describe('DotFormBuilderComponent', () => {
    let fixture: ComponentFixture<DotFormBuilderComponent>;
    let de: DebugElement;

    beforeEach(
        async(() => {
            DOTTestBed.configureTestingModule({
                declarations: [DotFormBuilderComponent],
                imports: [
                    DotContentTypesListingModule,
                    DotUnlicensedPorletModule,
                    RouterTestingModule
                ],
                providers: [
                    {
                        provide: LoginService,
                        useClass: LoginServiceMock
                    },
                    {
                        provide: ActivatedRoute,
                        useClass: ActivatedRouteMock
                    }
                ]
            }).compileComponents();
        })
    );

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotFormBuilderComponent);
        de = fixture.debugElement;
    });

    it('should show unlicense portlet', () => {
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
        routeDatamock = { unlicensed: null };
        fixture.detectChanges();
        const unlicensed = de.query(By.css('dot-unlicensed-porlet'));
        const contentTypes = de.query(By.css('dot-content-types'));
        expect(unlicensed).toBeNull();
        expect(contentTypes).toBeDefined();
    });
});
