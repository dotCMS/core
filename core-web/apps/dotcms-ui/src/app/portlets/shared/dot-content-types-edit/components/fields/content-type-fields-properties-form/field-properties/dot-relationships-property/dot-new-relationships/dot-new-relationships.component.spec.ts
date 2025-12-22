import {
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';
import { FormControl, FormGroup, FormGroupDirective } from '@angular/forms';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotContentTypeComponent } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotNewRelationshipsComponent } from './dot-new-relationships.component';

import { DotCardinalitySelectorComponent } from '../dot-cardinality-selector/dot-cardinality-selector.component';
import { DotRelationshipCardinality } from '../model/dot-relationship-cardinality.model';
import { DotRelationshipService } from '../services/dot-relationship.service';

const mockContentType: DotCMSContentType = {
    id: '123',
    name: 'Blog',
    variable: 'Blog'
} as DotCMSContentType;

const mockContentTypeWithDot: DotCMSContentType = {
    id: '456',
    name: 'News Article',
    variable: 'NewsArticle'
} as DotCMSContentType;

const formMock = new FormGroup({
    contentType: new FormControl('')
});

const formGroupDirectiveMock = new FormGroupDirective([], []);
formGroupDirectiveMock.form = formMock;

const messageServiceMock = new MockDotMessageService({
    'contenttypes.field.properties.relationships.contentType.label': 'Content Type',
    'contenttypes.field.properties.relationships.label': 'Cardinality',
    'contenttypes.field.properties.relationship.new.content_type.placeholder': 'Select Content Type'
});

