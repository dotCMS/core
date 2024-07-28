import { client } from "@/utils/dotcmsClient";
import { postMessageToEditor } from "@dotcms/client";
import { useEffect, useState } from "react";

export const usePageGQLData = (currentPageData) => {
    const [pageData, setPageData] = useState(null);

    useEffect(() => {
        client.editor.on("gql-changes", (page) => {
            if (!page) {
                return;
            }
            setPageData({ ...page });
        });
        return () => {
            client.editor.off("changes");
        };
    }, [currentPageData]);

    return pageData ?? currentPageData;
};
