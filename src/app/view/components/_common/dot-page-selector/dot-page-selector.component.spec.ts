import { of as observableOf, Observable } from 'rxjs';
import { async, ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement, Component, Injectable } from '@angular/core';

import { DotPageSelectorComponent } from './dot-page-selector.component';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotPageSelectorService } from './service/dot-page-selector.service';
import { FormGroup, FormBuilder } from '@angular/forms';
import { AutoComplete, OverlayPanelModule } from 'primeng/primeng';
import { DotDirectivesModule } from '@shared/dot-directives.module';
import { MdInputTextModule } from '@directives/md-inputtext/md-input-text.module';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import {
    DotPageSelectorResults,
    DotPageSeletorItem
} from '@components/_common/dot-page-selector/models/dot-page-selector.models';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock } from '../../../../test/login-service.mock';

export const mockDotPageSelectorResults = {
    type: 'page',
    query: 'about-us',
    data: [
        {
            label: '//demo.dotcms.com/about-us',
            payload: {
                template: '8660b482-1ef6-4d00-9459-3996e703ba19',
                owner: 'dotcms.org.1',
                identifier: 'c12fe7e6-d338-49d5-973b-2d974d57015b',
                friendlyname: 'About Us',
                modDate: '2018-05-21 09:52:38.461',
                cachettl: '0',
                pagemetadata: 'dotCMS',
                languageId: 1,
                title: 'About Us',
                showOnMenu: 'true',
                inode: '5cd3b647-e465-4a6d-a78b-e834a7a7331a',
                seodescription: 'dotCMS Content Management System demo site - About Quest',
                folder: '1049e7fe-1553-4731-bdf9-ba069f1dc08b',
                __DOTNAME__: 'About Us',
                sortOrder: 0,
                path: '/about-us',
                seokeywords: 'dotCMS Content Management System',
                modUser: 'dotcms.org.1',
                host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                lastReview: '2018-05-18 15:30:34.428',
                stInode: 'c541abb1-69b3-4bc5-8430-5e09e5239cc8',
                url: '/about-us/index',
                hostName: 'demo.dotcms.com'
            }
        }
    ]
};

export const mockDotSiteSelectorResults = {
    type: 'site',
    query: 'demo.dotcms.com',
    data: [
        {
            label: '//demo.dotcms.com/',
            isHost: true,
            payload: {
                hostname: 'demo.dotcms.com',
                type: 'host',
                identifier: 's48190c8c-42c4-46af-8d1a-0cd5db894797'
            }
        }
    ]
};

@Injectable()
class MockDotPageSelectorService {
    search(_param: string): Observable<DotPageSelectorResults> {
        return observableOf(mockDotPageSelectorResults);
    }

    getPageById(_param: string): Observable<DotPageSeletorItem> {
        return observableOf(mockDotPageSelectorResults.data[0]);
    }
}
@Component({
    selector: 'dot-fake-form',
    template: `
        <form [formGroup]="form">
            <dot-page-selector
                [floatingLabel]="floatingLabel"
                formControlName="page"
                [style]="{'width': '100%'}"
                label="Hello World">
            </dot-page-selector>
        </form>
    `
})
class FakeFormComponent {
    form: FormGroup;
    floatingLabel = false;

    constructor(private fb: FormBuilder) {
        /*
            This should go in the ngOnInit but I don't want to detectChanges everytime for
            this fake test component
        */
        this.form = this.fb.group({
            page: [{ value: 'c12fe7e6-d338-49d5-973b-2d974d57015b', disabled: false }]
        });
    }
}

const config = host => {
    return {
        declarations: [host, DotPageSelectorComponent],
        imports: [DotDirectivesModule, MdInputTextModule, DotIconButtonModule, OverlayPanelModule],
        providers: [
            { provide: DotPageSelectorService, useClass: MockDotPageSelectorService },
            { provide: LoginService, useClass: LoginServiceMock }
        ]
    };
};
let hostDe: DebugElement;
let component: DotPageSelectorComponent;
let de: DebugElement;
let autocomplete: DebugElement;
let autocompleteComp: AutoComplete;
let dotPageSelectorService: DotPageSelectorService;

