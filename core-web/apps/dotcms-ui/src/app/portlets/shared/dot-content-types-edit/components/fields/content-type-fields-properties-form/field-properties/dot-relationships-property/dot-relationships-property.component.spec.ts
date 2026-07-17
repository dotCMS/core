/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpTestingController } from '@angular/common/http/testing';
import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import {
    FormGroupDirective,
    NgControl,
    UntypedFormControl,
    UntypedFormGroup
} from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { dotcmsContentTypeFieldBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotRelationshipsPropertyComponent } from './dot-relationships-property.component';
import { DotEditContentTypeCacheService } from './services/dot-edit-content-type-cache.service';
import { DotRelationshipService } from './services/dot-relationship.service';

import { DOTTestBed } from '../../../../../../../../test/dot-test-bed';

@Component({
    selector: 'dot-field-validation-message',
    template: '',
    standalone: false
})
class TestFieldValidationMessageComponent {
    @Input()
    field: NgControl;
    @Input()
    message: string;
}

@Component({
    selector: 'dot-new-relationships',
    template: '',
    standalone: false
})
class TestNewRelationshipsComponent {
    @Input()
    cardinality: number;

    @Input()
    velocityVar: string;

    @Input()
    editing: boolean;

    @Output()
    switch: EventEmitter<any> = new EventEmitter();
}

@Component({
    selector: 'dot-edit-relationships',
    template: '',
    standalone: false
})
class TestEditRelationshipsComponent {
    @Output()
    switch: EventEmitter<any> = new EventEmitter();
}

