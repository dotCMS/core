import { useDotcmsPageContext } from "@dotcms/react";

const ImageLoader = ({ src }) => {
    const dotcmsURL = new URL(process.env.NEXT_PUBLIC_DOTCMS_HOST);

    const context = useDotcmsPageContext();

    const languageId = context?.pageAsset.viewAs.language.id ?? 1;

    return src.includes("/dA/") // Check if the image is a DotCMS asset or a file asset
        ? `${dotcmsURL.origin}${src}?language_id=${languageId}`
        : `${dotcmsURL.origin}/dA/${src}?language_id=${languageId}`;
};

export default ImageLoader;
