import { DotLoginPageComponent } from '@components/login/main/dot-login-page.component';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { mockLoginFormResponse } from '@tests/login-service.mock';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/primeng';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { of } from 'rxjs';
import { DotLoginPageStateService } from '@components/login/shared/services/dot-login-page-state.service';
import { RouterTestingModule } from '@angular/router/testing';
import { Injectable } from '@angular/core';

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
                DotFieldValidationMessageModule,
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
