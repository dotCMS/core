import type { DotCMSBasicContentlet } from "@dotcms/types";

interface RecommendedCardProps extends DotCMSBasicContentlet {
  urlMap: string;
}

const RecommendedCard = ({
  contentlet,
}: {
  contentlet: RecommendedCardProps;
}) => {
  const { url, title, inode, image, urlMap, modDate } = contentlet;
  const dateFormatOptions = {
    year: "numeric",
    month: "long",
    day: "numeric",
  };

  return (
    <div className="flex gap-7 min-h-16 relative">
      {/* <EditButton contentlet={contentlet} /> */}
      <a className="relative min-w-32" href={urlMap || url}>
        {image && (
          <img src={`/dA/${inode}`} alt={title} className="object-cover" />
        )}
      </a>
      <div className="flex flex-col gap-1">
        <a className="text-sm text-white font-bold" href={urlMap || url}>
          {title}
        </a>
        <time className="text-gray-400">
          {new Date(modDate).toLocaleDateString(
            "en-US",
            dateFormatOptions as Intl.DateTimeFormatOptions,
          )}
        </time>
      </div>
    </div>
  );
};

export default RecommendedCard;
