import { of, throwError } from 'rxjs';

import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { AvatarModule } from 'primeng/avatar';

import { DotGravatarService } from '@dotcms/app/api/services/dot-gravatar-service';

import { DotGravatarDirective } from './dot-gravatar.directive';

@Component({
    template: `
        <p-avatar [email]="email" dotGravatar></p-avatar>
    `
})
class TestHostComponent {
    //Some dummy email from this post https://stackoverflow.com/questions/41786225/what-is-a-good-gravatar-example-email
    email = 'jitewaboh@lagify.com';
}

describe('DotGravatarDirective', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let component: TestHostComponent;
    let element: DebugElement;

    const testEmail = 'test@test.com';
    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent],
            imports: [AvatarModule, DotGravatarDirective],
            providers: [
                {
                    provide: DotGravatarService,
                    useValue: {
                        getPhoto: (email: string) => {
                            return email != testEmail
                                ? of(
                                      'https://www.gravatar.com/avatar/09abd59eb5653a7183ba812b8261f48b'
                                  )
                                : throwError(null);
                        }
                    }
                }
            ]
        });
        fixture = TestBed.createComponent(TestHostComponent);
        component = fixture.componentInstance;
        element = fixture.debugElement;
    });

    it('should show an image when a valid email is provided', () => {
        fixture.detectChanges();
        expect(element.query(By.css('img'))).toBeTruthy();
    });

    it('should fallback to label when email is not found as gravatar user', () => {
        component.email = testEmail;
        fixture.detectChanges();

        expect(element.query(By.css('.p-avatar-text')).nativeElement.textContent).toBe(
            testEmail.charAt(0).toUpperCase()
        );
    });
});
