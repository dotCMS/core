export const EDIT_PAGE_JS = `
(function () {
    var forbiddenTarget;
    let currentModel;
    var executeScroll = 1;

function initDragAndDrop () {
    function getContainers() {
        var containers = [];
        var containersNodeList = document.querySelectorAll('[data-dot-object="container"]');

        for (var i = 0; i < containersNodeList.length; i++) {
            containers.push(containersNodeList[i]);
        };

        return containers;
    }

    function getDotNgModel(data = {}) {
        const { identifier, uuid, addedContentId } = data;
        const model = [];
        getContainers().forEach(function(container) {
            const contentlets = Array.from(container.querySelectorAll('[data-dot-object="contentlet"]'));
            const placeholder = container.querySelector('#contentletPlaceholder');
            const contentletsId = contentlets
                .map((contentlet) => {
                    // Replace current PlaceHolder position with the new contentlet Id that will be added
                    if(contentlet === placeholder) {
                        return addedContentId;
                    }

                    return contentlet.dataset.dotIdentifier;
                });

            const { dotIdentifier, dotUuid } = container.dataset;
            const isSameUuid = uuid === dotUuid;
            const isSameIdentifier = identifier === dotIdentifier;

            // If the placeholder is not present and the containers matched
            // add the new contentlet Id at the end of the array
            if (isSameUuid && isSameIdentifier && !placeholder) {
                contentletsId.push(addedContentId);
            }

            // Filter the array to remove the undefined values
            const filteredContentletsId = contentletsId.filter((value) => !!value);

            model.push({
                identifier: dotIdentifier,
                uuid: dotUuid,
                contentletsId: filteredContentletsId
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
            window.contentletEvents.next({
                name: 'reorder',
                data: updatedModel
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

    function checkIfContainerAllowsDotAsset(event, container) {

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

        // Container reached max contentlet's limit
        if (container.querySelectorAll('div:not(.gu-transit)[data-dot-object="contentlet"]').length === parseInt(container.dataset.maxContentlets, 10)) {
            return false;
        }

        return true;
    }

    function checkIfContainerAllowContentType(container) {
        if (container.querySelectorAll('div:not(.gu-transit)[data-dot-object="contentlet"]').length === parseInt(container.dataset.maxContentlets, 10)) {
            return false;
        }

        const dotAcceptTypes = container.dataset.dotAcceptTypes.toLocaleLowerCase();

        if (!isDraggedContentSet()) {
            return false;
        }
 
        const variable = draggedContent.variable?.toLocaleLowerCase();
        const contentType = draggedContent.contentType?.toLocaleLowerCase();
        const baseType = draggedContent.baseType?.toLocaleLowerCase();

        const isWidget = baseType === 'widget';

        const dotAssetIncludesContent = dotAcceptTypes.includes(variable || contentType || baseType);

        return isWidget || dotAssetIncludesContent;
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
    window.addEventListener("mousemove", clearScroll, false );

    function clearScroll() {
        executeScroll = 0;
    }

    function dragEnterEvent(event) {
        event.preventDefault();
        event.stopPropagation();
        const container = event.target.closest('[data-dot-object="container"]');
        currentContainer = container;
        if (container && !(checkIfContainerAllowsDotAsset(event, container) || checkIfContainerAllowContentType(container))) {
            container.classList.add('no');
        }
    }

    function dotWindowScroll(step){
        if (!!executeScroll ) {
            window.scrollBy({
                top: step,
                behaviour: 'smooth'
            });
        } else {
            clearInterval(scrollInterval);
        }
    }

    var scrollInterval;
    function dotCustomScroll (step) {
        if (executeScroll === 0) {
            executeScroll = step;
            scrollInterval = setInterval( ()=> {dotWindowScroll(step)}, 1);
        }
    }

    function dragOverEvent(event) {
        event.preventDefault();
        event.stopPropagation();
        const container = event.target.closest('[data-dot-object="container"]');
        const contentlet = event.target.closest('[data-dot-object="contentlet"]');

        if (event.clientY < 150) {
            dotCustomScroll(-5)
        } else if (event.clientY > (document.body.clientHeight - 150)) {
            dotCustomScroll(5)
        } else {
            clearScroll();
        }

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
        } else if (
                container &&
                !container.querySelectorAll('[data-dot-object="contentlet"]').length &&
                isContainerValid(container)
            ) { // Empty container

            if (isContentletPlaceholderInDOM()) {
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

        if (isContentletPlaceholderInDOM()){
            removeElementById('contentletPlaceholder');
        }
    }

    function sendCreateContentletEvent(contentlet) {
        window.contentletEvents.next({
            name: 'add-contentlet',
            data: {
                contentlet
            }
        });
    }

    function sendCreateFormEvent(formId) {
        window.contentletEvents.next({
            name: 'add-form',
            data: formId
        });
    }

    function dropEvent(event) {
        event.preventDefault();
        event.stopPropagation();
        const container = event.target.closest('[data-dot-object="container"]');
        const files = event.dataTransfer?.files;

        if (container && !container.classList.contains('no') && isContentletPlaceholderInDOM()) {
        
            if (files?.length) { // trying to upload an image
                setLoadingIndicator();
                loadImageToDotcms(files[0]);
            } 
            
            if(isDraggedContentSet()) {
                // Adding specific Content Type / Contentlet
                if (draggedContent?.contentType) { // Contentlet
                    if (draggedContent.contentType === 'FORM') {
                        sendCreateFormEvent(draggedContent.id)
                    } else {
                        sendCreateContentletEvent(draggedContent);
                    }
                } else { // Content Type
                    window.contentletEvents.next({
                        name: 'add-content',
                        data: {
                            container: container.dataset,
                            contentType: draggedContent
                        }
                    });
                }
            }
        }

        if (container) {
            setTimeout(()=> {
                container.classList.remove('no');
            }, 0);
        }

        // We need to clean the dragged content after the drop event
        // That way, if the user tries to drag & drop a invalid content
        // It won't use an old dragged content reference
        cleanDraggedContent();
    }

    function removeEvents(e) {
        window.removeEventListener("dragenter", dragEnterEvent, false);
        window.removeEventListener("dragover", dragOverEvent, false);
        window.removeEventListener("dragleave", dragLeaveEvent, false);
        window.removeEventListener("drop", dropEvent, false);
        window.removeEventListener("beforeunload", removeEvents, false);
        window.removeEventListener("mousemove", clearScroll, false );
    }

    function disableDraggableHtmlElements() {
        var containerAnchorsList = document.querySelectorAll('[data-dot-object="container"] a, [data-dot-object="container"] a img');
        for (var i = 0; i < containerAnchorsList.length; i++) {
            containerAnchorsList[i].setAttribute("draggable", "false")
        };
    }

    /**
     * @description
     * Uploads an image to dotCMS and returns a promise with the temp file
     * 
     * @param {File} file
     * @returns {Promise}
     */
    function loadImageToDotcms(file) {
        uploadFile(file).then((dotCMSTempFile) => {
            dotAssetCreate({
                file: dotCMSTempFile,
                url: '/api/v1/workflow/actions/default/fire/PUBLISH',
                folder: ''
            }).then((response) => {
                window.contentletEvents.next({
                    name: 'add-uploaded-dotAsset',
                    data: {
                        contentlet: response
                    }
                });
            }).catch(e => {
                handleHttpErrors(e);
            })
        }).catch(e => {
            handleHttpErrors(e);
        })
    }

    /**
     * @description
     * Check if draggedContent is set
     * 
     * @returns {Boolean}
     */
    function isDraggedContentSet() {
        // draggedContent is set by dotContentletEditorService.draggedContentType$ [dot-edit-content.component.ts#L585-L593]
        return window.hasOwnProperty('draggedContent');
    }

    /**
     * @description
     * Clean draggedContent from window object
     * 
     * @returns {void}
     */
    function cleanDraggedContent() {
        isDraggedContentSet() && delete window.draggedContent;
    }

    disableDraggableHtmlElements();

    // D&D Img - End
}

    /*
        This setInterval is required because this script is running
        before the web component finishes rendering. Currently,
        we do not have a way to listen to the web component event,
        so this setInterval is used.
    */

    let attempts = 0;
    const initScript = setInterval(function() {
        const isContainer = document.querySelector('[data-dot-object="container"]');
        attempts++;
        if(isContainer) {
            clearInterval(initScript);
            initDragAndDrop();
        } else if( attempts === 10) {
            clearInterval(initScript);
        }
    }, 500);

})();

`;
