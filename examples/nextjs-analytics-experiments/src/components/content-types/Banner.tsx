"use client";

import Image from "next/image";
import Link from "next/link";

interface BannerProps {
  title?: string;
  caption?: string;
  image?: { identifier?: string };
  link?: string;
  buttonText?: string;
}

export default function Banner({
  title,
  caption,
  image,
  link,
  buttonText,
}: BannerProps) {
  return (
    <div className="relative h-64 w-full overflow-hidden bg-gray-200 p-4">
      {image?.identifier && (
        <Image
          src={image.identifier}
          fill
          className="object-cover"
          alt={title ?? "Banner"}
        />
      )}
      <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/40 p-4 text-center text-white">
        {title && <h2 className="mb-2 text-3xl font-bold">{title}</h2>}
        {caption && <p className="mb-4 text-lg">{caption}</p>}
        {link && (
          <Link
            className="rounded bg-blue-600 px-4 py-2 font-semibold hover:bg-blue-700"
            href={link}
          >
            {buttonText ?? "Learn more"}
          </Link>
        )}
      </div>
    </div>
  );
}
