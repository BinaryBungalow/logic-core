# Logic Core

Core repository for 'logic-core'.

## Quick Start

### Prerequisites

- **Java 25** (required — the project uses Java 25 language features and virtual threads)
- **Gradle** (wrapper included — no need to install separately)

### Build

```bash
./gradlew build
```

### Run the Gateway

```bash
# Set required environment variables
export BINBUN_PROVIDER=openai          # openai, anthropic, or google
export BINBUN_API_KEY=your-api-key     # your LLM provider API key
export BINBUN_MODEL=gpt-4o             # optional, defaults to provider default
export BINBUN_BASE_URL=https://api.openai.com  # optional, defaults to provider default

# Run the gateway
./gradlew :binbun-cli:run --args="gateway"
```

### Available CLI Commands

```bash
# Start the gateway daemon
./gradlew :binbun-cli:run --args="gateway"

# The gateway starts:
# - WebSocket server on port 8789 (method dispatch + event subscriptions)
# - ACP HTTP transport on port 8787 (health, protocol, events)
# - ACP socket transport on port 8788 (session attach/prompt/close)
```

### Connecting to the Gateway

Once running, the gateway exposes:

| Endpoint | Protocol | Description |
|----------|----------|-------------|
| `tcp://127.0.0.1:8789` | WebSocket (JSON lines) | Method dispatch + event subscriptions |
| `http://127.0.0.1:8787/health` | HTTP GET | Gateway health report |
| `http://127.0.0.1:8787/gateway/health` | HTTP GET | Detailed health with subsystems |
| `http://127.0.0.1:8787/gateway/readiness` | HTTP GET | Readiness check |
| `http://127.0.0.1:8787/acp/protocol` | HTTP GET | ACP protocol info |
| `tcp://127.0.0.1:8788` | Socket (JSON lines) | ACP session transport |

### WebSocket Method Protocol

Connect to `tcp://127.0.0.1:8789` and send JSON frames:

**Request:**
```json
{"type": "req", "id": "1", "method": "health", "params": {}}
```

**Response:**
```json
{"type": "res", "id": "1", "ok": true, "payload": {"liveness": "UP", "readiness": "UP"}}
```

**Subscribe to events:**
```json
{"type": "sub", "id": "2", "event": "session.registered"}
```

**Available methods:**
- `health` — Gateway health report (no auth required)
- `status` — Gateway runtime status (requires `operator.read`)
- `sessions.list` — List all sessions (requires `operator.read`)
- `channels.status` — Channel connector status (requires `operator.read`)
- `plugins.list` — List installed plugins (requires `operator.read`)
- `recovery.plan` — Recovery checkpoint plan (requires `operator.admin`)

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `BINBUN_PROVIDER` | No | `openai` | LLM provider: `openai`, `anthropic`, `google` |
| `BINBUN_API_KEY` | Yes | — | LLM provider API key |
| `BINBUN_MODEL` | No | provider default | Model name to use |
| `BINBUN_BASE_URL` | No | provider default | Custom API base URL |
| `BINBUN_MESSAGE_DISPATCH` | No | `file` | Dispatch mode: `file`, `console`, `webhook` |
| `BINBUN_MESSAGE_WEBHOOK_URL` | Conditional | — | Required when `BINBUN_MESSAGE_DISPATCH=webhook` |
| `BINBUN_ACP_TOKEN` | No | `dev-token` | ACP authentication token |
| `BINBUN_TELEGRAM_BOT_TOKEN` | No | — | Telegram bot token (enables Telegram connector) |
| `BINBUN_TELEGRAM_WEBHOOK_URL` | No | — | Telegram webhook URL |
| `BINBUN_SLACK_BOT_TOKEN` | No | — | Slack bot token (enables Slack connector) |
| `BINBUN_WEBHOOK_ENDPOINT` | No | `http://127.0.0.1:9999/inbox` | Webhook endpoint URL |

## Included modules

