package ai.binbun.acp.protocol;

public enum AcpOperation {
    HELLO,
    AUTH,
    READY,
    ATTACH,
    PROMPT,
    CLOSE,
    ACK,
    HEARTBEAT,
    EVENT,
    ERROR
}
