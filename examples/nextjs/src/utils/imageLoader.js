const imageLoaderByConfig = ({ language }) => {
    const dotcmsURL = new URL(process.env.NEXT_PUBLIC_DOTCMS_HOST);

    return ({ src }) =>
        src.includes("/dA/") // Check if the image is a DotCMS asset or a file asset
            ? `${dotcmsURL.origin}${src}?language_id=${language}`
            : `${dotcmsURL.origin}/dA/${src}?language_id=${language}`;
};

export default imageLoaderByConfig;
