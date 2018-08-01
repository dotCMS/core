<script type="text/javascript">
    var customEvent = window.top.document.createEvent('CustomEvent');
    customEvent.initCustomEvent('ng-event', false, false,  {
                name: 'reload-edit-mode-page'
    });

    window.top.document.dispatchEvent(customEvent);
</script>
