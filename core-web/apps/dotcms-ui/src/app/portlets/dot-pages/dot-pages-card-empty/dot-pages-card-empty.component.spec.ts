import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotPagesCardEmptyComponent } from './dot-pages-card-empty.component';
import { CommonModule } from '@angular/common';
import { SkeletonModule } from 'primeng/skeleton';

describe('DotPagesCardEmptyComponent', () => {
    let fixture: ComponentFixture<DotPagesCardEmptyComponent>;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [CommonModule, DotIconModule, DotPipesModule, SkeletonModule],
            declarations: [DotPagesCardEmptyComponent]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotPagesCardEmptyComponent);
        fixture.detectChanges();
    });

    describe('Init', () => {
        it('should have header and body with star icon and skeleton', () => {
            expect(
                fixture.debugElement.query(By.css('[data-testid="favoriteCardEmptyHeader"]'))
            ).toBeTruthy();
            expect(fixture.debugElement.query(By.css('dot-icon')).attributes.name).toBe('star');
            expect(fixture.debugElement.queryAll(By.css('p-skeleton')).length).toBe(2);
        });
    });
});
