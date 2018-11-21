import { ContentType } from '@portlets/content-types/shared/content-type.model';
import { NewRelationshipsComponent } from './new-relationships.component';
import { ComponentFixture } from '@angular/core/testing';
import { DebugElement, Component, Input, Output, EventEmitter, Injectable, forwardRef } from '@angular/core';
import { PaginatorService } from '@services/paginator';
import { MockDotMessageService } from 'src/app/test/dot-message-service.mock';
import { DOTTestBed } from 'src/app/test/dot-test-bed';
import { PaginationEvent } from '@components/_common/searchable-dropdown/component';
import { DotMessageService } from '@services/dot-messages-service';
import { RelationshipService } from '@portlets/content-types/fields/service/relationship.service';
import { DotContentTypeService } from '@services/dot-content-type/dot-content-type.service';
import { By } from '@angular/platform-browser';
import { Observable, of } from 'rxjs';
import { DotRelationshipCardinality } from '@portlets/content-types/fields/shared/dot-relationship-cardinality.model';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

const cardinalities = [
    {
        label: 'Many to many',
        id: 0,
        name: 'MANY_TO_MANY'
    },
    {
        label: 'One to one',
        id: 1,
        name: 'ONE_TO_ONE'
    }
];

const contentTypeMock: ContentType = {
    clazz: 'clazz',
    defaultType: false,
    fixed: false,
    folder: 'folder',
    host: 'host',
    name: 'Banner',
    id: '1',
    variable: 'banner',
    owner: 'user',
    system: true,
};

@Component({
    selector: 'dot-host-component',
    template: `<dot-new-relationships [cardinalityIndex] = "cardinalityIndex"
                                      [velocityVar] = "velocityVar"
                                      [editing] = "editing">
               </dot-new-relationships>`
})
class HostTestComponent {
    cardinalityIndex: number;
    velocityVar: string;
    editing: boolean;
}

@Component({
    selector: 'dot-searchable-dropdown',
    template: '',
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MockSearchableDropdownComponent)
        }
    ],
})
class MockSearchableDropdownComponent implements ControlValueAccessor {
    @Input()
    data: string[];
    @Input()
    labelPropertyName: string | string[];
    @Input()
    pageLinkSize = 3;
    @Input()
    rows: number;
    @Input()
    totalRecords: number;
    @Input()
    placeholder = '';

    @Output()
    change: EventEmitter<any> = new EventEmitter();
    @Output()
    filterChange: EventEmitter<string> = new EventEmitter();
    @Output()
    pageChange: EventEmitter<PaginationEvent> = new EventEmitter();

    writeValue(obj: any): void {
        console.log('obj', obj);
    }
    registerOnChange(fn: any): void {
        console.log('fn', fn);
    }
    registerOnTouched(fn: any): void {
        console.log('fn', fn);
    }
    setDisabledState?(isDisabled: boolean): void {
        console.log('isDisabled', isDisabled);
    }
}

@Component({
    selector: 'dot-cardinality-selector',
    template: '',
})
class MockCardinalitySelectorComponent {
    @Input()
    cardinalityIndex: number;

    @Input()
    disabled: boolean;

    @Output()
    change: EventEmitter<DotRelationshipCardinality> = new EventEmitter();
}

@Injectable()
class MockPaginatorService {
    url: string;

    public paginationPerPage: 10;
    public maxLinksPage: 5;
    public totalRecords: 40;

    public getWithOffset(): Observable<any[]> {
        return null;
    }
}

@Injectable()
class MockRelationshipService {
    loadCardinalities(): Observable<DotRelationshipCardinality[]> {
        return of(cardinalities);
    }
}

@Injectable()
class MockDotContentTypeService {
    getContentType(): Observable<ContentType> {
        return of(contentTypeMock);
    }
}

