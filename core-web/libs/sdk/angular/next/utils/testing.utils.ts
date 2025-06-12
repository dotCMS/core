import { DotCMSPageAsset, DotCMSBasicContentlet } from '@dotcms/types';

export const PageResponseMock: DotCMSPageAsset = {
    canCreateTemplate: true,
    containers: {
        '//demo.dotcms.com/application/containers/default/': {
            containerStructures: [
                {
                    id: '77ec7720-bad8-431c-a0a3-6443fe87af73',
                    structureId: 'ce7295c8-df36-46c0-9c98-2fb764e9ec1c',
                    containerInode: 'f6a7e729-56f5-4fc0-a045-9711960b7427',
                    containerId: '69b3d24d-7e80-4be6-b04a-d352d16493ee',
                    code: '#dotParse("//demo.dotcms.com/application/containers/default/calltoaction.vtl")',
                    contentTypeVar: 'CallToAction'
                }
            ],
            contentlets: {
                'uuid-1': [
                    {
                        hostName: 'demo.dotcms.com',
                        modDate: '1599064929560',
                        imageMetaData: {
                            modDate: 1716268464265,
                            sha256: '041e8f2a721bf85fc833db50666a892ca9c5fcf816091cb6b6123a08ca701085',
                            length: 33386,
                            title: 'diving.jpg',
                            editableAsText: false,
                            version: 20220201,
                            isImage: true,
                            fileSize: 33386,
                            name: 'diving.jpg',
                            width: 270,
                            contentType: 'image/jpeg',
                            height: 270
                        },
                        publishDate: 1599064929608,
                        description:
                            'World-class diving is at your doorstep. From shore or by boat, you have exclusive access to miles of pristine reefs, where diverse and dramatic undersea landscapes harbor the highest level of marine biodiversity on the planet.',
                        title: 'Diving',
                        body: '<p>Our dive destinations where created to deliver the ultimate dive experience and was established following an extensive search to identify the perfect location for a dive resort in terms of geography, climate, oceanic topography and marine biodiversity. Having identified the premier location, we developed resorts that, despite its remote position, offers a plenitude of facilities and comforts to make a dive trip, and all that surrounds it, an experience that you will cherish.</p>\n<p><strong>Scuba</strong><br />Unlimited diving on pristine, protected coral reefs for all divers be they open-circuit, technical or rebreather.</p>\n<p><strong>Snorkeling</strong><br />Ideal place for snorkelers. The house reef and many dive sites have beautiful shallows on the coral tops.</p>\n<p><strong>Private Dive/Snorkel Guides</strong><br />Let us fulfill your innermost wishes and tailor an exclusive, personalised diving experience to fit your individual needs and desires.</p>\n<p><strong>Underwater photography</strong><br />The photo opportunities here are stupendous and we offer support and equipment of a level that satisfies the many pros who visit us.</p>',
                        baseType: 'CONTENT',
                        inode: 'b77151f2-7206-4b03-862d-68217432d54d',
                        archived: false,
                        ownerUserName: 'Admin User',
                        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                        working: true,
                        locked: false,
                        stInode: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
                        contentType: 'Activity',
                        live: true,
                        owner: 'dotcms.org.1',
                        imageVersion: '/dA/b77151f2-7206-4b03-862d-68217432d54d/image/diving.jpg',
                        identifier: '4694d40b-d9be-4e09-b031-64ee3e7c9642',
                        image: '/dA/4694d40b-d9be-4e09-b031-64ee3e7c9642/image/diving.jpg',
                        imageContentAsset: '4694d40b-d9be-4e09-b031-64ee3e7c9642/image',
                        urlTitle: 'diving',
                        publishUserName: 'Admin User',
                        publishUser: 'dotcms.org.1',
                        languageId: 1,
                        URL_MAP_FOR_CONTENT: '/activities/diving',
                        creationDate: 1599064929560,
                        url: '/content.e9332c43-dfc2-4aa1-b0d6-60ca3df7cb76',
                        tags: 'snorkeling,waterenthusiast,diving,scuba',
                        titleImage: 'image',
                        modUserName: 'Admin User',
                        urlMap: '/activities/diving',
                        hasLiveVersion: true,
                        folder: 'SYSTEM_FOLDER',
                        hasTitleImage: true,
                        sortOrder: 0,
                        modUser: 'dotcms.org.1',
                        onNumberOfPages: '3'
                    } as unknown as DotCMSBasicContentlet
                ]
            },
            container: {
                identifier: '69b3d24d-7e80-4be6-b04a-d352d16493ee',
                uuid: '69b3d24d-7e80-4be6-b04a-d352d16493ee',
                iDate: 1687784158222,
                type: 'containers',
                owner: 'system',
                inode: 'f6a7e729-56f5-4fc0-a045-9711960b7427',
                source: 'FILE',
                title: 'Default',
                friendlyName: 'container',
                modDate: 1687784158222,
                modUser: 'system',
                sortOrder: 0,
                showOnMenu: false,
                code: '',
                maxContentlets: 25,
                useDiv: false,
                preLoop: '',
                postLoop: '',
                staticify: false,
                notes: '',
                languageId: 1,
                path: '',
                live: true,
                locked: false,
                working: true,
                deleted: false,
                name: 'Default',
                archived: false,
                permissionId: '69b3d24d-7e80-4be6-b04a-d352d16493ee',
                versionId: '69b3d24d-7e80-4be6-b04a-d352d16493ee',
                versionType: 'containers',
                permissionType: 'com.dotmarketing.portlets.containers.model.Container',
                categoryId: 'f6a7e729-56f5-4fc0-a045-9711960b7427',
                idate: 1687784158222,
                new: false,
                acceptTypes: '',
                contentlets: [],
                parentPermissionable: {
                    Inode: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                    Identifier: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                    permissionByIdentifier: true,
                    type: 'host',
                    identifier: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                    permissionId: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                    permissionType: 'com.dotmarketing.portlets.contentlet.model.Contentlet',
                    inode: '48190c8c-42c4-46af-8d1a-0cd5db894797'
                }
            }
        },
        '//demo.dotcms.com/application/containers/banner/': {
            containerStructures: [
                {
                    id: 'f87da091-44eb-4662-8cd1-16d31afa14a8',
                    structureId: '4c441ada-944a-43af-a653-9bb4f3f0cb2b',
                    containerInode: '1d9165ad-f8ea-4006-8017-b103eb0d9f9b',
                    containerId: '5e8c9a71-8cae-4d96-a2f7-d25b9cf69a83',
                    code: '#dotParse("//demo.dotcms.com/application/containers/banner/banner.vtl")',
                    contentTypeVar: 'Banner'
                }
            ],
            contentlets: {
                'uuid-1': [
                    {
                        hostName: 'demo.dotcms.com',
                        modDate: '1716324004216',
                        imageMetaData: {
                            modDate: 1716268463471,
                            sha256: 'd9c3c87a691fe062ce370ddda2e9a71fa333739260711f93d5a82f56a08c1703',
                            length: 1158082,
                            title: 'adventure-boat-exotic-1371360.jpg',
                            editableAsText: false,
                            version: 20220201,
                            isImage: true,
                            fileSize: 1158082,
                            name: 'adventure-boat-exotic-1371360.jpg',
                            width: 1400,
                            contentType: 'image/jpeg',
                            height: 679
                        },
                        link: '/store/category/apparel/#products-title',
                        publishDate: 1716324004446,
                        caption: 'Look Great Doing it. Stock up on New Apparel!!',
                        title: 'Explore the World II',
                        baseType: 'CONTENT',
                        inode: '84534373-582a-4e62-8189-03abd0435778',
                        archived: false,
                        ownerUserName: 'Admin User',
                        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                        working: true,
                        locked: false,
                        stInode: '4c441ada-944a-43af-a653-9bb4f3f0cb2b',
                        contentType: 'Banner',
                        live: true,
                        owner: 'dotcms.org.1',
                        imageVersion:
                            '/dA/84534373-582a-4e62-8189-03abd0435778/image/adventure-boat-exotic-1371360.jpg',
                        identifier: '2e5d54e6-7ea3-4d72-8577-b8731b206ca0',
                        buttonText: '20% Off - Shop Now',
                        image: '/dA/2e5d54e6-7ea3-4d72-8577-b8731b206ca0/image/adventure-boat-exotic-1371360.jpg',
                        imageContentAsset: '2e5d54e6-7ea3-4d72-8577-b8731b206ca0/image',
                        publishUserName: 'Admin User',
                        publishUser: 'dotcms.org.1',
                        languageId: 1,
                        creationDate: 1599247961410,
                        textColor: '#FFFFFF',
                        url: '/content.8b12d62d-5253-44b9-a2d0-8c4e5ec06e68',
                        tags: 'beach',
                        layout: '1',
                        titleImage: 'image',
                        modUserName: 'Admin User',
                        hasLiveVersion: true,
                        folder: 'SYSTEM_FOLDER',
                        hasTitleImage: true,
                        sortOrder: 0,
                        modUser: 'dotcms.org.1',
                        onNumberOfPages: '1'
                    } as unknown as DotCMSBasicContentlet
                ]
            },
            container: {
                archived: false,
                categoryId: '1d9165ad-f8ea-4006-8017-b103eb0d9f9b',
                deleted: false,
                friendlyName: 'container',
                iDate: 1600437115745,
                idate: 1600437115745,
                identifier: '5e8c9a71-8cae-4d96-a2f7-d25b9cf69a83',
                inode: '1d9165ad-f8ea-4006-8017-b103eb0d9f9b',
                languageId: 1,
                live: true,
                locked: false,
                maxContentlets: 25,
                sortOrder: 0,
                source: 'FILE',
                title: 'Banner',
                type: 'containers',
                useDiv: false,
                versionId: '5e8c9a71-8cae-4d96-a2f7-d25b9cf69a83',
                versionType: 'containers',
                working: true,
                uuid: '5e8c9a71-8cae-4d96-a2f7-d25b9cf69a83',
                modDate: 1600437115745,
                modUser: 'system',
                showOnMenu: false,
                preLoop: '',
                postLoop: '',
                staticify: false,
                notes: '',
                path: '',
                permissionId: '5e8c9a71-8cae-4d96-a2f7-d25b9cf69a83',
                permissionType: 'com.dotmarketing.portlets.containers.model.Container',
                parentPermissionable: {
                    Inode: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                    Identifier: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                    permissionByIdentifier: true,
                    type: 'host',
                    identifier: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                    permissionId: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                    permissionType: 'com.dotmarketing.portlets.contentlet.model.Contentlet',
                    inode: '48190c8c-42c4-46af-8d1a-0cd5db894797'
                },
                acceptTypes: '',
                contentlets: [],
                name: 'Banner',
                new: false
            }
        },
        SYSTEM_CONTAINER: {
            containerStructures: [],
            contentlets: {
                'uuid-1': []
            },
            container: {
                archived: false,
                categoryId: 'SYSTEM_CONTAINER',
                code: '<style>\n.system_container {\n    border: 1px solid #b3b1b8;\n    gap: 20px;\n    padding: 10px;\n    display: flex;\n    background: white;\n    margin: 20px;\n}\n\n.system_container .card_logo {\n    width: 150px;\n    height: 150px;\n    overflow: hidden;\n    display: flex;\n    align-items: center;\n    justify-content: center;\n}\n    \n.system_container .card_logo .material-icons {\n    font-size: 150px\n}\n\n.system_container .card_logo img {\n    width: 100%;\n    height: 100%;\n    object-fit: cover;\n    object-position: center;\n}\n\n.system_container .card_body {\n    width: calc(100% - 150px);\n    flex-grow: 1;\n    display: flex;\n    flex-direction: column;\n}\n\n.system_container .card_header {\n    flex-grow: 1;\n    overflow: hidden;\n    margin-bottom: 15px;\n}\n\n.system_container .card_footer {\n    display: flex;\n    align-items: center;\n    gap: 10px;\n}\n\n.system_container .card_title h1 {\n    overflow: hidden;\n    font-size: 40px;\n    margin: 0;\n    margin-bottom: 10px;\n    text-overflow: ellipsis;\n    white-space: nowrap;\n}\n\n.system_container .content_type {\n    background-color: #b3b1b8;\n    display: inline-block;\n    color: white;\n    padding: 4px 6px;\n    text-align: center;\n    border-radius: 5px;\n    font-size: .75rem;\n}\n\n.system_container .lang {\n    border-radius: 2px;\n    font-size: 12px;\n    padding: 0 0.25em;\n    display: inline-block;\n    background-color: transparent;\n    border: solid 1px #6f5fa3;\n    color: #6f5fa3;\n}\n\n.system_container .status {\n    border-radius: 50%;\n    border: solid 2px;\n    box-sizing: border-box;\n    height: 20px;\n    width: 20px;\n}\n\n.system_container .status.live {\n    background: #27b970;\n}\n\n.system_container .status.working {\n    background: transparent;\n}\n\n@media (max-width: 640px) {\n    .system_container {\n        flex-direction: column;\n        max-width: 250px;\n        gap: 5px;\n        padding: 0;\n        margin: 20px auto;\n    }\n    \n    .system_container .card_logo {\n        width: 250px;\n        height: 200px;\n        overflow: hidden;\n        font-size: 200px;\n    }\n    \n    .system_container .card_logo .material-icons {\n        font-size: 200px;\n    }\n    \n    .system_container .card_body {\n        padding: 10px;\n        width: 100%;\n    }\n    \n    .system_container .card_title {\n        width: calc(100% - 20px);\n        font-size: 40px;\n    }\n}\n</style>\n\n\n#set($language = $languagewebapi.getLanguage("$!{dotContentMap.languageId}"))\n\n<div class="system_container">\n    <div class="card_logo" id="logo-image-$!{dotContentMap.inode}"></div>\n    <div class="card_body">\n        <div class="card_header">\n            <div class="card_title">\n                <h1>$!{dotContentMap.title}</h1>\n            </div>\n           <span class="content_type">$!{dotContentMap.contentType.name}</span>\n        </div>\n        <div class="card_footer">\n            <div class="status #if($dotContentMap.isLive())live#{else}working#end"></div>\n            <span class="lang">$!{language}</span>\n        </div>\n    </div>\n</div>\n\n<script type="text/javascript">\n\n    // Adding the image using JavaScript avoids displaying a broken image for milliseconds\n    (function autoexecute() {\n        const image = new Image();\n        image.onload=imageFound;\n        image.onerror=imageNotFound;\n        image.src="/dA/$!{dotContentMap.inode}";\n\n        function imageFound() {\n            const div = document.getElementById(`logo-image-$!{dotContentMap.inode}`);\n            div.appendChild(image);\n        }\n\n        function imageNotFound() {\n            const div = document.getElementById(`logo-image-$!{dotContentMap.inode}`);\n            div.innerHTML = `<span class="material-icons">$!{dotContentMap.contentType.icon}</span>`;\n        }\n    })()\n    \n</script>',
                deleted: false,
                friendlyName: 'System Container',
                iDate: 1716356412597,
                idate: 1716356412597,
                identifier: 'SYSTEM_CONTAINER',
                inode: 'SYSTEM_CONTAINER',
                live: true,
                locked: false,
                useDiv: false,
                versionId: 'SYSTEM_CONTAINER',
                versionType: 'containers',
                working: false,
                uuid: 'SYSTEM_CONTAINER',
                type: 'containers',
                source: 'FILE',
                title: 'System Container',
                modDate: 1716356412597,
                modUser: 'system',
                sortOrder: 0,
                showOnMenu: false,
                maxContentlets: 25,
                preLoop: '',
                postLoop: '',
                staticify: false,
                notes: '',
                languageId: 1,
                path: '',
                permissionId: 'SYSTEM_CONTAINER',
                permissionType: 'com.dotmarketing.portlets.containers.model.Container',
                parentPermissionable: {
                    Inode: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                    Identifier: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                    permissionByIdentifier: true,
                    type: 'host',
                    identifier: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                    permissionId: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                    permissionType: 'com.dotmarketing.portlets.contentlet.model.Contentlet',
                    inode: '48190c8c-42c4-46af-8d1a-0cd5db894797'
                },
                acceptTypes: '',
                contentlets: [],
                name: 'System Container',
                new: false
            }
        }
    },
    layout: {
        width: '',
        title: 'anonymouslayout1600437132653',
        header: true,
        footer: true,
        pageWidth: '',
        layout: JSON.stringify({
            rows: [
                {
                    identifier: 1,
                    columns: [
                        {
                            containers: [
                                {
                                    identifier: '//demo.dotcms.com/application/containers/banner/',
                                    uuid: '1',
                                    historyUUIDs: []
                                }
                            ],
                            widthPercent: 100,
                            leftOffset: 1,
                            styleClass: 'banner-tall',
                            preview: false,
                            width: 12,
                            left: 0
                        }
                    ],
                    styleClass: 'p-0 banner-tall'
                },
                {
                    identifier: 2,
                    columns: [
                        {
                            containers: [
                                {
                                    identifier: '//demo.dotcms.com/application/containers/default/',
                                    uuid: '2',
                                    historyUUIDs: []
                                }
                            ],
                            widthPercent: 50,
                            leftOffset: 1,
                            styleClass: 'content-column',
                            preview: false,
                            width: 6,
                            left: 0
                        },
                        {
                            containers: [
                                {
                                    identifier: 'SYSTEM_CONTAINER',
                                    uuid: '3',
                                    historyUUIDs: []
                                }
                            ],
                            widthPercent: 50,
                            leftOffset: 7,
                            styleClass: 'content-column',
                            preview: false,
                            width: 6,
                            left: 6
                        }
                    ],
                    styleClass: 'content-row'
                },
                {
                    identifier: 3,
                    columns: [
                        {
                            containers: [
                                {
                                    identifier: '//demo.dotcms.com/application/containers/default/',
                                    uuid: '4',
                                    historyUUIDs: []
                                }
                            ],
                            widthPercent: 100,
                            leftOffset: 1,
                            styleClass: 'footer-column',
                            preview: false,
                            width: 12,
                            left: 0
                        }
                    ],
                    styleClass: 'footer-row'
                }
            ]
        }),
        body: {
            rows: [
                {
                    identifier: 1,
                    columns: [
                        {
                            containers: [
                                {
                                    identifier: '//demo.dotcms.com/application/containers/banner/',
                                    uuid: '1',
                                    historyUUIDs: []
                                }
                            ],
                            widthPercent: 100,
                            leftOffset: 1,
                            styleClass: 'banner-tall',
                            preview: false,
                            width: 12,
                            left: 0
                        }
                    ],
                    styleClass: 'p-0 banner-tall'
                },
                {
                    identifier: 2,
                    columns: [
                        {
                            containers: [
                                {
                                    identifier: '//demo.dotcms.com/application/containers/default/',
                                    uuid: '2',
                                    historyUUIDs: []
                                }
                            ],
                            widthPercent: 50,
                            leftOffset: 1,
                            styleClass: 'content-column',
                            preview: false,
                            width: 6,
                            left: 0
                        },
                        {
                            containers: [
                                {
                                    identifier: 'SYSTEM_CONTAINER',
                                    uuid: '3',
                                    historyUUIDs: []
                                }
                            ],
                            widthPercent: 50,
                            leftOffset: 7,
                            styleClass: 'content-column',
                            preview: false,
                            width: 6,
                            left: 6
                        }
                    ],
                    styleClass: 'content-row'
                },
                {
                    identifier: 3,
                    columns: [
                        {
                            containers: [
                                {
                                    identifier: '//demo.dotcms.com/application/containers/default/',
                                    uuid: '4',
                                    historyUUIDs: []
                                }
                            ],
                            widthPercent: 100,
                            leftOffset: 1,
                            styleClass: 'footer-column',
                            preview: false,
                            width: 12,
                            left: 0
                        }
                    ],
                    styleClass: 'footer-row'
                }
            ]
        },
        sidebar: {
            preview: false,
            containers: [],
            location: '',
            widthPercent: 0,
            width: ''
        }
    },
    page: {
        contentType: 'htmlpageasset',
        template: 'fdc739f6-fe53-4271-9c8c-a3e05d12fcac',
        modDate: 1689887118429,
        metadata: '',
        cachettl: '0',
        pageURI: '/index',
        title: 'Home',
        type: 'htmlpage',
        showOnMenu: 'false',
        httpsRequired: false,
        inode: 'f9ce2b8e-eb06-4218-b036-9f266e2113b9',
        disabledWYSIWYG: [],
        seokeywords: '',
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        lastReview: 0,
        working: true,
        locked: true,
        stInode: 'c541abb1-69b3-4bc5-8430-5e09e5239cc8',
        friendlyName: 'TravelLux - Your Destination for Exclusive Experiences',
        live: true,
        owner: 'dotcms.org.1',
        identifier: 'a9f30020-54ef-494e-92ed-645e757171c2',
        nullProperties: [],
        friendlyname: 'TravelLux - Your Destination for Exclusive Experiences',
        pagemetadata: '',
        languageId: 1,
        url: '/index',
        seodescription: '',
        modUserName: 'Admin User',
        folder: 'SYSTEM_FOLDER',
        deleted: false,
        sortOrder: 0,
        modUser: 'dotcms.org.1',
        pageUrl: 'index',
        workingInode: 'f9ce2b8e-eb06-4218-b036-9f266e2113b9',
        shortyWorking: 'f9ce2b8eeb',
        canEdit: true,
        canRead: true,
        canLock: true,
        lockedOn: 1690313868046,
        lockedBy: 'dotcms.org.1',
        lockedByName: 'Admin User',
        liveInode: 'f9ce2b8e-eb06-4218-b036-9f266e2113b9',
        shortyLive: 'f9ce2b8eeb'
    },
    site: {
        lowIndexPriority: false,
        name: 'demo.dotcms.com',
        default: true,
        aliases: 'localhost\n127.0.0.1',
        parent: true,
        tagStorage: 'SYSTEM_HOST',
        systemHost: false,
        inode: '59bb8831-6706-4589-9ca0-ff74016e02b2',
        versionType: 'contentlet',
        structureInode: '855a2d72-f2f3-4169-8b04-ac5157c4380c',
        hostname: 'demo.dotcms.com',
        hostThumbnail: null,
        owner: 'dotcms.org.1',
        permissionId: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        permissionType: 'com.dotmarketing.portlets.contentlet.model.Contentlet',
        type: 'contentlet',
        identifier: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        modDate: 1634235141702,
        host: 'SYSTEM_HOST',
        live: true,
        indexPolicy: 'WAIT_FOR',
        categoryId: '59bb8831-6706-4589-9ca0-ff74016e02b2',
        actionId: null,
        new: false,
        archived: false,
        locked: false,
        disabledWysiwyg: [],
        modUser: 'dotcms.org.1',
        working: true,
        titleImage: { present: false },
        folder: 'SYSTEM_FOLDER',
        htmlpage: false,
        fileAsset: false,
        vanityUrl: false,
        keyValue: false,
        title: 'demo.dotcms.com',
        languageId: 1,
        indexPolicyDependencies: 'DEFER',
        contentTypeId: '855a2d72-f2f3-4169-8b04-ac5157c4380c',
        versionId: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        lastReview: 0,
        nextReview: null,
        reviewInterval: null,
        sortOrder: 0,
        contentType: {
            owner: null,
            parentPermissionable: {
                Inode: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                Identifier: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                permissionByIdentifier: true,
                type: 'host',
                identifier: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                permissionId: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                permissionType: 'com.dotmarketing.portlets.contentlet.model.Contentlet',
                inode: '48190c8c-42c4-46af-8d1a-0cd5db894797'
            },
            permissionId: '48190c8c-42c4-46af-8d1a-0cd5db894797',
            permissionType: 'com.dotmarketing.portlets.contentlet.model.Contentlet'
        }
    },
    template: {
        iDate: 1562013807321,
        type: 'template',
        owner: '',
        inode: '446da365-352f-4ac9-8d0d-4083e68a787e',
        identifier: 'fdc739f6-fe53-4271-9c8c-a3e05d12fcac',
        source: 'DB',
        title: 'anonymous_layout_1600437132653',
        friendlyName: '',
        modDate: 1716492043079,
        modUser: 'system',
        sortOrder: 0,
        showOnMenu: false,
        image: '',
        drawed: true,
        drawedBody:
            '{"width":"","title":"anonymouslayout1600437132653","header":true,"footer":true,"body":{"rows":[{"columns":[{"containers":[{"identifier":"//demo.dotcms.com/application/containers/banner/","uuid":"1"}],"widthPercent":100,"leftOffset":1,"styleClass":"banner-tall","preview":false,"width":12,"left":0}],"styleClass":"p-0 banner-tall"}]}}',
        theme: 'd7b0ebc2-37ca-4a5a-b769-e8a3ff187661',
        anonymous: true,
        template: false,
        name: 'anonymous_layout_1600437132653',
        live: false,
        archived: false,
        locked: false,
        working: true,
        permissionId: 'fdc739f6-fe53-4271-9c8c-a3e05d12fcac',
        versionId: 'fdc739f6-fe53-4271-9c8c-a3e05d12fcac',
        versionType: 'template',
        deleted: false,
        permissionType: 'com.dotmarketing.portlets.templates.model.Template',
        categoryId: '446da365-352f-4ac9-8d0d-4083e68a787e',
        idate: 1562013807321,
        new: false,
        canEdit: true
    },
    viewAs: {
        language: {
            id: 1,
            languageCode: 'en',
            countryCode: 'US',
            language: 'English',
            country: 'United States'
        },
        mode: 'EDIT_MODE'
    }
};

export const PageResponseOneRowMock: DotCMSPageAsset = {
    ...PageResponseMock,
    layout: {
        ...PageResponseMock.layout,
        body: {
            ...PageResponseMock.layout.body,
            rows: [PageResponseMock.layout.body.rows[0]]
        }
    }
};

export const dotcmsContentletMock: DotCMSBasicContentlet = {
    archived: false,
    baseType: '',
    contentType: '',
    folder: '',
    hasTitleImage: false,
    host: '',
    hostName: '',
    identifier: '',
    inode: '',
    languageId: 1,
    live: false,
    locked: false,
    modDate: '',
    modUser: '',
    modUserName: '',
    owner: '',
    sortOrder: 1,
    stInode: '',
    title: 'This is my editable title',
    titleImage: '',
    url: '',
    working: false
};
