import { ComponentFixture } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { DotNavIconComponent } from './dot-nav-icon.component';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotIconModule } from '../../../_common/dot-icon/dot-icon.module';
import { DotIconComponent } from '../../../_common/dot-icon/dot-icon.component';
import { By } from '@angular/platform-browser';

describe('DotNavIconComponent', () => {
    let comp: DotNavIconComponent;
    let fixture: ComponentFixture<DotNavIconComponent>;
    let de: DebugElement;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotNavIconComponent],
            imports: [DotIconModule]
        });

        fixture = DOTTestBed.createComponent(DotNavIconComponent);
        de = fixture.debugElement;
        comp = fixture.componentInstance;
    });

    it('should have dot-icon', () => {
        comp.icon = 'test';
        fixture.detectChanges();
        const dotIconComponent: DotIconComponent = de.query(By.css('dot-icon')).componentInstance;
        expect(dotIconComponent).toBeDefined();
        expect(dotIconComponent.size).toBe('18');
    });

    it('should have font awesome icon', () => {
        comp.icon = 'fa-test';
        fixture.detectChanges();
        const faIcon = de.query(By.css('.fa'));
        expect(faIcon.componentInstance).toBeDefined();
        expect(faIcon.nativeElement.style['font-size']).toBe('18px');
    });
});
