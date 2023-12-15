import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditEmaLayoutComponent } from './edit-ema-layout.component';

import { EditEmaStore } from '../dot-ema-shell/store/dot-ema.store';
import { DotPageApiService } from '../services/dot-page-api.service';

describe('EditEmaLayoutComponent', () => {
    let component: EditEmaLayoutComponent;
    let fixture: ComponentFixture<EditEmaLayoutComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [EditEmaLayoutComponent, HttpClientTestingModule],
            providers: [EditEmaStore, DotPageApiService, HttpClient]
        }).compileComponents();

        fixture = TestBed.createComponent(EditEmaLayoutComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
