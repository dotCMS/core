import { async, ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement, Component, Injectable } from '@angular/core';

import { DotPageSelectorComponent } from './dot-page-selector.component';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotPageSelectorService, DotPageAsset } from './service/dot-page-selector.service';
import { FormGroup, FormBuilder } from '@angular/forms';
import { AutoComplete } from 'primeng/primeng';
import { DotDirectivesModule } from '../../../../shared/dot-directives.module';
import { Observable } from 'rxjs/Observable';

export const mockPageSelector = [
    {
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
        seokeywords: 'dotCMS Content Management System',
        modUser: 'dotcms.org.1',
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        lastReview: '2018-05-18 15:30:34.428',
        stInode: 'c541abb1-69b3-4bc5-8430-5e09e5239cc8',
        url: '/about-us/index'
    },
    {
        template: 'fdb3f906-e9c4-46c4-b7e4-148201271d04',
        modDate: '2015-02-02 20:04:24.499',
        cachettl: '15',
        title: 'Location Detail',
        httpsreq: '',
        showOnMenu: '',
        inode: '56ceec3a-6e57-4158-b1f0-d8de535e0238',
        __DOTNAME__: 'Location Detail',
        seokeywords: '',
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        lastReview: '2015-02-02 20:04:24.49',
        stInode: 'c541abb1-69b3-4bc5-8430-5e09e5239cc8',
        owner: 'dotcms.org.1',
        identifier: 'fdeb07ff-6fc3-4237-91d9-728109bc621d',
        friendlyname: 'Location Detail',
        redirecturl: '',
        pagemetadata: '',
        languageId: 1,
        url: '/about-us/locations/location-detail',
        seodescription: '',
        folder: 'd19a2815-1037-4a17-bce5-7a36eeaa8d54',
        sortOrder: 0,
        modUser: 'dotcms.org.1'
    }
];

@Injectable()
class MockDotPageSelectorService {
    getPagesInFolder(_folder: string): Observable<DotPageAsset[]> {
        return Observable.of(mockPageSelector);
    }
    getPage(idenfier: string): Observable<DotPageAsset> {
        return Observable.of(mockPageSelector[0]);
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

const config = (host) => {
    return {
        declarations: [host, DotPageSelectorComponent],
        imports: [DotDirectivesModule],
        providers: [{ provide: DotPageSelectorService, useClass: MockDotPageSelectorService }]
    };
};
let hostDe: DebugElement;
let component: DotPageSelectorComponent;
let de: DebugElement;
let autocomplete: DebugElement;
let autocompleteComp: AutoComplete;
let dotPageSelectorService: DotPageSelectorService;

describe('DotPageSelectorComponent', () => {
    let hostFixture: ComponentFixture<FakeFormComponent>;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule(config(FakeFormComponent)).compileComponents();
    }));

    beforeEach(() => {
        hostFixture = DOTTestBed.createComponent(FakeFormComponent);
        hostDe = hostFixture.debugElement;
        de = hostDe.query(By.css('dot-page-selector'));
        component = de.componentInstance;
        dotPageSelectorService = de.injector.get(DotPageSelectorService);

        spyOn(component.selected, 'emit');
        spyOn(dotPageSelectorService, 'getPagesInFolder').and.callThrough();
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
        autocomplete.triggerEventHandler('completeMethod', {
            query: 'hello'
        });

        expect(dotPageSelectorService.getPagesInFolder).toHaveBeenCalledWith('hello');
    });

    it('should pass attrs to autocomplete component', () => {
        expect(autocompleteComp.style).toEqual({ width: '100%' });
        expect(autocompleteComp.placeholder).toEqual('Hello World');
    });

    describe('ControlValueAccessor', () => {
        beforeEach(() => {
            spyOn(component, 'propagateChange').and.callThrough();
        });

        it('should emit selected item and propagate changes', () => {
            autocomplete.triggerEventHandler('onSelect', { identifier: '123' });
            expect(component.selected.emit).toHaveBeenCalledWith({ identifier: '123' });
            expect(component.propagateChange).toHaveBeenCalledWith('123');
        });

        it('should write value', () => {
            expect(component.writeValue).toHaveBeenCalledWith('c12fe7e6-d338-49d5-973b-2d974d57015b');
            expect(component.val.identifier).toEqual('c12fe7e6-d338-49d5-973b-2d974d57015b');
        });

        it('should clear model and suggections', () => {
            autocomplete.triggerEventHandler('onClear', {});
            expect(component.propagateChange).toHaveBeenCalledWith(null);
            expect(component.results).toEqual([]);
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
