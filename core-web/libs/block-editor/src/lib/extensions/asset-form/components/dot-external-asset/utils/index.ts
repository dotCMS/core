/**
 * Handle the error of the video
 *
 * @param {*} e
 * @return {*}  {string}
 */
export const handleLoadVideoError = (e): string => {
    switch (e.target.error.code) {
        case e.target.error.MEDIA_ERR_ABORTED:
            return 'You aborted the video playback.';

        case e.target.error.MEDIA_ERR_NETWORK:
            return 'A network error caused the video download to fail part-way.';

        case e.target.error.MEDIA_ERR_DECODE:
            return 'Video playback aborted due to browser compatibility issues. Try a different browser or visit <a href="https://developer.mozilla.org/en-US/docs/Web/Media/Formats#media_file_types_and_codecs">MDN Video Support</a> for more information.';

        case e.target.error.MEDIA_ERR_SRC_NOT_SUPPORTED:
            return 'Invalid URL. Please provide a URL to a Youtube Video, .mp4, .webm, or .ogv video file. For more info, visit:  <a href="https://developer.mozilla.org/en-US/docs/Web/Media/Formats/Containers#common_container_formats" target="_blank">MDN Video Format Support</a>';

        default:
            return 'An unknown error occurred. <a href="https://www.dotcms.com/services/support/" target="_blank">Please contact support</a>';
    }
};
