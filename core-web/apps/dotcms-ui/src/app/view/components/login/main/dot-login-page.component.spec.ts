import { of } from 'rxjs';

import { Injectable } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { DotFieldValidationMessageComponent } from '@dotcms/ui';
import { mockLoginFormResponse } from '@dotcms/utils-testing';

import { DotLoginPageComponent } from './dot-login-page.component';

import { DotLoginPageStateService } from '../shared/services/dot-login-page-state.service';

@Injectable()
class MockLoginPageStateService {
    get = jest.fn().mockReturnValue(of(mockLoginFormResponse));
}

describe('DotLoginPageComponent', () => {
    let fixture: ComponentFixture<DotLoginPageComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                DotLoginPageComponent,
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

    it('should set the background image, color, and layout styles on body', () => {
        expect(['#3a3847', 'rgb(58, 56, 71)']).toContain(document.body.style.backgroundColor);
        expect(document.body.style.backgroundImage).toContain('/html/images/backgrounds/bg-11.jpg');
        expect(document.body.style.backgroundPosition).toEqual('top center');
        expect(document.body.style.backgroundRepeat).toEqual('no-repeat');
        expect(document.body.style.backgroundSize).toEqual('cover');
    });
});
