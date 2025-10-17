/* eslint-disable @typescript-eslint/no-explicit-any */

import { createFakeEvent } from '@ngneat/spectator';
import { of } from 'rxjs';

import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { Dropdown, DropdownModule } from 'primeng/dropdown';
import { SelectButton, SelectButtonModule } from 'primeng/selectbutton';
import { TableModule } from 'primeng/table';

import { DotMessageService, DotFormatDateService } from '@dotcms/data-access';
import { DotcmsConfigService, LoginService } from '@dotcms/dotcms-js';
import { DotDiffPipe, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';
import { MockDotMessageService, mockMatchMedia } from '@dotcms/utils-testing';

import { DotContentCompareTableComponent } from './dot-content-compare-table.component';

import { DotContentCompareTableData } from '../../store/dot-content-compare.store';
import { DotContentComparePreviewFieldComponent } from '../fields/dot-content-compare-preview-field/dot-content-compare-preview-field.component';

@Component({
    standalone: false,
    selector: 'dot-test-host-component',
    template:
        '<dot-content-compare-table [data]="data" (bringBack)="bringBack.emit($event)" (changeDiff)="changeDiff.emit($event)" (changeVersion)="changeVersion.emit($event)" [showDiff]="showDiff"></dot-content-compare-table>'
})
class TestHostComponent {
    @Input() data: DotContentCompareTableData;
    @Input() showDiff: boolean;
    @Output() bringBack = new EventEmitter<string>();

    changeDiff: EventEmitter<any> = new EventEmitter();
    changeVersion: EventEmitter<any> = new EventEmitter();
}

export const dotContentCompareTableDataMock: DotContentCompareTableData = {
    working: {
        archived: false,
        baseType: 'CONTENT',
        binary: '/dA/2970221a3a51990039a81976db3b137f/binary/costarica.png',
        binaryContentAsset: '2970221a3a51990039a81976db3b137f/binary',
        binaryVersion: '/dA/21ae95f9-357d-4f1e-b677-1fc23ccde394/binary/costarica.png',
        contentType: 'AllFields',
        file: '186283d7-0e9d-454b-90a5-010410d96926',
        folder: 'SYSTEM_FOLDER',
        hasTitleImage: true,
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        hostName: 'demo.dotcms.com',
        identifier: '2970221a3a51990039a81976db3b137f',
        image: 'f0625f765ffc0aa98e66f07e96dc9e48',
        inode: '21ae95f9-357d-4f1e-b677-1fc23ccde394',
        languageId: 1,
        live: true,
        locked: false,
        modDate: '12/15/2021 - 02:56 PM',
        modUser: 'dotcms.org.1',
        modUserName: 'Admin User',
        owner: 'dotcms.org.1',
        sortOrder: 0,
        stInode: 'f778408c6c5454a26547b633b7d803d5',
        text: 'Hi',
        title: '2970221a3a51990039a81976db3b137f',
        titleImage: 'binary',
        url: '/content.dcd47b31-0682-448f-8ffb-c40350caab41',
        working: true,
        json: { name: 'John' }
    },
    compare: {
        archived: false,
        baseType: 'CONTENT',
        binary: '/dA/2970221a3a51990039a81976db3b137f/binary/leon.png',
        binaryContentAsset: '2970221a3a51990039a81976db3b137f/binary',
        binaryVersion: '/dA/d094b42f-5ef8-4bcc-8783-73ff39a4c6e6/binary/leon.png',
        contentType: 'AllFields',
        file: 'new-one',
        folder: 'SYSTEM_FOLDER',
        hasTitleImage: true,
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        hostName: 'demo.dotcms.com',
        identifier: '2970221a3a51990039a81976db3b137f',
        image: 'f0625f765ffc0aa98e66f07e96dc9e48',
        inode: 'd094b42f-5ef8-4bcc-8783-73ff39a4c6e6',
        languageId: 1,
        live: false,
        locked: false,
        modDate: '12/15/2021 - 02:56 PM',
        modUser: 'dotcms.org.1',
        modUserName: 'Admin User',
        owner: 'dotcms.org.1',
        sortOrder: 0,
        stInode: 'f778408c6c5454a26547b633b7d803d5',
        text: 'Hello',
        title: '2970221a3a51990039a81976db3b137f',
        titleImage: 'binary',
        url: '/content.dcd47b31-0682-448f-8ffb-c40350caab41',
        working: false,
        json: { name: 'David' }
    },
    versions: [
        {
            archived: false,
            baseType: 'CONTENT',
            binary: '/dA/2970221a3a51990039a81976db3b137f/binary/leon.png',
            binaryContentAsset: '2970221a3a51990039a81976db3b137f/binary',
            binaryVersion: '/dA/d094b42f-5ef8-4bcc-8783-73ff39a4c6e6/binary/leon.png',
            contentType: 'AllFields',
            file: '186283d7-0e9d-454b-90a5-010410d96926',
            folder: 'SYSTEM_FOLDER',
            hasTitleImage: true,
            host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
            hostName: 'demo.dotcms.com',
            identifier: '2970221a3a51990039a81976db3b137f',
            image: 'f0625f765ffc0aa98e66f07e96dc9e48',
            inode: 'd094b42f-5ef8-4bcc-8783-73ff39a4c6e6',
            languageId: 1,
            live: false,
            locked: false,
            modDate: '12/15/2021 - 01:56 PM',
            modUser: 'dotcms.org.1',
            modUserName: 'Admin User',
            owner: 'dotcms.org.1',
            sortOrder: 0,
            stInode: 'f778408c6c5454a26547b633b7d803d5',
            text: 'just a simple text',
            title: '2970221a3a51990039a81976db3b137f',
            titleImage: 'binary',
            url: '/content.dcd47b31-0682-448f-8ffb-c40350caab41',
            working: false,
            json: { name: 'John' }
        },
        {
            archived: false,
            baseType: 'CONTENT',
            binary: '/dA/2970221a3a51990039a81976db3b137f/binary/leon.png',
            binaryContentAsset: '2970221a3a51990039a81976db3b137f/binary',
            binaryVersion: '/dA/dcd47b31-0682-448f-8ffb-c40350caab41/binary/leon.png',
            contentType: 'AllFields',
            file: '186283d7-0e9d-454b-90a5-010410d96926',
            folder: 'SYSTEM_FOLDER',
            hasTitleImage: true,
            host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
            hostName: 'demo.dotcms.com',
            identifier: '2970221a3a51990039a81976db3b137f',
            image: 'f0625f765ffc0aa98e66f07e96dc9e48',
            inode: 'dcd47b31-0682-448f-8ffb-c40350caab41',
            languageId: 1,
            live: false,
            locked: false,
            modDate: '12/12/2021 - 01:56 PM',
            modUser: 'dotcms.org.1',
            modUserName: 'Admin User',
            owner: 'dotcms.org.1',
            sortOrder: 0,
            stInode: 'f778408c6c5454a26547b633b7d803d5',
            text: 'just a simple text',
            title: '2970221a3a51990039a81976db3b137f',
            titleImage: 'binary',
            url: '/content.dcd47b31-0682-448f-8ffb-c40350caab41',
            working: false,
            json: { name: 'David' }
        }
    ],
    fields: [
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableBinaryField',
            contentTypeId: 'f778408c6c5454a26547b633b7d803d5',
            dataType: 'SYSTEM',
            fieldType: 'Binary',
            fieldTypeLabel: 'Binary',
            fieldVariables: [],
            fixed: false,
            iDate: 1639601659000,
            id: '080130cd48183bc6a458c3446f49ee69',
            indexed: false,
            listed: false,
            modDate: 1639601659000,
            name: 'binary',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 2,
            unique: false,
            variable: 'binary'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableFileField',
            contentTypeId: 'f778408c6c5454a26547b633b7d803d5',
            dataType: 'TEXT',
            fieldType: 'File',
            fieldTypeLabel: 'File',
            fieldVariables: [],
            fixed: false,
            iDate: 1639601664000,
            id: '2d2fdfb8408a2eab8277464c6b250c96',
            indexed: false,
            listed: false,
            modDate: 1639601664000,
            name: 'file',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 3,
            unique: false,
            variable: 'file'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableImageField',
            contentTypeId: 'f778408c6c5454a26547b633b7d803d5',
            dataType: 'TEXT',
            fieldType: 'Image',
            fieldTypeLabel: 'Image',
            fieldVariables: [],
            fixed: false,
            iDate: 1639601679000,
            id: '5b4e414b65d19e7cb05f11171500cc33',
            indexed: false,
            listed: false,
            modDate: 1639601679000,
            name: 'image',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 4,
            unique: false,
            variable: 'image'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
            contentTypeId: 'f778408c6c5454a26547b633b7d803d5',
            dataType: 'TEXT',
            fieldType: 'Text',
            fieldTypeLabel: 'Text',
            fieldVariables: [],
            fixed: false,
            iDate: 1639601686000,
            id: '98741e6e7d421475cf184370bebcaf71',
            indexed: false,
            listed: false,
            modDate: 1639601686000,
            name: 'text',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 5,
            unique: false,
            variable: 'text'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableJSONField',
            contentTypeId: '61226fd915b7f025da020fc1f5856ab7',
            dataType: 'LONG_TEXT',
            fieldType: 'JSON-Field',
            fieldTypeLabel: 'JSON Field',
            fieldVariables: [],
            fixed: false,
            iDate: 1666028511000,
            id: 'b577e8c1842949797350b735659beb40',
            indexed: false,
            listed: false,
            modDate: 1666028511000,
            name: 'test',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 2,
            unique: false,
            variable: 'json'
        }
    ]
};

