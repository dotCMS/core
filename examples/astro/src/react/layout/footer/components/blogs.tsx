import { Contentlets } from "@react/shared/contentlets";
import type { DotCMSContentlet } from "@dotcms/types";
import { client } from "@utils/client";
import { useEffect, useState } from "react";

export const Blogs = () => {
  const [blogs, setBlogs] = useState<DotCMSContentlet[]>([]);

  useEffect(() => {
    client.content
      .getCollection("Blog")
      .sortBy([
        {
          field: "modDate",
          order: "desc",
        },
      ])
      .limit(3)
      .then((response) => {
        setBlogs(response.contentlets);
      })
      .catch((error) => {
        console.error(`Error fetching Blogs`, error);
      });
  }, []);

  return (
    <div className="flex flex-col">
      <h2 className="text-2xl font-bold mb-7 text-black">Latest Blog Posts</h2>
      {!!blogs.length && <Contentlets contentlets={blogs} />}
    </div>
  );
};
