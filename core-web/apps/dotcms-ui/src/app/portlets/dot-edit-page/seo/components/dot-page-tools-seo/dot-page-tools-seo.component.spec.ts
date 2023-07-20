import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotPageToolsSeoComponent } from './dot-page-tools-seo.component';

describe('DotPageToolsSeoComponent', () => {
    let component: DotPageToolsSeoComponent;
    let fixture: ComponentFixture<DotPageToolsSeoComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotPageToolsSeoComponent, HttpClientTestingModule]
        }).compileComponents();

        fixture = TestBed.createComponent(DotPageToolsSeoComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
