import { useDotcmsPageContext } from "@dotcms/react";

const useImageSrc = ({ src, width }: { src: string; width?: number }) => {
  const dotcmsURL = new URL(import.meta.env.PUBLIC_DOTCMS_HOST).origin;

  const context = useDotcmsPageContext();

  const languageId = context?.pageAsset.viewAs.language.id ?? 1;

  const imageSRC = src.includes("/dA/") ? src : `/dA/${src}`; // Check if the image is a DotCMS asset or a file asset

  const withWidth = width ? `/${width}` : "";

  return `${dotcmsURL}${imageSRC}${withWidth}?language_id=${languageId}`;
};

export default useImageSrc;
