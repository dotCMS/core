import { notFound } from "next/navigation";
import { ErrorLayout } from "@/components/ErrorLayout";

export const ERROR_COPY = {
    403: {
        heading: "Access Denied",
        body: "Sorry, you don’t have permission to view this page.",
    },
    404: {
        heading: "Something's missing.",
        body: "Sorry, we can't find that page. You'll find lots to explore on the home page.",
    },
    default: {
        heading: "Something went wrong.",
        body: "An unexpected error occurred. Please try again later.",
    },
};

interface ErrorPageProps {
    error?: { status?: number };
}

export async function ErrorPage({ error }: ErrorPageProps) {
    const status = error?.status ?? 500;

    if (status === 404) {
        notFound();
    }

    const copy =
        ERROR_COPY[status as keyof typeof ERROR_COPY] ?? ERROR_COPY.default;

    return (
        <ErrorLayout
            status={status}
            heading={copy.heading}
            body={copy.body}
        />
    );
}
