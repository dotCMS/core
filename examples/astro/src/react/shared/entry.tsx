import type { DotCMSContentlet } from "@dotcms/types";
import useImageSrc from "@react/hooks/useImageSrc";
import type { FC } from "react";

const dateFormatOptions: Intl.DateTimeFormatOptions = {
  year: "numeric",
  month: "long",
  day: "numeric",
};

export type EntryProps = {
  contentlet: DotCMSContentlet;
};

export const Entry: FC<EntryProps> = ({ contentlet }) => {
  const src = useImageSrc({
    src: contentlet.image?.idPath ?? contentlet.image,
  });

  return (
    <>
      <a
        className="relative min-w-32"
        href={contentlet.urlMap || contentlet.url}
      >
        {contentlet.image && (
          <img
            src={src}
            alt={contentlet.urlTitle}
            className="object-cover absolute w-full h-full top-0 left-0"
          />
        )}
      </a>
      <div className="flex flex-col gap-1">
        <a
          className="text-sm text-zinc-900 font-bold"
          href={contentlet.urlMap || contentlet.url}
        >
          {contentlet.title}
        </a>
        <time className="text-zinc-600">
          {new Date(contentlet.modDate).toLocaleDateString(
            "en-US",
            dateFormatOptions,
          )}
        </time>
      </div>
    </>
  );
};
