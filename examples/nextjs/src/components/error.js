import { notFound } from "next/navigation";
import { ErrorLayout } from "@/components/ErrorLayout";

export const ERROR_COPY = {
    403: {
        heading: "Access Denied",
        body: "Sorry, you don’t have permission to view this page.",
    },
    default: {
        heading: "Something went wrong.",
        body: "An unexpected error occurred. Please try again later.",
    },
};

export async function ErrorPage({ error }) {
    if (error.status === 404) {
        notFound();
    }

    const copy = ERROR_COPY[error.status] ?? ERROR_COPY.default;

    return (
        <ErrorLayout
            status={error.status}
            heading={copy.heading}
            body={copy.body}
        />
    );
}