const mockCardinalities: DotRelationshipCardinality[] = [
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

describe('DotNewRelationshipsComponent', () => {
    let spectator: Spectator<DotNewRelationshipsComponent>;
    let contentTypeService: SpyObject<DotContentTypeService>;

    const createComponent = createComponentFactory({
        component: DotNewRelationshipsComponent,
        imports: [
            MockComponent(DotContentTypeComponent),
            MockComponent(DotCardinalitySelectorComponent)
        ],
        providers: [
            mockProvider(DotContentTypeService),
            mockProvider(DotRelationshipService, {
                loadCardinalities: jest.fn().mockReturnValue(of(mockCardinalities))
            }),
            { provide: FormGroupDirective, useValue: formGroupDirectiveMock },
            { provide: DotMessageService, useValue: messageServiceMock },
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        contentTypeService = spectator.inject(DotContentTypeService, true);
        contentTypeService.getContentTypesWithPagination.mockReturnValue(
            of({
                contentTypes: [mockContentType, mockContentTypeWithDot],
                pagination: {
                    currentPage: 1,
                    perPage: 40,
                    totalEntries: 2
                }
            })
        );
        contentTypeService.getContentType.mockImplementation((variable: string) =>
            of(
                [mockContentType, mockContentTypeWithDot].find((ct) => ct.variable === variable) ||
                    mockContentType
            )
        );
    });

    describe('Component Initialization', () => {
        it('should create', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should initialize with default values', () => {
            expect(spectator.component.contentType).toBeUndefined();
            expect(spectator.component.currentCardinalityIndex).toBeUndefined();
        });
    });

    describe('ngOnChanges', () => {
        it('should load content type when velocityVar changes', () => {
            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            spectator.setInput('velocityVar', 'Blog');
            spectator.detectChanges();

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('Blog');
            expect(spectator.component.contentType).toEqual(mockContentType);
        });

        it('should handle velocityVar with dot notation', () => {
            contentTypeService.getContentType.mockReturnValue(of(mockContentTypeWithDot));

            spectator.setInput('velocityVar', 'NewsArticle.field');
            spectator.detectChanges();

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('NewsArticle');
            expect(spectator.component.contentType).toEqual(mockContentTypeWithDot);
        });

        it('should set contentType to null when velocityVar is empty', () => {
            spectator.setInput('velocityVar', '');
            spectator.detectChanges();

            expect(contentTypeService.getContentType).not.toHaveBeenCalled();
            expect(spectator.component.contentType).toBeNull();
        });

        it('should set contentType to null when velocityVar is undefined', () => {
            spectator.setInput('velocityVar', undefined);
            spectator.detectChanges();

            expect(contentTypeService.getContentType).not.toHaveBeenCalled();
            expect(spectator.component.contentType).toBeNull();
        });

        it('should handle null contentType from service', () => {
            contentTypeService.getContentType.mockReturnValue(of(null));

            spectator.setInput('velocityVar', 'NonExistent');
            spectator.detectChanges();

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('NonExistent');
            expect(spectator.component.contentType).toBeNull();
        });

        it('should update currentCardinalityIndex when cardinality changes', () => {
            spectator.setInput('cardinality', 2);
            spectator.detectChanges();

            expect(spectator.component.currentCardinalityIndex).toBe(2);
        });

        it('should handle both velocityVar and cardinality changes', () => {
            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            spectator.setInput('velocityVar', 'Blog');
            spectator.setInput('cardinality', 1);
            spectator.detectChanges();

            expect(spectator.component.contentType).toEqual(mockContentType);
            expect(spectator.component.currentCardinalityIndex).toBe(1);
        });
    });

    describe('onContentTypeChange', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should update contentType and emit switch event', fakeAsync(() => {
            const emitSpy = jest.spyOn(spectator.component.switch, 'emit');
            spectator.component.currentCardinalityIndex = 1;
            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            spectator.component.onContentTypeChange(mockContentType.variable);
            tick();

            expect(contentTypeService.getContentType).toHaveBeenCalledWith(mockContentType.variable);
            expect(spectator.component.contentType).toEqual(mockContentType);
            expect(emitSpy).toHaveBeenCalledWith({
                velocityVar: mockContentType.variable,
                cardinality: 1
            });
        }));

        it('should handle null variable', () => {
            const emitSpy = jest.spyOn(spectator.component.switch, 'emit');
            spectator.component.currentCardinalityIndex = 0;

            spectator.component.onContentTypeChange(null);

            expect(contentTypeService.getContentType).not.toHaveBeenCalled();
            expect(spectator.component.contentType).toBeNull();
            expect(emitSpy).toHaveBeenCalledWith({
                velocityVar: undefined,
                cardinality: 0
            });
        });

        it('should prioritize velocityVar input over contentType variable when emitting', fakeAsync(() => {
            const emitSpy = jest.spyOn(spectator.component.switch, 'emit');
            spectator.setInput('velocityVar', 'CustomVar');
            spectator.component.currentCardinalityIndex = 2;
            contentTypeService.getContentType.mockReturnValue(of(mockContentType));
            spectator.detectChanges();

            spectator.component.onContentTypeChange(mockContentType.variable);
            tick();

            expect(spectator.component.contentType).toEqual(mockContentType);
            expect(emitSpy).toHaveBeenCalledWith({
                velocityVar: 'CustomVar',
                cardinality: 2
            });
        }));
    });

    describe('cardinalityChanged', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should update currentCardinalityIndex and emit switch event', () => {
            const emitSpy = jest.spyOn(spectator.component.switch, 'emit');
            spectator.setInput('velocityVar', 'Blog');
            spectator.component.contentType = mockContentType;
            spectator.detectChanges();

            spectator.component.cardinalityChanged(3);

            expect(spectator.component.currentCardinalityIndex).toBe(3);
            expect(emitSpy).toHaveBeenCalledWith({
                velocityVar: 'Blog',
                cardinality: 3
            });
        });

        it('should use contentType variable when velocityVar is not set', () => {
            const emitSpy = jest.spyOn(spectator.component.switch, 'emit');
            spectator.component.contentType = mockContentType;
            spectator.detectChanges();

            spectator.component.cardinalityChanged(1);

            expect(emitSpy).toHaveBeenCalledWith({
                velocityVar: mockContentType.variable,
                cardinality: 1
            });
        });

        it('should handle undefined velocityVar and contentType', () => {
            const emitSpy = jest.spyOn(spectator.component.switch, 'emit');

            spectator.component.cardinalityChanged(0);

            expect(emitSpy).toHaveBeenCalledWith({
                velocityVar: undefined,
                cardinality: 0
            });
        });
    });

    describe('triggerChanged', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should emit switch event with velocityVar input when available', () => {
            const emitSpy = jest.spyOn(spectator.component.switch, 'emit');
            spectator.setInput('velocityVar', 'CustomVar');
            spectator.component.currentCardinalityIndex = 2;
            spectator.detectChanges();

            spectator.component.triggerChanged();

            expect(emitSpy).toHaveBeenCalledWith({
                velocityVar: 'CustomVar',
                cardinality: 2
            });
        });

        it('should emit switch event with contentType variable when velocityVar is not set', () => {
            const emitSpy = jest.spyOn(spectator.component.switch, 'emit');
            spectator.component.contentType = mockContentType;
            spectator.component.currentCardinalityIndex = 1;
            spectator.detectChanges();

            spectator.component.triggerChanged();

            expect(emitSpy).toHaveBeenCalledWith({
                velocityVar: mockContentType.variable,
                cardinality: 1
            });
        });

        it('should emit switch event with undefined velocityVar when neither input nor contentType is set', () => {
            const emitSpy = jest.spyOn(spectator.component.switch, 'emit');
            spectator.component.currentCardinalityIndex = 0;
            spectator.detectChanges();

            spectator.component.triggerChanged();

            expect(emitSpy).toHaveBeenCalledWith({
                velocityVar: undefined,
                cardinality: 0
            });
        });
    });

    describe('Template Integration', () => {
        beforeEach(() => {
            contentTypeService.getContentType.mockReturnValue(of(mockContentType));
            spectator.setInput('velocityVar', 'Blog');
            spectator.setInput('cardinality', 1);
            spectator.detectChanges();
        });

        it('should render dot-content-type component', () => {
            const contentTypeComponent = spectator.query('dot-content-type');
            expect(contentTypeComponent).toBeTruthy();
        });

        it('should render dot-cardinality-selector component', () => {
            const cardinalitySelector = spectator.query('dot-cardinality-selector');
            expect(cardinalitySelector).toBeTruthy();
        });

        it('should pass disabled prop to dot-content-type when editing', () => {
            spectator.setInput('editing', true);
            spectator.detectChanges();

            const contentTypeComponent = spectator.query('dot-content-type');
            expect(contentTypeComponent).toBeTruthy();
            expect(spectator.component.editing).toBe(true);
        });

        it('should pass disabled prop to dot-cardinality-selector when editing', () => {
            spectator.setInput('editing', true);
            spectator.detectChanges();

            const cardinalitySelector = spectator.query('dot-cardinality-selector');
            expect(cardinalitySelector).toBeTruthy();
            expect(spectator.component.editing).toBe(true);
        });

        it('should pass cardinality value to dot-cardinality-selector', () => {
            spectator.setInput('cardinality', 2);
            spectator.detectChanges();

            const cardinalitySelector = spectator.query('dot-cardinality-selector');
            expect(cardinalitySelector).toBeTruthy();
            expect(spectator.component.cardinality).toBe(2);
        });
    });

    describe('Event Handling', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should handle content type change from dot-content-type component', fakeAsync(() => {
            const emitSpy = jest.spyOn(spectator.component.switch, 'emit');
            spectator.component.currentCardinalityIndex = 1;
            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            spectator.triggerEventHandler('dot-content-type', 'onChange', mockContentType.variable);
            tick();

            expect(contentTypeService.getContentType).toHaveBeenCalledWith(mockContentType.variable);
            expect(spectator.component.contentType).toEqual(mockContentType);
            expect(emitSpy).toHaveBeenCalled();
        }));

        it('should handle cardinality change from dot-cardinality-selector component', () => {
            const emitSpy = jest.spyOn(spectator.component.switch, 'emit');
            spectator.setInput('velocityVar', 'Blog');
            spectator.component.contentType = mockContentType;
            spectator.detectChanges();

            spectator.triggerEventHandler('dot-cardinality-selector', 'switch', 2);

            expect(spectator.component.currentCardinalityIndex).toBe(2);
            expect(emitSpy).toHaveBeenCalledWith({
                velocityVar: 'Blog',
                cardinality: 2
            });
        });
    });
});

