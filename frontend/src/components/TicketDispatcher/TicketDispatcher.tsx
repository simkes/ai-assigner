import styles from './TicketDispatcher.module.css'
import {useDispatchMutation, useStreamLogsQuery} from "../../services/ticketApi.ts";
import {useEffect, useState} from 'react';
import {motion, AnimatePresence} from "framer-motion";

interface BaseLogMessage<T extends string> {
    type: T;
    toolName: string;
    timestamp: number;
}

interface ToolCallLogMessage extends BaseLogMessage<"CALL"> {
    args: string;
}

interface ToolResultLogMessage extends BaseLogMessage<"OK"> {
    result: string;
}

interface ToolErrorLogMessage extends BaseLogMessage<"ERROR"> {
    error: string;
}

interface UnknownLogMessage extends BaseLogMessage<"UNKNOWN"> {
    message: string;
}

type LogMessage = ToolCallLogMessage | ToolResultLogMessage | ToolErrorLogMessage | UnknownLogMessage;

function renderLogLine(log: LogMessage) {
    const time = new Date(log.timestamp).toLocaleTimeString();

    if (log.type === "CALL") {
        const c = log as ToolCallLogMessage;
        return (
            <>
                <span style={{color: "#0066cc"}}>[{time}] CALL</span>{" "}
                {c.toolName}({c.args})
            </>
        );
    } else if (log.type === "OK") {
        const ok = log as ToolResultLogMessage;

        let content: React.ReactNode;
        try {
            // Unwrap double quotes if needed
            let maybeJson = ok.result.trim();
            if (maybeJson.startsWith('"') && maybeJson.endsWith('"')) {
                maybeJson = JSON.parse(maybeJson);
            }

            const parsed = JSON.parse(maybeJson);
            // Pretty‐print in a plain <pre>
            content = (
                <pre
                    style={{
                        margin: 0,
                        fontSize: "0.85em",
                        fontFamily: "monospace",
                        whiteSpace: "pre-wrap",
                    }}
                >
        {JSON.stringify(parsed, null, 2)}
      </pre>
            );
        } catch {
            // Fallback to raw
            content = (
                <pre
                    style={{
                        margin: 0,
                        fontSize: "0.85em",
                        fontFamily: "monospace",
                        whiteSpace: "pre-wrap",
                    }}
                >
        {ok.result}
      </pre>
            );
        }

        return (
            <div>
                <span style={{color: "#008800"}}>[{time}] OK</span> ← {ok.toolName}:
                <div style={{marginLeft: 16}}>{content}</div>
            </div>
        );

    } else if (log.type === "ERROR") {
        const e = log as ToolErrorLogMessage;
        return (
            <>
                <span style={{color: "#cc0000"}}>[{time}] ERROR</span> ←{" "}
                {e.toolName}: {e.error}
            </>
        );
    } else {
        // covers UNKNOWN and any other unexpected types
        const u = log as UnknownLogMessage;
        return (
            <>
                <span style={{color: "#666666"}}>[{time}] UNKNOWN</span>:{" "}
                {u.message}
            </>
        );
    }
}

export default function AITicketDispatcher() {
    const [text, setText] = useState("");
    const [trigger, {data, isLoading, error}] = useDispatchMutation();
    const {data: logs = []} = useStreamLogsQuery();
    const [latestLog, setLatestLog] = useState<LogMessage | null>(null);
    const isWorking = isLoading && !data && !error;

    useEffect(() => {
        if (logs.length === 0) return;

        const raw = logs[logs.length - 1];
        try {
            setLatestLog(JSON.parse(raw) as LogMessage);
        } catch {
            setLatestLog({
                type: "UNKNOWN",
                toolName: "",
                timestamp: Date.now(),
                message: raw,
            } as UnknownLogMessage);
        }
    }, [logs]);

// ─── 3. when a final answer arrives, wipe the log line ─────────
    useEffect(() => {
        if (data) setLatestLog(null);
    }, [data]);

    // Parse JSON log messages
    // const parsedLogs = logs.map((logString) => {
    //     try {
    //         return JSON.parse(logString) as LogMessage;
    //     } catch {
    //         // Fallback for any non-JSON logs
    //         return { type: "UNKNOWN", toolName: "", timestamp: Date.now(), message: logString };
    //     }
    // });

    return <div className={styles.container}>
        <div className={styles.header}> Ticket Dispatcher</div>
        <textarea
            placeholder={'For example: When a license has AI product code, then we use overuseTill date to provide access with an overuse period. However, if the license has product code like ALL, then we try to use AI allowance inside the license, and in that case we use paidUpTo field in allowance ignoring overuseTill in the license.'}
            rows={6}
            className={styles.multilineInput}
            onChange={(e) => setText(e.target.value)}
        >
        </textarea>
        <button disabled={isLoading} onClick={() => trigger(text)} className={styles.button}>Find best assignees
        </button>

        {latestLog && (<div>
            <h3>Agent Log</h3>
            <div style={{fontFamily: "monospace", minHeight: 24}} className={styles.logs}>
                <AnimatePresence mode="wait">
                    {latestLog && (
                        <motion.div
                            key={latestLog.timestamp /* unique per log */}
                            initial={{opacity: 0, translateY: 4}}
                            animate={{opacity: 1, translateY: 0}}
                            exit={{opacity: 0, translateY: -4}}
                        >
                            {renderLogLine(latestLog)}
                        </motion.div>
                    )}
                </AnimatePresence>
            </div>
        </div>)}

        {isWorking && !latestLog && (
            <div className={styles.logs}>
                <h3>Agent Log</h3>
                <div style={{fontFamily: "monospace", minHeight: 24}}>
                    <div style={{color: "#666"}}>
                        [{new Date().toLocaleTimeString()}] Working...
                    </div>
                </div>
            </div>
        )}

        {error && <pre style={{color: "red"}}>{JSON.stringify(error)}</pre>}


        <div className={`${styles.fadeIn} ${data ? styles.visible : ""}`}>
            <h2 style={{textAlign: 'center', marginTop: '48px'}}>Summary</h2>
            <span>{data?.summary}</span>

            <h3 style={{textAlign: 'center'}}>Assignees</h3>
            <ul className={styles.assigneesList}>
                {data?.assignees.map((a) => (
                    <li key={a.login}>
                        <strong>{a.name}</strong> ({a.login}): {a.reason}
                    </li>
                ))}
            </ul>
        </div>
    </div>
}