describe('DotContentCompareTableComponent', () => {
    let hostComponent: TestHostComponent;
    let hostFixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    const messageServiceMock = new MockDotMessageService({
        diff: 'Diff',
        plain: 'Plain'
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                TestHostComponent,
                DotContentCompareTableComponent,
                DotContentComparePreviewFieldComponent
            ],
            imports: [
                TableModule,
                DropdownModule,
                SelectButtonModule,
                DotDiffPipe,
                DotMessagePipe,
                FormsModule,
                DotRelativeDatePipe,
                ButtonModule,
                BrowserAnimationsModule
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                DotFormatDateService,
                {
                    provide: DotcmsConfigService,
                    useValue: {
                        getSystemTimeZone: () =>
                            of({
                                id: 'America/Costa_Rica',
                                label: 'Central Standard Time (America/Costa_Rica)',
                                offset: -21600000
                            })
                    }
                },
                {
                    provide: LoginService,
                    useValue: { currentUserLanguageId: 'en-US' }
                }
            ]
        });

        hostFixture = TestBed.createComponent(TestHostComponent);
        hostComponent = hostFixture.componentInstance;
        de = hostFixture.debugElement;
        hostComponent.data = dotContentCompareTableDataMock;
        hostFixture.detectChanges();
        mockMatchMedia();
    });

    describe('header', () => {
        it('should show title correctly', () => {
            expect(
                de
                    .query(By.css('[data-testId="table-tittle"]'))
                    .nativeElement.innerHTML.replace(/^\s+|\s+$/gm, '')
            ).toEqual('Current');
        });
        it('should show dropdown', () => {
            const dropdown: Dropdown = de.query(By.css('p-dropdown')).componentInstance;
            expect(dropdown.options).toEqual(dotContentCompareTableDataMock.versions);
        });
        it('should show selectButton', () => {
            const select: SelectButton = de.query(
                By.css('[data-testId="show-diff"]')
            ).componentInstance;
            expect(select.options).toEqual([
                { label: 'Diff', value: true },
                { label: 'Plain', value: false }
            ]);
        });
        it('should show versions selectButton with transformed label', async () => {
            const dropdown = de.query(By.css('p-dropdown'));

            dropdown.componentInstance.show();
            hostFixture.detectChanges();

            const versions = hostComponent.data.versions;

            dropdown.queryAll(By.css('.p-dropdown-item')).forEach((item, index) => {
                const textContent = item.nativeElement.textContent;
                const text = `${versions[index].modDate} by ${versions[index].modUserName}`;
                expect(textContent).toContain(text);
            });
        });
    });

    describe('fields', () => {
        it('should show image', () => {
            const workingImage: HTMLImageElement = de.query(
                By.css('[data-testId="table-image-working"]')
            ).nativeElement;
            const compareImage: HTMLImageElement = de.query(
                By.css('[data-testId="table-image-compare"]')
            ).nativeElement;
            expect(workingImage.src).toContain(
                `/dA/${dotContentCompareTableDataMock.working.image}/20q`
            );
            expect(workingImage.alt).toContain(dotContentCompareTableDataMock.working.image);
            expect(compareImage.src).toContain(
                `/dA/${dotContentCompareTableDataMock.compare.image}/20q`
            );
            expect(compareImage.alt).toContain(dotContentCompareTableDataMock.compare.image);
        });
        it('should show File', () => {
            const workingFile: DotContentComparePreviewFieldComponent = de.query(
                By.css('[data-testId="table-file-working"]')
            ).componentInstance;
            const compareIFile: DotContentComparePreviewFieldComponent = de.query(
                By.css('[data-testId="table-file-compare"]')
            ).componentInstance;
            expect(workingFile.fileURL).toContain(
                `/dA/${dotContentCompareTableDataMock.working.file}/fileAsset/`
            );
            expect(workingFile.label).toContain(
                `/dA/${dotContentCompareTableDataMock.working.file}/fileAsset/`
            );
            expect(compareIFile.fileURL).toContain(
                `/dA/${dotContentCompareTableDataMock.compare.file}/fileAsset/`
            );
            expect(compareIFile.label).toEqual(
                '/dA/<del class="diffmod">186283d7</del><ins class="diffmod">new</ins>-<del class="diffmod">0e9d-454b-90a5-010410d96926</del><ins class="diffmod">one</ins>/fileAsset/'
            );
        });
        it('should show Binary', () => {
            const workingBinary: DotContentComparePreviewFieldComponent = de.query(
                By.css('[data-testId="table-binary-working"]')
            ).componentInstance;
            const compareBinary: DotContentComparePreviewFieldComponent = de.query(
                By.css('[data-testId="table-binary-compare"]')
            ).componentInstance;
            expect(workingBinary.fileURL).toContain(
                dotContentCompareTableDataMock.working.binaryVersion
            );
            expect(workingBinary.label).toContain(
                dotContentCompareTableDataMock.working.binaryVersion
            );
            expect(compareBinary.fileURL).toContain(
                dotContentCompareTableDataMock.compare.binaryVersion
            );
            expect(compareBinary.label).toEqual(
                '/dA/<del class="diffmod">21ae95f9</del><ins class="diffmod">d094b42f</ins>-<del class="diffmod">357d</del><ins class="diffmod">5ef8</ins>-<del class="diffmod">4f1e</del><ins class="diffmod">4bcc</ins>-<del class="diffmod">b677</del><ins class="diffmod">8783</ins>-<del class="diffmod">1fc23ccde394</del><ins class="diffmod">73ff39a4c6e6</ins>/binary/<del class="diffmod">costarica</del><ins class="diffmod">leon</ins>.png'
            );
        });
        it('should show json field', () => {
            const workingJson = de.query(By.css('[data-testId="table-json-working"]')).nativeElement
                .innerHTML;
            const compareJson = de.query(By.css('[data-testId="table-json-compare"]')).nativeElement
                .innerHTML;

            expect(workingJson).toContain('{\n' + '  "name": "John"\n' + '}');
            expect(compareJson).toEqual(
                '{\n' +
                    '  "name": "<del class="diffmod">John</del><ins class="diffmod">David</ins>"\n' +
                    '}'
            );
        });

        it('should show others fields', () => {
            const workingField = de.query(By.css('[data-testId="table-field-working"]'))
                .nativeElement.innerHTML;
            const compareFiled = de.query(By.css('[data-testId="table-field-compare"]'))
                .nativeElement.innerHTML;

            expect(workingField).toEqual(dotContentCompareTableDataMock.working.text);
            expect(compareFiled).toEqual(
                '<del class="diffmod">Hi</del><ins class="diffmod">Hello</ins>'
            );
        });

        describe('diff disable', () => {
            beforeEach(() => {
                hostComponent.showDiff = false;
                hostFixture.detectChanges();
            });
            it('should show File', () => {
                const compareIFile: DotContentComparePreviewFieldComponent = de.query(
                    By.css('[data-testId="table-file-compare"]')
                ).componentInstance;
                expect(compareIFile.label).toEqual('/dA/new-one/fileAsset/');
            });
            it('should show Binary', () => {
                const compareBinary: DotContentComparePreviewFieldComponent = de.query(
                    By.css('[data-testId="table-binary-compare"]')
                ).componentInstance;
                expect(compareBinary.label).toEqual(
                    dotContentCompareTableDataMock.compare.binaryVersion
                );
            });
            it('should show others fields', () => {
                const compareFiled = de.query(By.css('[data-testId="table-field-compare"]'))
                    .nativeElement.innerHTML;
                expect(compareFiled).toEqual(dotContentCompareTableDataMock.compare.text);
            });
        });
    });

    describe('events', () => {
        it('should emit changeVersion', () => {
            jest.spyOn(hostComponent.changeVersion, 'emit');
            const dropdown: Dropdown = de.query(By.css('p-dropdown')).componentInstance;
            dropdown.onChange.emit({ value: 'test', originalEvent: createFakeEvent('click') });

            expect(hostComponent.changeVersion.emit).toHaveBeenCalledWith('test');
        });
        it('should emit changeDiff', () => {
            jest.spyOn(hostComponent.changeDiff, 'emit');
            const select: SelectButton = de.query(
                By.css('[data-testId="show-diff"]')
            ).componentInstance;
            select.onChange.emit({ value: true });

            expect(hostComponent.changeDiff.emit).toHaveBeenCalledWith(true);
        });

        it('should emit bring back', () => {
            jest.spyOn(hostComponent.bringBack, 'emit');
            const button = de.query(By.css('[data-testId="table-bring-back"]'));

            button.triggerEventHandler('click', '');

            expect(hostComponent.bringBack.emit).toHaveBeenCalledWith(
                dotContentCompareTableDataMock.compare.inode
            );
        });
    });
});
