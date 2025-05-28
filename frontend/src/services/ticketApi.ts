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
        }),
    }),
});

export const { useDispatchMutation } = ticketApi;