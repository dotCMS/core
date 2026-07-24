import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { SuggestionsListItemComponent } from './suggestions-list-item.component';

const DATA_URL_ICON =
    'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzIiIGhlaWdodD0iMzIiIHZpZXdCb3g9IjAgMCAzMiAzMiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48L3N2Zz4K';

describe('SuggestionsListItemComponent', () => {
    let component: SuggestionsListItemComponent;
    let fixture: ComponentFixture<SuggestionsListItemComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [SuggestionsListItemComponent],
            teardown: { destroyAfterEach: false }
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(SuggestionsListItemComponent);
        component = fixture.componentInstance;
    });

    it('should create', () => {
        fixture.detectChanges();
        expect(component).toBeTruthy();
    });

    it('should render a trusted data URL icon without unsafe prefix', () => {
        component.url = DATA_URL_ICON;
        fixture.detectChanges();

        const img = fixture.debugElement.query(By.css('img'));
        const src = img.nativeElement.getAttribute('src') ?? img.nativeElement.src;

        expect(img).toBeTruthy();
        expect(src).toContain('data:image/svg+xml');
        expect(src).not.toContain('unsafe:');
    });

    it('should render a material icon when url is a material symbol name', () => {
        component.url = 'receipt';
        fixture.detectChanges();

        const materialIcon = fixture.debugElement.query(By.css('.material-symbols-outlined'));

        expect(materialIcon).toBeTruthy();
        expect(materialIcon.nativeElement.textContent.trim()).toBe('receipt');
        expect(fixture.debugElement.query(By.css('img'))).toBeNull();
    });
});
