import { dotcmsClient } from "@dotcms/client";
import { MyPage } from "@/components/my-page";
import { graphqlToPageEntity } from "../utils/gql";

const client = dotcmsClient.init({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    authToken: process.env.DOTCMS_AUTH_TOKEN,
    siteId: "59bb8831-6706-4589-9ca0-ff74016e02b2",
    requestOptions: {
        // In production you might want to deal with this differently
        cache: "no-cache",
    },
});

// Define your GraphQL query
const query = `{
    page(url: "/index") {
        title
        url
        seodescription
        containers {
            path
            identifier
            containerStructures {
                id
                structureId
                containerInode
                containerId
                code
            }
            containerContentlets {
                uuid
                contentlets {
                    identifier
                    inode
                    title
                    contentType
                    image,
                    caption,
                    buttonText,
                    link
                }
            }
        }
        host {
            hostName
        }
        layout {
            header
            footer
            body {
                rows {
                    columns {
                        containers {
                            identifier
                            uuid
                        }
                    }
                }
            }
        }
        template {
            iDate
            inode
            identifier
            source
            title
            friendlyName
            modDate
            sortOrder
            showOnMenu
            image
            drawed
            drawedBody
        }
        viewAs {
            mode
        }
    }
}
  `;

export default async function Home({ searchParams, params }) {
    const requestData = {
        path: params?.slug ? params.slug.join("/") : "index",
        language_id: searchParams.language_id,
        "com.dotmarketing.persona.id":
            searchParams["com.dotmarketing.persona.id"] || "",
        mode: searchParams.mode,
        variantName: searchParams["variantName"],
    };
    const nav = await client.nav.get({
        path: "/",
        depth: 2,
        languageId: searchParams.language_id,
    });

    const { data } = await fetch("http://localhost:8080/api/v1/graphql", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ query }),
    }).then((response) => response.json());

    const entity = graphqlToPageEntity(data);

    return <MyPage nav={nav.entity.children} data={entity}></MyPage>;
}
