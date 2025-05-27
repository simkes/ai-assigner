import ReactDOM from 'react-dom/client';
import './index.css';

import { StrictMode } from 'react';
import App from './App';
import {store} from "./store.ts";
import { Provider } from 'react-redux';

ReactDOM.createRoot(document.getElementById('root')!).render(
    <Provider store={store}>
        <StrictMode>
            <App />
        </StrictMode>
    </Provider>
);
