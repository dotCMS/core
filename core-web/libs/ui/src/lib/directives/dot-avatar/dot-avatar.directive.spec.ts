import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { AvatarModule } from 'primeng/avatar';

import { DotAvatarDirective } from './dot-avatar.directive';

@Component({
    template: `
        <p-avatar [text]="text" [image]="image" dotAvatar></p-avatar>
    `
})
class TestHostComponent {
    text: string;
    image: string;
}

describe('DotAvatarDirective', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let component: TestHostComponent;
    let element: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent],
            imports: [AvatarModule, DotAvatarDirective]
        });
        fixture = TestBed.createComponent(TestHostComponent);
        component = fixture.componentInstance;
        element = fixture.debugElement;
    });

    it('should show an image when the image is not broken', () => {
        component.image = 'https://dummyimage.com/600x400/000/fff';
        component.text = 'test';
        fixture.detectChanges();

        expect(element.query(By.css('img'))).toBeTruthy();
    });
    xit('should fallback to label when the image is broken', (done) => {
        component.image = 'https/dumyimage.om600x400/000/fff';
        component.text = 'test';
        fixture.detectChanges();

        fixture.whenStable().then(() => {
            setTimeout(() => {
                fixture.detectChanges();
                expect(element.query(By.css('p-avatar')).nativeElement.textContent).toBe(
                    component.text.charAt(0).toUpperCase()
                );
                done();
            }, 100);
        });
    });
});
