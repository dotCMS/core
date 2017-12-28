export const GOOGLE_FONTS = 'https://fonts.googleapis.com/css?family=Roboto:400,700';
export const EDIT_PAGE_JS = `
(function () {
    var containers = Array.from(document.querySelectorAll('div[data-dot-object="container"]'));
    var forbiddenTarget;
    var drake = dragula(
        containers, {
        accepts: function (el, target, source, sibling) {
            var canDrop = target.dataset.dotAcceptTypes.indexOf(el.dataset.dotType) > -1;

            if (target.dataset.dotMaxLimit) {
                var containerMaxLimit = parseInt(target.dataset.dotMaxLimit, 10);
                var containerChildrenQuantity = target.children.length
                canDrop = containerChildrenQuantity < containerMaxLimit;
            }

           if (!canDrop && target !== source) {
                forbiddenTarget = target;
                forbiddenTarget.classList.add('no')
            }

            return canDrop;
        },
        invalid: function(el, handle) {
            return !handle.classList.contains('dotedit-contentlet__drag');
        }
    });
    drake.on('dragend', function(el) {
        if (forbiddenTarget && forbiddenTarget.classList.contains('no')) {
            forbiddenTarget.classList.remove('no');
        }
    });
    drake.on('drop', function(el, target, source, sibling) {
        if (target !== source) {
            window.contentletEvents.next({
                event: 'relocate',
                data: {
                    container: {
                        identifier: target.dataset.dotIdentifier,
                        uuid: 'fake-one'
                    },
                    contentlet: {
                        identifier: el.dataset.dotIdentifier,
                        inode: el.dataset.dotInode
                    }
                }
            });
        }
    })
})();
`;
