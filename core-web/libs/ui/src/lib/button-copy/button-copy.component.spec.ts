import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';

import { ButtonCopyComponent } from './button-copy.component';

describe('ButtonCopyComponent', () => {
    let component: ButtonCopyComponent;
    let fixture: ComponentFixture<ButtonCopyComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [ButtonCopyComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(ButtonCopyComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should switch icons when clicked and revert after 1s', fakeAsync(() => {
        const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');

        button.click();
        fixture.detectChanges();

        const icon: HTMLElement = button.querySelector('.pi') as HTMLElement;
        expect(icon.classList).toContain('pi-check');

        tick(1000);
        fixture.detectChanges();

        expect(icon.classList).toContain('pi-copy');
    }));
});
