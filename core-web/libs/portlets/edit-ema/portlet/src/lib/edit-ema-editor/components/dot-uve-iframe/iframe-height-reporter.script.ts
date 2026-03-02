/**
 * Temporary iframe height reporter for traditional pages in UVE.
 * Keep isolated so it can be removed once SDK owns this behavior.
 */
export const IFRAME_HEIGHT_REPORTER_SCRIPT = `<script>
(() => {
    const send = () => {
      const height = Math.max(
        document.body?.scrollHeight ?? 0,
        document.documentElement?.scrollHeight ?? 0
      );

      window.parent.postMessage(
        { name: 'dotcms:iframeHeight', payload: { height } },
        '*'
      );
    };

    setTimeout(send, 1000);
})();
</script>`;
