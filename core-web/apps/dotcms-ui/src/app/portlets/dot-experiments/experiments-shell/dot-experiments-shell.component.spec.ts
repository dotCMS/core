import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotExperimentsShellComponent } from './dot-experiments-shell.component';
import { of } from 'rxjs';
import { LoadingState } from '@portlets/shared/models/shared-models';
import { DotExperimentsStore } from '../shared/stores/dot-experiments-store.service';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

const storeMock = {
    get getState$() {
        return of(LoadingState.INIT);
    }
};

describe('DotExperimentsShellComponent', () => {
    let component: DotExperimentsShellComponent;
    let fixture: ComponentFixture<DotExperimentsShellComponent>;
    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotExperimentsShellComponent],
            providers: [
                {
                    provide: DotExperimentsStore,
                    useValue: storeMock
                }
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();

        fixture = TestBed.createComponent(DotExperimentsShellComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
