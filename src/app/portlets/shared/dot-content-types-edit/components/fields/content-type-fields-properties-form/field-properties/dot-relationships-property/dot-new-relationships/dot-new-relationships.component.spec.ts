import { DotCMSContentType } from 'dotcms-models';
import { DotNewRelationshipsComponent } from './dot-new-relationships.component';
import { ComponentFixture, async } from '@angular/core/testing';
import {
    DebugElement,
    Component,
    Input,
    Output,
    EventEmitter,
    Injectable,
    forwardRef
} from '@angular/core';
import { PaginatorService } from '@services/paginator';
import { MockDotMessageService } from 'src/app/test/dot-message-service.mock';
import { DOTTestBed } from 'src/app/test/dot-test-bed';
import { PaginationEvent } from '@components/_common/searchable-dropdown/component';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotRelationshipService } from '@portlets/shared/dot-content-types-edit/components/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/services/dot-relationship.service';
import { DotContentTypeService } from '@services/dot-content-type/dot-content-type.service';
import { By } from '@angular/platform-browser';
import { Observable, of } from 'rxjs';
import { DotRelationshipCardinality } from '@portlets/shared/dot-content-types-edit/components/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/model/dot-relationship-cardinality.model';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { dotcmsContentTypeBasicMock } from '@tests/dot-content-types.mock';

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

const contentTypeMock: DotCMSContentType = {
    ...dotcmsContentTypeBasicMock,
    clazz: 'clazz',
    defaultType: false,
    fixed: false,
    folder: 'folder',
    host: 'host',
    name: 'Banner',
    id: '1',
    variable: 'banner',
    owner: 'user',
    system: true
};

