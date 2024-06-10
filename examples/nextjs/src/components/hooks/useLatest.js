import { useEffect, useState } from "react";
import { client } from "@/app/utils/dotcmsClient";

const useLatest = (contentType, limit = 3) => {
    const [contentlets, setContentlets] = useState([]);

    useEffect(() => {
        client.content
            .getCollection(contentType)
            .sortBy([
                {
                    field: "modDate",
                    order: "desc",
                },
            ])
            .limit(limit)
            .then((response) => {
                setContentlets(response.contentlets);
            })
            .catch((error) => {
                console.error(`Error fetching ${contentType}`, error);
            });
    }, [contentType, limit]);

    return contentlets;
};

export default useLatest;
