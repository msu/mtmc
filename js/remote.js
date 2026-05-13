/**
 * Remote mode for MTMC-16 emulator.
 * When window.REMOTE_MODE is set (by the IntelliJ plugin's web server),
 * this module connects via SSE to receive emulator state from the Java
 * debug session and renders it using the existing UI functions.
 */

let eventSource = null

export function initRemote(updateUI, consolePrint, display, memory, cpu) {
  if (!window.REMOTE_MODE) return false

  // Hide the file explorer/editor column
  const fsPanel = document.getElementById('fs')
  if (fsPanel) fsPanel.style.display = 'none'

  // Start in 1-col stacked mode, hide file explorer
  const main = document.querySelector('main')
  if (main) main.classList.add('one-col')

  // Hide control buttons/dropdowns but keep the header
  const controlButtons = document.getElementById('control-buttons')
  if (controlButtons) controlButtons.style.display = 'none'
  const controlSecondary = document.getElementById('control-secondary')
  if (controlSecondary) controlSecondary.style.display = 'none'

  // Add remote-mode CSS overrides
  const style = document.createElement('style')
  style.textContent = `
    body {
      min-height: 768px;
      overflow: hidden;
      margin: 0;
    }

    /* Two-column mode */
    main {
      display: grid !important;
      grid-template-columns: 470px 1fr !important;
      grid-gap: 0 !important;
      height: 100vh;
      overflow: hidden;
    }
    #left {
      display: flex !important;
      flex-direction: column;
      min-width: 0; max-width: none;
      height: 100vh;
      overflow: hidden;
    }
    #center {
      display: flex !important;
      flex-direction: column;
      min-width: 0;
      height: 100vh;
      overflow: hidden;
    }

    /* Registers: fixed natural height */
    #register-panel { flex: 0 0 auto; overflow: hidden; }
    #register-table { width: 100%; table-layout: fixed; }

    /* Memory: expand to fill remaining space */
    #memory-panel { flex: 1 1 0; overflow-y: auto; overflow-x: hidden; min-height: 0; }

    /* Display + gamepad: fixed natural height */
    #display { flex: 0 0 auto; padding: 0; margin: 0; }
    #display-wrapper { padding: 0; margin: 0; }
    #gameboy-controls { flex: 0 0 auto; }

    /* Console: fixed height */
    #console-panel { flex: 0 0 200px; overflow: hidden; }
    #console { height: 100%; overflow-y: auto; }

    /* Controls: compact */
    #controls { flex: 0 0 auto; }

    /* === Single-column stacking mode === */
    main.one-col {
      display: flex !important;
      flex-direction: column !important;
      height: 100vh;
    }
    main.one-col #left {
      flex: 0 0 auto;
      height: auto;
      max-height: none;
    }
    main.one-col #center {
      flex: 0 0 auto;
      height: auto;
      max-height: none;
    }
    /* In 1-col, memory expands but with a reasonable max */
    main.one-col #memory-panel {
      flex: 1 1 200px;
      max-height: 40vh;
      min-height: 100px;
    }
    main.one-col #console-panel {
      flex: 0 0 150px;
    }
    /* Allow whole page to scroll in 1-col */
    main.one-col {
      overflow-y: auto;
    }
  `
  document.head.appendChild(style)

  // Console input — wire the terminal to send input to Java
  const consoleInput = document.getElementById('console-input')
  if (consoleInput) {
    const newInput = consoleInput.cloneNode(true)
    consoleInput.parentNode.replaceChild(newInput, consoleInput)
    newInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        const text = newInput.value
        newInput.value = ''
        consolePrint('> ' + text)
        sendCommand('input', { text })
      }
    })
  }

  // Connect to SSE
  eventSource = new EventSource('/api/events')

  eventSource.onmessage = (event) => {
    try {
      const state = JSON.parse(event.data)
      applyState(state, updateUI, consolePrint, display, memory, cpu)
    } catch (err) {
      console.error('Failed to parse remote state:', err)
    }
  }

  eventSource.onerror = () => {
    consolePrint('[Remote connection lost]')
  }

  // Wire gamepad buttons
  wireGamepadRemote()

  consolePrint('[Connected to IntelliJ debug session]')
  return true
}

function applyState(state, updateUI, consolePrint, display, memory, cpu) {
  // Update registers
  if (state.registers && cpu) {
    const r = state.registers
    cpu.registers.AX = r.AX
    cpu.registers.BX = r.BX
    cpu.registers.CX = r.CX
    cpu.registers.DX = r.DX
    cpu.registers.SI = r.SI
    cpu.registers.DI = r.DI
    cpu.registers.SP = r.SP
    cpu.registers.BP = r.BP
    cpu.registers.HP = r.HP
    cpu.registers.IP = r.IP
    if (r.CS !== undefined) cpu.registers.CB = r.CS
    cpu.registers.setFlags(r.ZF, r.SF, r.CF, r.OF)
  }

  // Update memory
  if (state.memory && memory) {
    const bytes = Uint8Array.from(atob(state.memory), c => c.charCodeAt(0))
    if (bytes.length !== memory.data.length) {
      memory.resize(bytes.length)
    }
    memory.data.set(bytes)
    if (state.registers) {
      memory.cs = state.registers.CS || 0
    }
  }

  // Update VRAM
  if (state.vram && display) {
    const vramBytes = Uint8Array.from(atob(state.vram), c => c.charCodeAt(0))
    const vram = display.getVRAM()
    vram.set(vramBytes)
    display.refresh()
  }

  // Console output
  if (state.console) {
    consolePrint(state.console)
  }

  // Refresh UI
  updateUI()
}

function sendCommand(action, extra) {
  const body = { action, ...extra }
  fetch('/api/command', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  }).catch(err => console.error('Command failed:', err))
}

function wireGamepadRemote() {
  const buttons = {
    'btn-right': 0x01, 'btn-left': 0x02, 'btn-up': 0x04, 'btn-down': 0x08,
    'btn-a': 0x10, 'btn-b': 0x20, 'btn-select': 0x40, 'btn-start': 0x80
  }

  for (const [id, bit] of Object.entries(buttons)) {
    const el = document.getElementById(id)
    if (!el) continue

    el.addEventListener('mousedown', () => {
      sendCommand('button', { type: 'press', bit })
    })

    el.addEventListener('mouseup', () => {
      sendCommand('button', { type: 'release', bit })
    })
  }
}
