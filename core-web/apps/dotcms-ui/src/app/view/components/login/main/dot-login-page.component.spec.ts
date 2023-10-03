import { of } from 'rxjs';

import { Injectable } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { DotLoginPageComponent } from '@components/login/main/dot-login-page.component';
import { DotLoginPageStateService } from '@components/login/shared/services/dot-login-page-state.service';
import { DotFieldValidationMessageComponent } from '@dotcms/ui';
import { mockLoginFormResponse } from '@dotcms/utils-testing';

@Injectable()
class MockLoginPageStateService {
    get = jasmine.createSpy('get').and.returnValue(of(mockLoginFormResponse));
}

describe('DotLoginPageComponent', () => {
    let fixture: ComponentFixture<DotLoginPageComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotLoginPageComponent],
            imports: [
                BrowserAnimationsModule,
                FormsModule,
                ButtonModule,
                InputTextModule,
                DotFieldValidationMessageComponent,
                RouterTestingModule
            ],
            providers: [
                {
                    provide: DotLoginPageStateService,
                    useClass: MockLoginPageStateService
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotLoginPageComponent);
        fixture.detectChanges();
    });

    it('should set the background Image and background color', () => {
        expect(document.body.style.backgroundColor).toEqual('rgb(58, 56, 71)');
        expect(document.body.style.backgroundImage).toEqual(
            'url("/html/images/backgrounds/bg-11.jpg")'
        );
    });
});