@Component({
    selector: 'dot-host-component',
    template: `<dot-new-relationships [cardinality] = "cardinalityIndex"
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
    ]
})
class MockSearchableDropdownComponent implements ControlValueAccessor {
    @Input() data: string[];
    @Input() labelPropertyName: string | string[];
    @Input() pageLinkSize = 3;
    @Input() rows: number;
    @Input() totalRecords: number;
    @Input() placeholder = '';

    @Output() change: EventEmitter<any> = new EventEmitter();
    @Output() filterChange: EventEmitter<string> = new EventEmitter();
    @Output() pageChange: EventEmitter<PaginationEvent> = new EventEmitter();

    writeValue(): void {}

    registerOnChange(): void {}

    registerOnTouched(): void {}

    setDisabledState?(): void {}
}

@Component({
    selector: 'dot-cardinality-selector',
    template: ''
})
class MockCardinalitySelectorComponent {
    @Input() value: number;

    @Input() disabled: boolean;

    @Output() change: EventEmitter<DotRelationshipCardinality> = new EventEmitter();
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
    getContentType(): Observable<DotCMSContentType> {
        return of(contentTypeMock);
    }
}

describe('DotNewRelationshipsComponent', () => {
    let fixtureHostComponent: ComponentFixture<HostTestComponent>;
    let comp: DotNewRelationshipsComponent;
    let de: DebugElement;

    let paginatorService: PaginatorService;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.field.properties.relationship.new.label': 'new',
        'contenttypes.field.properties.relationship.new.content_type.placeholder':
            'Select Content Type',
        'contenttypes.field.properties.relationships.contentType.label': 'Content Type',
        'contenttypes.field.properties.relationships.label': 'Relationship'
    });

    beforeEach(
        async(() => {
            DOTTestBed.configureTestingModule({
                declarations: [
                    HostTestComponent,
                    DotNewRelationshipsComponent,
                    MockSearchableDropdownComponent,
                    MockCardinalitySelectorComponent
                ],
                imports: [],
                providers: [
                    { provide: DotMessageService, useValue: messageServiceMock },
                    { provide: PaginatorService, useClass: MockPaginatorService },
                    { provide: DotRelationshipService, useClass: MockRelationshipService },
                    { provide: DotContentTypeService, useClass: MockDotContentTypeService }
                ]
            });

            fixtureHostComponent = DOTTestBed.createComponent(HostTestComponent);
            de = fixtureHostComponent.debugElement.query(By.css('dot-new-relationships'));
            comp = de.componentInstance;

            paginatorService = de.injector.get(PaginatorService);
            spyOn(paginatorService, 'getWithOffset').and.returnValue(of([contentTypeMock]));
        })
    );

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

            const dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));

            expect(dotSearchableDropdown).not.toBeUndefined();
            expect(dotSearchableDropdown.componentInstance.pageLinkSize).toBe(
                paginatorService.maxLinksPage
            );
            expect(dotSearchableDropdown.componentInstance.rows).toBe(
                paginatorService.paginationPerPage
            );
            expect(dotSearchableDropdown.componentInstance.totalRecords).toBe(
                paginatorService.totalRecords
            );
            expect(dotSearchableDropdown.componentInstance.labelPropertyName).toBe('name');
            expect(dotSearchableDropdown.componentInstance.placeholder).toBe('Select Content Type');
        });

        it('should handle filter change into pagination', () => {
            const newFilter = 'new filter';

            fixtureHostComponent.detectChanges();

            const dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));
            dotSearchableDropdown.triggerEventHandler('filterChange', newFilter);

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

        it('should tigger change event when content type changed', done => {
            fixtureHostComponent.detectChanges();

            comp.change.subscribe((relationshipSelect: any) => {
                expect(relationshipSelect).toEqual({
                    velocityVar: 'banner',
                    cardinality: undefined
                });
                done();
            });

            const dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));
            dotSearchableDropdown.componentInstance.change.emit(contentTypeMock);
        });

        it('should set the correct labels', () => {
            fixtureHostComponent.detectChanges();
            const labels = de.queryAll(By.css('label'));
            const contentTypeLabel = labels[0].nativeElement.textContent;
            const relationshipsLabel = labels[1].nativeElement.textContent;

            expect(contentTypeLabel).toEqual('Content Type*');
            expect(relationshipsLabel).toEqual('Relationship');
        });

        describe('inverse relationships', () => {
            beforeEach(() => {
                fixtureHostComponent.componentInstance.velocityVar = `${contentTypeMock.name}.${contentTypeMock.variable}`;
            });

            it('should load content type, and emit change event with the right variableValue', done => {
                const contentTypeService = de.injector.get(DotContentTypeService);
                spyOn(contentTypeService, 'getContentType').and.callThrough();

                fixtureHostComponent.detectChanges();

                comp.change.subscribe((relationshipSelect: any) => {
                    expect(relationshipSelect).toEqual({
                        velocityVar: `${contentTypeMock.name}.${contentTypeMock.variable}`,
                        cardinality: undefined
                    });
                    done();
                });

                comp.triggerChanged();

                expect(contentTypeService.getContentType).toHaveBeenCalled();
                expect(paginatorService.getWithOffset).not.toHaveBeenCalled();
            });
        });
    });

    describe('Cardinalitys Selector', () => {
        beforeEach(() => {
            fixtureHostComponent.componentInstance.cardinalityIndex = 2;

            fixtureHostComponent.detectChanges();
        });

        it('should hava a dot-cardinality-selector with the right attributes', () => {
            const dotCardinalitySelector = de.query(By.css('dot-cardinality-selector'));
            expect(dotCardinalitySelector).not.toBeUndefined();

            expect(dotCardinalitySelector.componentInstance.value).toEqual(comp.cardinality);
        });

        it('should tigger change event when cardinality changed', done => {
            comp.change.subscribe((relationshipSelect: any) => {
                expect(relationshipSelect).toEqual({
                    velocityVar: undefined,
                    cardinality: 0
                });
                done();
            });

            const dotCardinalitySelector = de.query(By.css('dot-cardinality-selector'));
            dotCardinalitySelector.componentInstance.change.emit(0);
        });
    });
});