fdescribe('DotPageSelectorComponent', () => {
    let hostFixture: ComponentFixture<FakeFormComponent>;
    const searchObj = { originalEvent: { target: { value: 'demo' } }, query: 'demo' };
    const specialSearchObj = { originalEvent: { target: { value: 'd#emo$%' } }, query: 'd#emo$%' };

    beforeEach(
        async(() => {
            DOTTestBed.configureTestingModule(config(FakeFormComponent)).compileComponents();
        })
    );

    beforeEach(() => {
        hostFixture = DOTTestBed.createComponent(FakeFormComponent);
        hostDe = hostFixture.debugElement;
        de = hostDe.query(By.css('dot-page-selector'));
        component = de.componentInstance;
        dotPageSelectorService = de.injector.get(DotPageSelectorService);

        spyOn(component.selected, 'emit');
        spyOn(dotPageSelectorService, 'search').and.callThrough();
        spyOn(component, 'writeValue').and.callThrough();

        hostFixture.detectChanges();
        autocomplete = de.query(By.css('p-autoComplete'));
        autocompleteComp = autocomplete.componentInstance;
    });

    it('should have autocomplete', () => {
        expect(autocomplete).toBeTruthy();
    });

    it('shold not set floating label directive', () => {
        expect(de.query(By.css('[dotMdInputtext]')) === null).toBe(true);
    });

    it('should search for pages', () => {
        autocomplete.triggerEventHandler('completeMethod', searchObj);
        expect(dotPageSelectorService.search).toHaveBeenCalledWith(searchObj.query);
    });

    it('should remove special characters when searching for pages', () => {
        autocomplete.triggerEventHandler('completeMethod', specialSearchObj);
        expect(dotPageSelectorService.search).toHaveBeenCalledWith('demo');
    });

    it('should pass attrs to autocomplete component', () => {
        expect(autocompleteComp.style).toEqual({ width: '100%' });
        expect(autocompleteComp.field).toEqual('Hello World');
    });

    describe('ControlValueAccessor', () => {
        beforeEach(() => {
            spyOn(component, 'propagateChange').and.callThrough();
        });

        it('should emit selected item and propagate changes', () => {
            component.results = mockDotPageSelectorResults;
            autocomplete.triggerEventHandler('onSelect', mockDotPageSelectorResults.data[0]);
            expect(component.selected.emit).toHaveBeenCalledWith(mockDotPageSelectorResults.data[0].payload);
            expect(component.propagateChange).toHaveBeenCalledWith(mockDotPageSelectorResults.data[0].payload.identifier);
        });

        it('should write value', () => {
            expect(component.writeValue).toHaveBeenCalledWith(
                'c12fe7e6-d338-49d5-973b-2d974d57015b'
            );
            expect(component.val.payload.identifier).toEqual(
                'c12fe7e6-d338-49d5-973b-2d974d57015b'
            );
        });

        it('should clear model and suggections', () => {
            autocomplete.triggerEventHandler('onClear', {});
            expect(component.propagateChange).toHaveBeenCalledWith(null);
            expect(component.results.data).toEqual([]);
        });
    });

    describe('floating label', () => {
        beforeEach(() => {
            component.floatingLabel = true;
            hostFixture.detectChanges();
            autocomplete = de.query(By.css('p-autoComplete'));
            autocompleteComp = autocomplete.componentInstance;
        });

        it('should set floating label directive', () => {
            const span: DebugElement = de.query(By.css('[dotMdInputtext]'));
            expect(span.componentInstance.label).toBe('Hello World');
            expect(span).toBeTruthy();
        });

        it('should not have placeholder', () => {
            expect(autocompleteComp.placeholder).toBeUndefined();
        });
    });
});
