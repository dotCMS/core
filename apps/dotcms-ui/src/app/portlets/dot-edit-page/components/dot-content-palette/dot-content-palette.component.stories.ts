import { DotContentPaletteComponent } from '@portlets/dot-edit-page/components/dot-content-palette/dot-content-palette.component';
import { moduleMetadata } from '@storybook/angular';
import { CommonModule } from '@angular/common';
import { Meta, Story } from '@storybook/angular/types-6-0';
import { DotIconModule } from '@dotcms/ui';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { Injectable } from '@angular/core';
import { DotMessagePipe } from '@dotcms/app/view/pipes';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';

const data = [
    {
        baseType: 'CONTENT',
        clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
        defaultType: false,
        detailPage: '1ef9be0e-7610-4c69-afdb-d304c8aabfac',
        fixed: false,
        folder: 'SYSTEM_FOLDER',
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        iDate: 1562940705000,
        icon: 'cloud',
        id: 'a1661fbc-9e84-4c00-bd62-76d633170da3',
        layout: [],
        modDate: 1626819743000,
        multilingualable: false,
        nEntries: 69,
        name: 'Product',
        sortOrder: 0,
        system: false,
        systemActionMappings: [],
        urlMapPattern: '/store/products/{urlTitle}',
        variable: 'Product',
        versionable: true,
        workflows: []
    },
    {
        baseType: 'CONTENT',
        clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
        defaultType: false,
        description: 'Travel Blog',
        detailPage: '8a14180a-4144-4807-80c4-b7cad20ac57b',
        fixed: false,
        folder: 'SYSTEM_FOLDER',
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        iDate: 1543419364000,
        icon: 'alt_route',
        id: '799f176a-d32e-4844-a07c-1b5fcd107578',
        layout: [],
        modDate: 1626819718000,
        multilingualable: false,
        nEntries: 6,
        name: 'Blog',
        publishDateVar: 'postingDate',
        sortOrder: 0,
        system: false,
        urlMapPattern: '/blog/post/{urlTitle}',
        variable: 'Blog',
        versionable: true,
        workflows: []
    },
    {
        baseType: 'FORM',
        clazz: 'com.dotcms.contenttype.model.type.ImmutableFormContentType',
        defaultType: false,
        description: 'General Contact Form',
        fixed: false,
        folder: 'SYSTEM_FOLDER',
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        iDate: 1563384216000,
        icon: 'cloud',
        id: '897cf4a9-171a-4204-accb-c1b498c813fe',
        layout: [],
        modDate: 1626818557000,
        multilingualable: false,
        nEntries: 0,
        name: 'Contact',
        sortOrder: 0,
        system: false,
        variable: 'Contact',
        versionable: true,
        workflows: []
    },
    {
        baseType: 'CONTENT',
        clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
        defaultType: false,
        fixed: false,
        folder: 'SYSTEM_FOLDER',
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        iDate: 1555017311000,
        icon: 'person',
        id: '6044a806-f462-4977-a353-57539eac2a2c',
        layout: [],
        modDate: 1626818557000,
        multilingualable: false,
        nEntries: 6,
        name: 'Long name Blog Comment',
        sortOrder: 0,
        system: false,
        variable: 'BlogComment',
        versionable: true,
        workflows: []
    }
];

@Injectable()
class MockDotContentletEditorService {
    setDraggedContentType = () => {};
}

const messageServiceMock = new MockDotMessageService({
    structure: 'Content Type'
});

export default {
    title: 'DotCMS/ Content Palette',
    component: DotContentPaletteComponent,
    decorators: [
        moduleMetadata({
            declarations: [DotContentPaletteComponent, DotMessagePipe],
            imports: [CommonModule, DotIconModule],
            providers: [
                { provide: DotContentletEditorService, useClass: MockDotContentletEditorService },
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        })
    ],
    args: {
        items: data
    }
} as Meta;

export const Default: Story<DotContentPaletteComponent> = (props) => {
    return {
        moduleMetadata: {
            declarations: [DotContentPaletteComponent]
        },
        component: DotContentPaletteComponent,
        props,
        template: `<dot-content-palette [items]='items'></dot-content-palette>`
    };
};

export const Empty: Story<DotContentPaletteComponent> = (props) => {
    return {
        moduleMetadata: {
            declarations: [DotContentPaletteComponent]
        },
        component: DotContentPaletteComponent,
        props,
        template: `<dot-content-palette [items]='[]'></dot-content-palette>`
    };
};