### Core
- `binbun-bom` — Bill of materials for dependency versions
- `binbun-model` — LLM provider abstraction and model clients
- `binbun-agent-core` — Agent runtime with think→tool→respond loop
- `binbun-tools` — Tool registry and execution
- `binbun-memory` — Session persistence and checkpointing
- `binbun-resources` — Resource management
- `binbun-cli` — CLI entry point with gateway command
- `binbun-deploy` — Deployment provider abstraction

### Phase 2 modules
- `binbun-gateway` — Gateway daemon with WebSocket server, method registry, session management, event bus
- `binbun-core-plugin` — Baked-in personal-assistant plugin
- `binbun-tooling-native` — Native tools: cron, scheduling, gateway inspection, messaging
- `binbun-workflows` — Resumable workflow engine with persistence
- `binbun-acp` — ACP protocol model and session management
- `binbun-acp-auth` — Authentication: static tokens, session tokens, device identity, challenge-response
- `binbun-acp-protocol` — ACP envelope model, codec, operations
- `binbun-acp-transport-socket` — Full-duplex socket transport with replay buffers
- `binbun-acp-transport-http` — HTTP transport with SSE event streaming
- `binbun-delivery-core` — Delivery connector SPI, retry, failure handling
- `binbun-delivery-model` — Shared delivery models (jobs, status, repositories)
- `binbun-delivery-store` — JSON-based delivery job persistence
- `binbun-delivery-webhook` — Webhook channel connector
- `binbun-delivery-telegram` — Telegram channel connector (Bot API)
- `binbun-delivery-slack` — Slack channel connector (Web API)
- `binbun-gateway-health` — Health probes and dynamic health reporting
- `binbun-gateway-recovery` — Recovery executors for sessions, workflows, delivery, plugins
- `binbun-gateway-observability` — Correlation context, structured logging, metrics
- `binbun-plugin-manifest` — Plugin manifest model, validation, loading
- `binbun-plugin-resolver` — Semantic version resolution with conflict detection
- `binbun-plugin-runtime` — Plugin lifecycle management with hooks
- `binbun-plugin-registry` — Plugin discovery (bundled + filesystem)
- `binbun-browser-core` — Browser automation abstraction
- `binbun-browser-playwright` — Playwright-based browser automation
- `binbun-integration-tests` — Integration test suite

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Gateway Daemon                    │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────┐ │
│  │  WebSocket  │  │ ACP Socket   │  │ ACP HTTP   │ │
│  │  :8789      │  │ :8788        │  │ :8787      │ │
│  └──────┬──────┘  └──────┬───────┘  └─────┬──────┘ │
│         │                │                 │        │
│  ┌──────▼────────────────▼─────────────────▼──────┐ │
│  │              Method Registry                    │ │
│  │  health, status, sessions.*, channels.*, ...   │ │
│  └──────────────────────┬─────────────────────────┘ │
│                         │                           │
│  ┌──────────────────────▼─────────────────────────┐ │
│  │              Gateway Runtime                    │ │
│  │  Session Registry │ Event Bus │ Workflow Engine │ │
│  └──────────────────────┬─────────────────────────┘ │
│                         │                           │
│  ┌──────────────────────▼─────────────────────────┐ │
│  │              Agent Core                         │ │
│  │  Think → Tool → Respond Loop │ Compaction      │ │
│  └──────────────────────┬─────────────────────────┘ │
│                         │                           │
│  ┌──────────┐  ┌────────▼────────┐  ┌────────────┐ │
│  │ Delivery │  │ Connector Reg.  │  │  Plugins   │ │
│  │  Retry   │  │ Telegram/Slack  │  │  Runtime   │ │
│  └──────────┘  └─────────────────┘  └────────────┘ │
└─────────────────────────────────────────────────────┘
```

## Development

### Run tests

```bash
./gradlew test
```

### Compile only

```bash
./gradlew compileJava compileTestJava
```

### Clean build

```bash
./gradlew clean build
```

