/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, of } from 'rxjs';

import {
    Component,
    DebugElement,
    EventEmitter,
    forwardRef,
    Injectable,
    Input,
    Output
} from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { ControlValueAccessor, FormGroupDirective, NG_VALUE_ACCESSOR } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotContentTypeService, DotMessageService, PaginatorService } from '@dotcms/data-access';
import { DotCMSClazzes, DotCMSContentType } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';
import { dotcmsContentTypeBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotNewRelationshipsComponent } from './dot-new-relationships.component';

import { DOTTestBed } from '../../../../../../../../../test/dot-test-bed';
import { PaginationEvent } from '../../../../../../../../../view/components/_common/searchable-dropdown/component/searchable-dropdown.component';
import { DotRelationshipCardinality } from '../model/dot-relationship-cardinality.model';
import { DotRelationshipService } from '../services/dot-relationship.service';

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
    clazz: DotCMSClazzes.TEXT,
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
    template: `
        <dot-new-relationships
            [cardinality]="cardinalityIndex"
            [velocityVar]="velocityVar"
            [editing]="editing"></dot-new-relationships>
    `,
    standalone: false
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
    standalone: false
})
class MockSearchableDropdownComponent implements ControlValueAccessor {
    @Input() data: string[];
    @Input() labelPropertyName: string | string[];
    @Input() pageLinkSize = 3;
    @Input() rows: number;
    @Input() totalRecords: number;
    @Input() placeholder = '';

    @Output() switch: EventEmitter<any> = new EventEmitter();
    @Output() filterChange: EventEmitter<string> = new EventEmitter();
    @Output() pageChange: EventEmitter<PaginationEvent> = new EventEmitter();

    writeValue(): void {
        /* */
    }

    registerOnChange(): void {
        /* */
    }

    registerOnTouched(): void {
        /* */
    }

    setDisabledState?(): void {
        /* */
    }
}

@Component({
    selector: 'dot-cardinality-selector',
    template: '',
    standalone: false
})
class MockCardinalitySelectorComponent {
    @Input() value: number;

    @Input() disabled: boolean;

    @Output() switch: EventEmitter<DotRelationshipCardinality> = new EventEmitter();
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

    beforeEach(waitForAsync(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                HostTestComponent,
                DotNewRelationshipsComponent,
                MockSearchableDropdownComponent,
                MockCardinalitySelectorComponent
            ],
            imports: [DotFieldRequiredDirective, DotMessagePipe],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: PaginatorService, useClass: MockPaginatorService },
                { provide: DotRelationshipService, useClass: MockRelationshipService },
                { provide: DotContentTypeService, useClass: MockDotContentTypeService },
                FormGroupDirective
            ]
        });

        fixtureHostComponent = DOTTestBed.createComponent(HostTestComponent);
        de = fixtureHostComponent.debugElement.query(By.css('dot-new-relationships'));
        comp = de.componentInstance;

        paginatorService = de.injector.get(PaginatorService);
        jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(of([contentTypeMock]));
    }));

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

        it('should tigger change event when content type changed', (done) => {
            fixtureHostComponent.detectChanges();

            comp.switch.subscribe((relationshipSelect: any) => {
                expect(relationshipSelect).toEqual({
                    velocityVar: 'banner',
                    cardinality: undefined
                });
                done();
            });

            const dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));
            dotSearchableDropdown.componentInstance.switch.emit(contentTypeMock);
        });

        it('should set the correct labels', () => {
            fixtureHostComponent.detectChanges();
            const labels = de.queryAll(By.css('label'));
            const contentTypeLabel = labels[0].nativeElement;
            const relationshipsLabel = labels[1].nativeElement.textContent;
            expect(contentTypeLabel.textContent.trim()).toEqual('Content Type');
            expect(contentTypeLabel.classList.contains('p-label-input-required')).toBeTruthy();
            expect(relationshipsLabel).toEqual('Relationship');
        });

        describe('inverse relationships', () => {
            beforeEach(() => {
                fixtureHostComponent.componentInstance.velocityVar = `${contentTypeMock.name}.${contentTypeMock.variable}`;
            });

            it('should load content type, and emit change event with the right variableValue', (done) => {
                const contentTypeService = de.injector.get(DotContentTypeService);
                jest.spyOn(contentTypeService, 'getContentType');

                fixtureHostComponent.detectChanges();

                comp.switch.subscribe((relationshipSelect: any) => {
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

        it('should tigger change event when cardinality changed', (done) => {
            comp.switch.subscribe((relationshipSelect: any) => {
                expect(relationshipSelect).toEqual({
                    velocityVar: undefined,
                    cardinality: 0
                });
                done();
            });

            const dotCardinalitySelector = de.query(By.css('dot-cardinality-selector'));
            dotCardinalitySelector.componentInstance.switch.emit(0);
        });
    });
});
