import { configureStore } from "@reduxjs/toolkit";
import { ticketApi } from "./services/ticketApi";

export const store = configureStore({
    reducer: {
        [ticketApi.reducerPath]: ticketApi.reducer,
    },
    middleware: (getDefault) =>
        getDefault().concat(ticketApi.middleware),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;