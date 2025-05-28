import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";

export interface Assignee {
    login: string;
    name: string;
    reason: string;
}

export interface TicketResponse {
    summary: string;
    assignees: Assignee[];
}

export const ticketApi = createApi({
    reducerPath: "ticketApi",
    baseQuery: fetchBaseQuery({
        baseUrl: "http://localhost:8081/api/tickets/",
    }),
    endpoints: (build) => ({
        dispatch: build.mutation<
            TicketResponse,
            string
        >({
            query: (description) => ({
                url: "dispatch",
                method: "POST",
                body: { description },
            }),
            transformResponse: (rawResponse: unknown) => {
                // If it's a string, try parsing it. Otherwise assume it's already the right shape.
                if (typeof rawResponse === "string") {
                    try {
                        return JSON.parse(rawResponse) as TicketResponse;
                    } catch {
                        return {
                            summary: rawResponse,
                            assignees: [],
                        };
                    }
                }
                // rawResponse is already an object
                return rawResponse as TicketResponse;
            },
        }),

        streamLogs: build.query<string[], void>({
            queryFn: () => ({ data: [] as string[] }),
            async onCacheEntryAdded(
                _arg,
                { updateCachedData, cacheDataLoaded, cacheEntryRemoved }
            ) {
                await cacheDataLoaded;
                const es = new EventSource("http://localhost:8081/api/tickets/logs");

                es.onmessage = (e) => {
                    updateCachedData((draft) => {
                        draft.push(e.data);
                    });
                };
                await cacheEntryRemoved;
                es.close();
            },
        }),
    }),
});

export const { useDispatchMutation, useStreamLogsQuery } = ticketApi;