describe('DotRelationshipsPropertyComponent', () => {
    let comp: DotRelationshipsPropertyComponent;
    let fixture: ComponentFixture<DotRelationshipsPropertyComponent>;
    let de: DebugElement;
    let httpMock: HttpTestingController;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.field.properties.relationship.existing.label': 'Existing',
        'contenttypes.field.properties.relationship.new.label': 'New',
        'contenttypes.field.properties.relationships.new.error.required': 'New validation error',
        'contenttypes.field.properties.relationships.edit.error.required': 'Edit validation error'
    });

    const mockCardinalities = [
        { label: 'Many to many', id: 0, name: 'MANY_TO_MANY' },
        { label: 'One to one', id: 1, name: 'ONE_TO_ONE' }
    ];

    const mockContentType: DotCMSContentType = {
        id: '1',
        variable: 'contentType',
        name: 'Content Type'
    } as DotCMSContentType;

    const flushRelationshipHttpMocks = (): void => {
        httpMock
            .match(() => true)
            .forEach((req) => {
                if (req.request.url.includes('/api/v1/relationships/cardinalities')) {
                    req.flush({ entity: mockCardinalities });
                } else if (req.request.url.includes('/api/v1/contenttype/id/')) {
                    req.flush({ entity: mockContentType });
                } else if (req.request.url.includes('/api/v1/contenttype')) {
                    req.flush({
                        entity: [mockContentType],
                        pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                    });
                } else {
                    req.flush({});
                }
            });
    };

    beforeEach(waitForAsync(() => {
        const formGroupDirectiveMock = {
            control: new UntypedFormGroup({
                relationship: new UntypedFormControl('')
            })
        };

        const dotEditContentTypeCacheServiceMock = {
            get: jest.fn().mockReturnValue({ id: 'test-content-type-id' }),
            set: jest.fn()
        };

        DOTTestBed.configureTestingModule({
            declarations: [
                TestFieldValidationMessageComponent,
                TestNewRelationshipsComponent,
                TestEditRelationshipsComponent
            ],
            imports: [DotRelationshipsPropertyComponent, DotMessagePipe],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                DotContentTypeService,
                {
                    provide: DotEditContentTypeCacheService,
                    useValue: dotEditContentTypeCacheServiceMock
                },
                { provide: FormGroupDirective, useValue: formGroupDirectiveMock }
            ]
        });

        TestBed.overrideComponent(DotRelationshipsPropertyComponent, {
            set: {
                providers: [
                    {
                        provide: DotRelationshipService,
                        useValue: {
                            loadCardinalities: jest.fn().mockReturnValue(of(mockCardinalities))
                        }
                    }
                ]
            }
        });

        fixture = DOTTestBed.createComponent(DotRelationshipsPropertyComponent);
        httpMock = TestBed.inject(HttpTestingController);
        de = fixture.debugElement;
        comp = fixture.componentInstance;
        const originalDetectChanges = fixture.detectChanges.bind(fixture);
        fixture.detectChanges = () => originalDetectChanges(false);

        comp.property = {
            name: 'relationship',
            value: {},
            field: {
                ...dotcmsContentTypeFieldBasicMock
            }
        };

        comp.group = new UntypedFormGroup({
            relationship: new UntypedFormControl('')
        });
    }));

    describe('not editing mode', () => {
        beforeEach(() => {
            fixture.detectChanges();
            flushRelationshipHttpMocks();
            fixture.detectChanges();
        });

        it('should have existing and new radio button', () => {
            const labels = de
                .queryAll(By.css('.mb-4 label'))
                .map((label) => label.nativeElement.textContent.trim());

            expect(labels).toEqual(['New', 'Existing']);
        });

        it('should show dot-new-relationships in new state', () => {
            comp.status = comp.STATUS_NEW;
            fixture.detectChanges();
            flushRelationshipHttpMocks();
            fixture.detectChanges();

            expect(de.query(By.css('dot-new-relationships'))).toBeDefined();
            expect(de.query(By.css('dot-edit-relationships'))).toBeNull();
        });

        it('should show dot-edit-relationships in existing state', () => {
            comp.status = comp.STATUS_EXISTING;

            fixture.detectChanges();

            expect(comp.status).toBe(comp.STATUS_EXISTING);
            expect(comp.getValidationErrorMessage()).toBe('Edit validation error');
        });

        it('should clean the relationships property value', () => {
            comp.ngOnInit();

            comp.group.setValue({
                relationship: {
                    velocityVar: 'velocityVar'
                }
            });

            comp.clean();

            expect(comp.group.get('relationship').value).toEqual(comp.beforeValue);
        });
    });

    describe('editing mode', () => {
        beforeEach(() => {
            comp.property = {
                name: 'relationship',
                value: {
                    velocityVar: 'velocityVar',
                    cardinality: 1
                },
                field: {
                    ...dotcmsContentTypeFieldBasicMock
                }
            };

            comp.group = new UntypedFormGroup({
                relationship: new UntypedFormControl(comp.property.value)
            });
        });

        it('should not have existing and new radio buttonand should show dot-new-relationships', () => {
            comp.ngOnInit();
            fixture.detectChanges();
            flushRelationshipHttpMocks();
            fixture.detectChanges();

            const dotNewRelationships = de.query(By.css('dot-new-relationships'));

            expect(comp.editing).toBe(true);
            expect(dotNewRelationships).toBeDefined();
            expect(de.query(By.css('dot-edit-relationships'))).toBeNull();

            const relationshipValue = comp.group.get('relationship').value;
            expect(relationshipValue.velocityVar).toEqual('velocityVar');
            expect(relationshipValue.cardinality).toEqual(1);
        });

        describe('with inverse relationship', () => {
            // TODO(#35930): Angular 22 FetchBackend + inverse velocityVar triggers async HTTP
            // outside HttpTestingController in this legacy DOTTestBed spec; re-enable after
            // migrating this suite to Spectator + provideHttpClientTesting().
            it.skip('should not have existing and new radio buttonand should show dot-new-relationships', () => {
                comp.group.get('relationship').setValue({
                    velocityVar: 'contentType.fieldName',
                    cardinality: 1
                });
                comp.ngOnInit();

                expect(comp.editing).toBe(true);
                expect(comp.status).toBe(comp.STATUS_NEW);

                const relationshipValue = comp.group.get('relationship').value;
                expect(relationshipValue.velocityVar).toEqual('contentType.fieldName');
                expect(relationshipValue.cardinality).toEqual(1);
            });
        });
    });
});
