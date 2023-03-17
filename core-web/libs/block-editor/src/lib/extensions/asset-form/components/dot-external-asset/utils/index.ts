/**
 * Handle the error of the video
 *
 * @private
 * @param {*} e
 * @return {*}  {string}
 * @memberof DotExternalAssetComponent
 */
export const handleError = (e): string => {
    switch (e.target.error.code) {
        case e.target.error.MEDIA_ERR_ABORTED:
            return 'You aborted the video playback.';

        case e.target.error.MEDIA_ERR_NETWORK:
            return 'A network error caused the video download to fail part-way.';

        case e.target.error.MEDIA_ERR_DECODE:
            return 'The video playback was aborted due to a corruption problem or because the video used features your browser did not support. For more info, visit:  <a href="https://developer.mozilla.org/en-US/docs/Web/Media/Formats#media_file_types_and_codecs">MDN Video Support</a>';

        case e.target.error.MEDIA_ERR_SRC_NOT_SUPPORTED:
            return 'Invalid URL. Please provide a URL to a .mp4, .webm, or .ogv video file. For more info, visit:  <a href="https://developer.mozilla.org/en-US/docs/Web/Media/Formats/Containers#common_container_formats" target="_blank">MDN Video Format Support</a>';

        default:
            return 'An unknown error occurred.';
    }
};