describe('NewRelationshipsComponent', () => {
    let fixtureHostComponent: ComponentFixture<HostTestComponent>;
    let comp: NewRelationshipsComponent;
    let de: DebugElement;

    let paginatorService: PaginatorService;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.field.properties.relationship.new.label': 'new',
        'contenttypes.field.properties.relationship.new.content_type.placeholder': 'Select Content Type',
    });

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                HostTestComponent,
                NewRelationshipsComponent,
                MockSearchableDropdownComponent,
                MockCardinalitySelectorComponent
            ],
            imports: [],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: PaginatorService, useClass: MockPaginatorService },
                { provide: RelationshipService, useClass: MockRelationshipService },
                { provide: DotContentTypeService, useClass: MockDotContentTypeService },
            ]
        });

        fixtureHostComponent = DOTTestBed.createComponent(HostTestComponent);
        de = fixtureHostComponent.debugElement.query(By.css('dot-new-relationships'));
        comp = de.componentInstance;

        paginatorService = de.injector.get(PaginatorService);
        spyOn(paginatorService, 'getWithOffset').and.returnValue(of([contentTypeMock]));
    });

    describe('Content Types', () => {
        beforeEach(() => {
            fixtureHostComponent.componentInstance.velocityVar = contentTypeMock.variable;
        });

        it('should set url to get content types', () => {
            fixtureHostComponent.detectChanges();
            expect(paginatorService.url).toBe('v1/contenttype');
        });

        it('should has a dot-searchable-dropdown and it should has the right attributes values', () => {
            fixtureHostComponent.detectChanges();

            const  dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));

            expect(dotSearchableDropdown).not.toBeUndefined();
            expect(dotSearchableDropdown.componentInstance.pageLinkSize).toBe(paginatorService.maxLinksPage);
            expect(dotSearchableDropdown.componentInstance.rows).toBe(paginatorService.paginationPerPage);
            expect(dotSearchableDropdown.componentInstance.totalRecords).toBe(paginatorService.totalRecords);
            expect(dotSearchableDropdown.componentInstance.labelPropertyName).toBe('name');
            expect(dotSearchableDropdown.componentInstance.placeholder).toBe('Select Content Type');
        });

        it('should handle filter change into pagination', () => {
            const newFilter = 'new filter';

            fixtureHostComponent.detectChanges();

            const dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));
            dotSearchableDropdown.componentInstance.filterChange.emit(newFilter);

            expect(paginatorService.filter).toBe(newFilter);
            expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);

            fixtureHostComponent.detectChanges();

            expect(dotSearchableDropdown.componentInstance.data).toEqual([contentTypeMock]);
        });

        it('should handle page change into pagination', () => {
            const event = {
                filter: 'new filter',
                first: 2
            };

            fixtureHostComponent.detectChanges();

            const dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));
            dotSearchableDropdown.componentInstance.pageChange.emit(event);

            expect(paginatorService.filter).toBe(event.filter);
            expect(paginatorService.getWithOffset).toHaveBeenCalledWith(event.first);

            fixtureHostComponent.detectChanges();

            expect(dotSearchableDropdown.componentInstance.data).toEqual([contentTypeMock]);
        });


        it('should tigger change event when content type changed', (done) => {
            fixtureHostComponent.detectChanges();

            comp.change.subscribe((relationshipSelect: any) => {
                expect(relationshipSelect).toEqual(
                    {
                        velocityVar: 'banner',
                        cardinality: undefined
                    }
                );
                done();
            });

            const dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));
            dotSearchableDropdown.componentInstance.change.emit(contentTypeMock);
        });
    });

    describe('Cardinalitys Selector', () => {

        beforeEach(() => {
            fixtureHostComponent.componentInstance.cardinalityIndex = 2;
        });

        it('should hava a dot-cardinality-selector with the right attributes', () => {
            fixtureHostComponent.detectChanges();

            const dotCardinalitySelector = de.query(By.css('dot-cardinality-selector'));
            expect(dotCardinalitySelector).not.toBeUndefined();

            expect(dotCardinalitySelector.componentInstance.cardinalityIndex).toEqual(comp.cardinalityIndex);
        });

        it('should tigger change event when cardinality changed', (done) => {
            fixtureHostComponent.detectChanges();

            comp.change.subscribe((relationshipSelect: any) => {
                expect(relationshipSelect).toEqual(
                    {
                        velocityVar: undefined,
                        cardinality: 0
                    }
                );
                done();
            });

            const dotCardinalitySelector = de.query(By.css('dot-cardinality-selector'));
            dotCardinalitySelector.componentInstance.change.emit(0);
        });
    });
});
