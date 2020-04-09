import { DebugElement } from '@angular/core/src/debug/debug_node';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { async, ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DotAvatarComponent } from './dot-avatar.component';
import { CommonModule } from '@angular/common';
import { Input, Component } from '@angular/core';

@Component({
    selector: 'dot-test-component',
    template: `
        <dot-avatar [url]="url" [label]="label" [size]="size" [showDot]="showDot"> </dot-avatar>
    `
})
class HostTestComponent {
    @Input()
    url: string;

    @Input()
    label: string;

    @Input()
    size = 32;

    @Input()
    showDot = false;
}

describe('DotAvatarComponent', () => {
    let component: DotAvatarComponent;
    let fixture: ComponentFixture<HostTestComponent>;
    let de: DebugElement;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [HostTestComponent, DotAvatarComponent],
            imports: [CommonModule]
        });

        fixture = DOTTestBed.createComponent(HostTestComponent);
        de = fixture.debugElement;
        component = fixture.debugElement.query(By.css('dot-avatar')).componentInstance;
    }));

    it('should not have a image nor dot when url is null', () => {
        fixture.detectChanges();
        expect(de.query(By.css('img'))).toBeNull();
        expect(de.query(By.css('.avatar__dot'))).toBeNull();
    });

    describe('url is set', () => {
        let img;

        beforeEach(() => {
            fixture.componentInstance.url = '/testing_url.png';
            fixture.detectChanges();

            img = de.query(By.css('img'));
        });

        it('should have a image', () => {
            expect(img).not.toBeNull();
        });

        it('should have the right src', () => {
            expect(img.nativeElement.src.endsWith(component.url)).toBe(true);
        });

        it('should set src to null if path is broken', (done) => {
            setTimeout(() => {
                expect(component.url).toBeNull();
                done();
            }, 100);
        });
    });

    describe('prop is set', () => {
        let placeholderDiv;

        beforeEach(() => {
            fixture.componentInstance.label = 'test';
            fixture.detectChanges();

            placeholderDiv = de.query(By.css('.avatar__placeholder'));
        });

        it('should have a placeholderDiv', () => {
            expect(placeholderDiv).not.toBeNull();
        });

        it('should have the placeholderDiv with the right content', () => {
            expect(placeholderDiv.nativeElement.innerText).toBe('T');
        });
    });

    describe('showDot is set', () => {
        let avatarDot;

        beforeEach(() => {
            fixture.componentInstance.showDot = true;
            fixture.detectChanges();

            avatarDot = de.query(By.css('.avatar__dot'));
        });

        it('should have a dotShow span', () => {
            expect(avatarDot).not.toBeNull();
        });
    });

    describe('size', () => {
        describe('default value', () => {
            const defaultStyles = {
                'font-size': '24px',
                height: '32px',
                'line-height': '32px',
                width: '32px'
            };

            it('should have default value size', () => {
                fixture.detectChanges();
                expect(component.size).toBe(32);
            });

            it('should set the right styles to img', () => {
                fixture.componentInstance.url = 'test';
                fixture.detectChanges();

                expect(de.query(By.css('img')).styles).toEqual(defaultStyles);
            });

            it('should set the right styles to avatar__placeholder', () => {
                fixture.componentInstance.label = 'Test';
                fixture.detectChanges();

                expect(de.query(By.css('.avatar__placeholder')).styles).toEqual(defaultStyles);
            });
        });

        describe('set size', () => {
            const styles = {
                'font-size': '15px',
                height: '20px',
                'line-height': '20px',
                width: '20px'
            };

            beforeEach(() => {
                fixture.componentInstance.size = 20;
            });

            it('should set the right styles to img', () => {
                fixture.componentInstance.url = 'test';
                fixture.detectChanges();

                expect(de.query(By.css('img')).styles).toEqual(styles);
            });

            it('should set the right styles to avatar__placeholder', () => {
                fixture.componentInstance.label = 'Test';
                fixture.detectChanges();

                expect(de.query(By.css('.avatar__placeholder')).styles).toEqual(styles);
            });
        });
    });
});
