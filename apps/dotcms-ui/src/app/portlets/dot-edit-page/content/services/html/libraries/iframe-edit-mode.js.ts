export const MODEL_VAR_NAME = 'dotNgModel';

export const EDIT_PAGE_JS = `
(function () {
    var forbiddenTarget;
    let currentModel;

    function getContainers() {
        var containers = [];
        var containersNodeList = document.querySelectorAll('[data-dot-object="container"]');

        for (var i = 0; i < containersNodeList.length; i++) {
            containers.push(containersNodeList[i]);
        };

        return containers;
    }

    function getDotNgModel() {
        var model = [];
        getContainers().forEach(function(container) {
            var contentlets = Array.from(container.querySelectorAll('[data-dot-object="contentlet"]'));

            model.push({
                identifier: container.dataset.dotIdentifier,
                uuid: container.dataset.dotUuid,
                contentletsId: contentlets.map(function(contentlet) {
                    return contentlet.dataset.dotIdentifier;
                })
            });
        });
        return model;
    }

    function checkIfContentletTypeIsAccepted(el, target) {
        return el.dataset.dotBasetype === 'WIDGET' ||
               el.dataset.dotBasetype === 'FORM' ||
               target.dataset.dotAcceptTypes.indexOf(el.dataset.dotType) > -1;
    }

    function checkIfMaxLimitNotReached(target) {
        return Array.from(
                target.querySelectorAll("[data-dot-object='contentlet']:not(.gu-transit)")
            ).length < parseInt(target.dataset.maxContentlets, 10);
    }

    function checkIfContentletIsUnique(el, target) {
        return Array.from(target.querySelectorAll("[data-dot-object='contentlet']:not(.gu-transit)"))
            .map(node => node.dataset.dotInode).indexOf(el.dataset.dotInode) === -1;
    }

    var drake = dragula(
        getContainers(), {
        accepts: function (el, target, source, sibling) {
            var canDrop = false;
            if (target.dataset.dotObject === 'container') {
                canDrop = checkIfContentletTypeIsAccepted(el, target)
                            && checkIfMaxLimitNotReached(target)
                            && checkIfContentletIsUnique(el, target);
                if (!canDrop && target !== source) {
                    forbiddenTarget = target;
                    forbiddenTarget.classList.add('no')
                }
            }
            return canDrop;
        },
        invalid: function(el, handle) {
            return !handle.classList.contains('dotedit-contentlet__drag');
        }
    });

    drake.on('drag', function() {
        window.requestAnimationFrame(function() {
            const el = document.querySelector('.gu-mirror');
            const rect = el.getBoundingClientRect();
            let transform = 'rotate(4deg)';

            if (rect.width > 500) {
                const scale = 500 / rect.width;
                transform = transform + ' scale(' + scale + ') '
            }

            el.style.transform = transform;
        });

        currentModel = getDotNgModel();

    })

    drake.on('over', function(el, container, source) {
        container.classList.add('over')
    })

    drake.on('out', function(el, container, source) {
        container.classList.remove('over')
    })

    drake.on('dragend', function(el) {
        if (forbiddenTarget && forbiddenTarget.classList.contains('no')) {
            forbiddenTarget.classList.remove('no');
        }

        currentModel = [];
    });

    drake.on('drop', function(el, target, source, sibling) {
        const updatedModel = getDotNgModel();

        if (JSON.stringify(updatedModel) !== JSON.stringify(currentModel)) {
            window.${MODEL_VAR_NAME}.next({
                model: getDotNgModel(),
                type: 3,
            });
        }

        if (target !== source) {
            window.contentletEvents.next({
                name: 'relocate',
                data: {
                    container: {
                        identifier: target.dataset.dotIdentifier,
                        uuid: target.dataset.dotUuid
                    },
                    contentlet: {
                        identifier: el.dataset.dotIdentifier,
                        inode: el.dataset.dotInode
                    }
                }
            });
        }
    })

    window.getDotNgModel = getDotNgModel;

    var myAutoScroll = autoScroll([
        window
    ],{
        margin: 100,
        maxSpeed: 60,
        scrollWhenOutside: true,
        autoScroll: function(){
            // Only scroll when the pointer is down, and there is a child being dragged.
            return this.down && drake.dragging;
        }
    });

    // D&D DotAsset - Start

    function dotAssetCreate(options) {
        const data = {
            contentlet: {
                baseType: 'dotAsset',
                asset: options.file.id,
                hostFolder: options.folder,
                indexPolicy: 'WAIT_FOR'
            }
        };

        return fetch(options.url, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json;charset=UTF-8'
            },
            body: JSON.stringify(data)
        })
        .then(async (res) => {
            const error = {};
            try {
                const data = await res.json();
                if (res.status !== 200) {
                    let message = '';
                    try {
                        message = data.errors[0].message;
                    } catch(e) {
                        message = e.message;
                    }
                    error = {
                        message: message,
                        status: res.status
                    };
                }
    
                if (!!error.message) {
                    throw error;
                } else {
                    return data.entity;
                }
            } catch(e) {
                throw res;
            }
        })
    }

    function uploadBinaryFile(file, maxSize) {
        let path = '/api/v1/temp';
        path += maxSize ? '?maxFileLength=' + maxSize : '';
        const formData = new FormData();
        formData.append('file', file);
        return fetch(path, {
            method: 'POST',
            body: formData
        }).then(async (response) => {
            if (response.status === 200) {
                return (await response.json()).tempFiles[0];
            } else {
                throw response;
            }
        }).catch(e => {
            throw e;
        })
    }

    function uploadFile(file, maxSize) {
        if (file instanceof File) {
            return uploadBinaryFile(file, maxSize);
        }
    }

    function setLoadingIndicator() {
        const currentContentlet = document.getElementById('contentletPlaceholder');
        currentContentlet.classList.remove('gu-transit');
        currentContentlet.innerHTML = '<div class="loader__overlay"><div class="loader"></div></div>';
    }

    function isCursorOnUpperSide(cursor, { top, bottom }) {
        return cursor.y - top  <  (bottom - top)/2
    }

    function isContentletPlaceholderInDOM() {
        return !!document.getElementById('contentletPlaceholder');
    }

    function isContainerValid(container) {
        return !container.classList.contains('no');
    }

    function isContainerAndContentletValid(container, contentlet) {
        return isContainerValid(container) && !contentlet.classList.contains('gu-transit');
    }

    function insertBeforeElement(newElem, element) {
        element.parentNode.insertBefore(newElem, element);
    }

    function insertAfterElement(newElem, element) {
        element.parentNode.insertBefore(newElem, element.nextSibling);
    }

    function removeElementById(elemId) {
        document.getElementById(elemId).remove()
    }

    function checkIfContainerAllowsDotAsset(event) {

        const container = event.target.closest('[data-dot-object="container"]');
        
        // Different than 1 file
        if (event.dataTransfer.items.length !== 1 ) {
            return false;
        }

        // File uploaded not an img
        if (!event.dataTransfer.items[0].type.match(/image/g) ) {
            return false;
        }

        // Container does NOT allow img
        if (!container.dataset.dotAcceptTypes.toLocaleLowerCase().match(/dotasset/g)) {
            return false;
        }

        return true;
    }

    function setPlaceholderContentlet() {
        const placeholder = document.createElement('div');
        placeholder.id = 'contentletPlaceholder';
        placeholder.setAttribute('data-dot-object', 'contentlet');
        placeholder.classList.add('gu-transit');
        return placeholder;
    }

    function handleHttpErrors(error) {
        window.contentletEvents.next({
            name: 'handle-http-error',
            data: error
        });
    }

    let currentContainer;

    window.addEventListener("dragenter", dragEnterEvent, false);
    window.addEventListener("dragover", dragOverEvent, false);
    window.addEventListener("dragleave", dragLeaveEvent, false);
    window.addEventListener("drop", dropEvent, false);
    window.addEventListener("beforeunload", removeEvents, false);

    function dragEnterEvent(event) {
        event.preventDefault(); 
        event.stopPropagation();

        const container = event.target.closest('[data-dot-object="container"]');
        currentContainer = container;

        if (container && !checkIfContainerAllowsDotAsset(event)) {
            container.classList.add('no');
        }
    }

    function dragOverEvent(event) {
        event.preventDefault(); 
        event.stopPropagation();

        const container = event.target.closest('[data-dot-object="container"]');
        const contentlet = event.target.closest('[data-dot-object="contentlet"]');

        if (contentlet) {

            if (isContainerAndContentletValid(container, contentlet) && isContentletPlaceholderInDOM()) { 
                removeElementById('contentletPlaceholder');
            }

            const contentletPlaceholder = setPlaceholderContentlet();
            if (isContainerAndContentletValid(container, contentlet)) {
                if (isCursorOnUpperSide(event, contentlet.getBoundingClientRect())) {
                    insertBeforeElement(contentletPlaceholder, contentlet);
                } else {
                    insertAfterElement(contentletPlaceholder, contentlet);                    
                }
            }

        } else if (container && !container.querySelectorAll('[data-dot-object="contentlet"]').length) { // Empty container
            if (isContainerValid(container) && isContentletPlaceholderInDOM()) { 
                removeElementById('contentletPlaceholder');
            }

            container.appendChild(setPlaceholderContentlet()); 
        }
    }

    function dragLeaveEvent(event) {
        event.preventDefault(); 
        event.stopPropagation();

        const container = event.target.closest('[data-dot-object="container"]');

        if (container && currentContainer !== container) {
            container.classList.remove('no');
        }
    }

    function dropEvent(event) {
        event.preventDefault(); 
        event.stopPropagation();

        const container = event.target.closest('[data-dot-object="container"]');

        if (container && !container.classList.contains('no')) {

            setLoadingIndicator();
            uploadFile(event.dataTransfer.files[0]).then((dotCMSTempFile) => {
                dotAssetCreate({
                    file: dotCMSTempFile,
                    url: '/api/v1/workflow/actions/default/fire/PUBLISH',
                    folder: ''
                }).then((response) => {
                    window.contentletEvents.next({
                        name: 'add-uploaded-dotAsset',
                        data: {
                            contentlet: response,
                            placeholderId: 'contentletPlaceholder'
                        }
                    });
                }).catch(e => {
                    handleHttpErrors(e);
                })
            }).catch(e => {
                handleHttpErrors(e);
            })
        }

        if (container) {
            container.classList.remove('no');
        }

    }

    function removeEvents(e) {
        window.removeEventListener("dragenter", dragEnterEvent, false);
        window.removeEventListener("dragover", dragOverEvent, false);
        window.removeEventListener("dragleave", dragLeaveEvent, false);
        window.removeEventListener("drop", dropEvent, false);
        window.removeEventListener("beforeunload", removeEvents, false);
    }

    // D&D Img - End

})();

`;
