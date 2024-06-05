const data = [
    {
        path: 'nicotest/',
        folders: [
            {
                key: 'nicotest/level1/',
                label: 'nicotest/level1/',
                data: {
                    addChildrenAllowed: true,
                    hostName: 'nicotest',
                    path: '/level1/',
                    type: 'folder'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                leaf: false
            },
            {
                key: 'nicotest/level1b/',
                label: 'nicotest/level1b/',
                data: {
                    addChildrenAllowed: true,
                    hostName: 'nicotest',
                    path: '/level1b/',
                    type: 'folder'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                leaf: false
            }
        ]
    },
    {
        path: 'nicotest/level1/',
        folders: [
            {
                key: 'nicotest/level1/1-1/',
                label: 'nicotest/level1/1-1/',
                data: {
                    addChildrenAllowed: true,
                    hostName: 'nicotest',
                    path: '/level1/1-1/',
                    type: 'folder'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                leaf: false
            },
            {
                key: 'nicotest/level1/1-2/',
                label: 'nicotest/level1/1-2/',
                data: {
                    addChildrenAllowed: true,
                    hostName: 'nicotest',
                    path: '/level1/1-2/',
                    type: 'folder'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                leaf: false
            }
        ]
    },
    {
        path: 'nicotest/level1/1-2/',
        folders: [
            {
                key: 'nicotest/level1/1-2/1-3/',
                label: 'nicotest/level1/1-2/1-3/',
                data: {
                    addChildrenAllowed: true,
                    hostName: 'nicotest',
                    path: '/level1/1-2/1-3/',
                    type: 'folder'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                leaf: false
            }
        ]
    },
    {
        path: 'nicotest/level1/1-2/1-3/',
        folders: []
    }
];
const response = data.reverse();
const [mainNode] = response;
const rta = response.reduce(
    (rta, node, index, array) => {
        const next = array[index + 1];
        if (next) {
            folder = next.folders.find((item) => item.key === node.path);
            if (folder) {
                folder.children = node.folders;
                console.log(mainNode.path, folder.key);
                if (mainNode.path === folder.key) {
                    rta.node = folder;
                }
            }
        }
        rta.tree = node;
        return rta;
    },
    { node: null, tree: null }
);

const buildTree = (tree) => {};

console.log(JSON.stringify(rta, null, 2));
