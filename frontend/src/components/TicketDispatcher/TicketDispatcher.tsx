import styles from './TicketDispatcher.module.css'
import {useDispatchMutation} from "../../services/ticketApi.ts";
import { useState } from 'react';

export default function AITicketDispatcher({}) {
    const [text, setText] = useState("");
    const [trigger, { data, isLoading, error }] = useDispatchMutation();

    return <div className={styles.container}>
        <div className={styles.header}> Ticket Dispatcher </div>
        <textarea
            placeholder={'For example: When a license has AI product code, then we use overuseTill date to provide access with an overuse period. However, if the license has product code like ALL, then we try to use AI allowance inside the license, and in that case we use paidUpTo field in allowance ignoring overuseTill in the license.'}
            rows={6}
            className={styles.multilineInput}
            onChange={(e) => setText(e.target.value)}
        >
        </textarea>
        <button disabled={isLoading} onClick={() => trigger(text)} className={styles.button}>Find best assignees</button>
        {error && <pre style={{ color: "red" }}>{JSON.stringify(error)}</pre>}

        {data && (
            <>
                <h2 style={{textAlign: 'center', marginTop: '48px'}}>Summary</h2>
                <span>{data.summary}</span>

                <h3 style={{textAlign: 'center'}}>Assignees</h3>
                <ul className={styles.assigneesList}>
                    {data.assignees.map((a) => (
                        <li key={a.login}>
                            <strong>{a.name}</strong> ({a.login}): {a.reason}
                        </li>
                    ))}
                </ul>
            </>
        )}
    </div>
}
