import readme from './readme.md';
import { text } from '@storybook/addon-knobs';

export default {
    title: 'Elements',
    parameters: {
        notes: readme
    }
};

const contentletMock = {
    typeVariable: 'Image',
    modDate: '2/5/2020 11:50AM',
    __wfstep__: 'Published',
    baseType: 'FILEASSET',
    inode: 'c68db8ec-b523-41b7-82bd-fcb7533d3cfa',
    __title__: 'pinos.jpg',
    Identifier: '10885ceb-7457-4571-bdbe-b2a2c0198bd1',
    permissions:
        'P654b0931-1027-41f7-ad4d-173115ed8ec1.2P P654b0931-1027-41f7-ad4d-173115ed8ec1.1P ',
    contentStructureType: '4',
    working: 'true',
    locked: 'false',
    live: 'true',
    owner: 'dotcms.org.1',
    identifier: '10885ceb-7457-4571-bdbe-b2a2c0198bd1',
    wfActionMapList: '[]',
    languageId: '1',
    __icon__: 'jpgIcon',
    statusIcons: '<span></span>',
    hasLiveVersion: 'false',
    deleted: 'false',
    structureInode: 'd5ea385d-32ee-4f35-8172-d37f58d9cd7a',
    __type__: '<div></div>',
    ownerCanRead: 'false',
    hasTitleImage: 'true',
    modUser: 'Admin User',
    ownerCanWrite: 'false',
    ownerCanPublish: 'false',
    title: '',
    sysPublishDate: '',
    mediaType: ''
};

export const contentletThumbnail = () => {
    const props = [
        {
            name: 'height',
            content: text('Height', '200px')
        },

        {
            name: 'width',
            content: text('Width', '200px')
        },
        {
            name: 'alt',
            content: text('Alt', 'Image Description')
        },

        {
            name: 'iconSize',
            content: text('Icon Size', '100px')
        }
    ];

    const fileIcon = document.createElement('dot-contentlet-thumbnail');

    fileIcon.contentlet = contentletMock;

    props.forEach(({ name, content }) => {
        fileIcon[name] = content;
    });

    return fileIcon;
};
