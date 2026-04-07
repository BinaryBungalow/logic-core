# Plan: pi-coding-agent Functional Parity with Logicbean-TUI

## Goal
Achieve 100% functional parity with the original badlogic/pi-coding-agent using logicbean-tui as the terminal UI implementation layer.

## Current Status
✅ **Core Subsystems Complete**:
- ✅ binbun-agent-core: Agent runtime think→tool→respond loop
- ✅ binbun-model: Unified LLM provider abstraction
- ✅ binbun-gateway: Transport and session management
- ✅ binbun-cli: CLI entry point
- ✅ binbun-memory: Session persistence layer

⚠️ **Missing parity components**:
- ❌ Terminal UI implementation
- ❌ Coding agent interactive mode
- ❌ Built-in file system and bash tools
- ❌ Session branching, forking and compaction
- ❌ Token and cost accounting
- ❌ Extension and plugin system

## Technical Approach
Use **logicbean-tui** directly as the terminal UI rendering layer. This library already provides all required TUI capabilities:
- ✅ Full Flexbox layout engine
- ✅ Retained-mode component rendering with diffing
- ✅ All required base UI components
- ✅ Keyboard and mouse input handling
- ✅ ANSI 24-bit color and theme system
- ✅ Markdown renderer for agent responses
- ✅ Java 25 compatible with virtual thread support

## Implementation Phases

### Phase 1: TUI Integration (Est: 1-2 days)
1.  Add logicbean-tui gradle dependency to binbun-cli module
2.  Implement TUI application bootstrap and lifecycle management
3.  Create base agent interface layout using logicbean-tui components
4.  Implement event bridge between agent runtime events and UI
5.  Implement keyboard shortcut mapping matching pi's keybindings

### Phase 2: Coding Agent Runtime (Est: 3-4 days)
1.  Implement full agent turn execution loop
2.  Implement tool execution orchestration pipeline
3.  Add built-in tools: `read`, `write`, `edit`, `bash`, `grep`, `find`, `ls`
4.  Implement context window management and automatic compaction
5.  Add token usage tracking and cost accounting

### Phase 3: Session Management (Est: 2-3 days)
1.  Implement JSONL session persistence format
2.  Add session branching and forking capabilities
3.  Implement session tree view and navigation
4.  Add session export / import functionality
5.  Implement automatic session checkpointing

### Phase 4: Extension System (Est: 3-5 days)
1.  Implement plugin classpath discovery and loading
2.  Add command registration system
3.  Implement UI extension hooks
4.  Add plugin package manager with git repository support
5.  Implement event bus for plugin integration

### Phase 5: RPC & Compatibility (Est: 2 days)
1.  Implement JSONL RPC protocol over stdin/stdout
2.  Add headless SDK embedding mode
3.  Implement all pi compatible CLI arguments
4.  Add session file compatibility mode

## Verification Steps
1.  Full build completes without errors
2.  TUI starts correctly and renders layout
3.  All built-in tools execute correctly
4.  Sessions persist across restarts
5.  Session branching and forking works correctly
6.  Keyboard shortcuts match pi behaviour
7.  RPC protocol responds correctly to standard pi commands

## Completion Criteria
✅ logicbean-tui integrated and rendering agent interface
✅ All pi built-in tools implemented and working
✅ Session persistence with branching/forking
✅ Token and cost accounting working
✅ Extension/plugin system functional
✅ 100% feature parity with pi-coding-agent
✅ All existing tests pass
✅ No regressions in core gateway functionality

## Dependencies
- Java 25 (already in use)
- JLine 3 (transitive from logicbean-tui)
- Jackson for JSON serialization
- Reactive Streams for async event handling
