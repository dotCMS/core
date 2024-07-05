import { Component } from '@angular/core';

@Component({
    selector: 'dotcms-root',
    templateUrl: './app.component.html'
})
export class AppComponent {
    title = 'dotcms-binary-field-builder';
    field = {
        clazz: 'com.dotcms.contenttype.model.field.ImmutableBinaryField',
        contentTypeId: 'd1901a41d38b6686dd5ed8f910346d7a',
        dataType: 'SYSTEM',
        fieldType: 'Binary',
        fieldTypeLabel: 'Binary',
        fieldVariables: [
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                fieldId: '5df3f8fc49177c195740bcdc02ec2db7',
                id: '1ff1ff05-b9fb-4239-ad3d-b2cfaa9a8406',
                key: 'accept',
                value: 'image/*,.html,.ts'
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                fieldId: '5df3f8fc49177c195740bcdc02ec2db7',
                id: '1ff1ff05-b9fb-4239-ad3d-b2cfaa9a8406',
                key: 'maxFileSize',
                value: '50000'
            }
        ],
        hint: 'Helper label to be displayed below the field',
        fixed: false,
        forceIncludeInApi: false,
        iDate: 1698153564000,
        id: '5df3f8fc49177c195740bcdc02ec2db7',
        indexed: false,
        listed: false,
        modDate: 1698153564000,
        name: 'Binary Field',
        readOnly: false,
        required: false,
        searchable: false,
        sortOrder: 2,
        unique: false,
        variable: 'binaryField'
    };
    content = {
        publishDate: '2023-10-24 13:21:49.682',
        inode: 'b22aa2f3-12af-4ea8-9d7d-164f98ea30b1',
        binaryField2: '/dA/af9294c29906dea7f4a58d845f569219/binaryField2/New-Image.png',
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        binaryField2Version: '/dA/b22aa2f3-12af-4ea8-9d7d-164f98ea30b1/binaryField2/New-Image.png',
        locked: false,
        stInode: 'd1901a41d38b6686dd5ed8f910346d7a',
        contentType: 'BinaryField',
        identifier: 'af9294c29906dea7f4a58d845f569219',
        folder: 'SYSTEM_FOLDER',
        hasTitleImage: true,
        sortOrder: 0,
        binaryField2MetaData: {
            modDate: 1698153707197,
            sha256: 'e84030fe91978e483e34242f0631a81903cf53a945475d8dcfbb72da484a28d5',
            length: 29848,
            title: 'New-Image.png',
            version: 20220201,
            isImage: true,
            fileSize: 29848,
            name: 'New-Image.png',
            width: 738,
            contentType: 'image/png',
            height: 435
        },
        hostName: 'demo.dotcms.com',
        modDate: '2023-10-24 13:21:49.682',
        title: 'af9294c29906dea7f4a58d845f569219',
        baseType: 'CONTENT',
        archived: false,
        working: true,
        live: true,
        owner: 'dotcms.org.1',
        binaryField2ContentAsset: 'af9294c29906dea7f4a58d845f569219/binaryField2',
        languageId: 1,
        url: '/content.b22aa2f3-12af-4ea8-9d7d-164f98ea30b1',
        titleImage: 'binaryField2',
        modUserName: 'Admin User',
        hasLiveVersion: true,
        modUser: 'dotcms.org.1',
        __icon__: 'contentIcon',
        contentTypeIcon: 'event_note',
        variant: 'DEFAULT'
    };
}
