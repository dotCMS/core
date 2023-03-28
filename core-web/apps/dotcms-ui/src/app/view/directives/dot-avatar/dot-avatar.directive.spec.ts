import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { AvatarModule } from 'primeng/avatar';

import { DotAvatarDirective } from './dot-avatar.directive';

@Component({
    template: `<p-avatar [label]="label" [image]="image" dotAvatar></p-avatar> `
})
class TestHostComponent {
    image: string;
    label = 'test';
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

        fixture.detectChanges();
    });

    it('should show the first letter of the label', () => {
        expect(element.query(By.css('.p-avatar-text')).nativeElement.textContent).toBe('T');
    });
    it("should show an image when it's provided", () => {
        component.image = 'https://dummyimage.com/600x400/000/fff';
        fixture.detectChanges();

        expect(element.query(By.css('img'))).toBeTruthy();
    });
    it('should remove the p-avatar-image class if the image is broken', (done) => {
        component.image = 'test';
        fixture.detectChanges();

        setTimeout(() => {
            fixture.detectChanges();
            expect(element.query(By.css('.p-avatar-image'))).toBeFalsy();
            done();
        }, 100);
    });
    it('should show the first letter of the label if the image is broken', (done) => {
        component.image = 'test';
        fixture.detectChanges();
        setTimeout(() => {
            fixture.detectChanges();
            expect(element.query(By.css('.p-avatar-text')).nativeElement.textContent).toBe('T');
            done();
        }, 100);
    });
    it('should set label to unknow when label is not provided', (done) => {
        component.image = 'test';
        component.label = undefined;
        fixture.detectChanges();
        setTimeout(() => {
            fixture.detectChanges();
            expect(element.query(By.css('.p-avatar-text')).nativeElement.textContent).toBe('U');
            done();
        }, 100);
    });
});
