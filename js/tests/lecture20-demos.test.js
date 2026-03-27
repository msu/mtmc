import { describe, it, expect } from 'vitest'
import { assemble } from '../assembler.js'
import { Memory, CPU } from '../emulator.js'

describe('Lecture 20: Memory Structures', () => {

  it('declaring and accessing arrays', () => {
    const { cpu } = runProgram(`
numbers: DW 10, 20, 30, 40, 50
  MOV BX, numbers
  MOV AX, [BX]
  ADD BX, 2
  MOV CX, [BX]
  HALT
    `)
    expect(cpu.registers.AX).toBe(10)
    expect(cpu.registers.CX).toBe(20)
  })

  it('array indexing with SHL', () => {
    const { cpu } = runProgram(`
numbers: DW 10, 20, 30, 40, 50
  MOV BX, numbers
  MOV CX, 3
  SHL CX, 1
  ADD BX, CX
  MOV AX, [BX]
  HALT
    `)
    expect(cpu.registers.AX).toBe(40)
  })

  it('array sum loop', () => {
    const { cpu } = runProgram(`
arr: DW 10, 20, 30, 40, 50
count: DW 5
  MOV BX, arr
  MOV CX, [count]
  MOV AX, 0
loop:
  ADD AX, [BX]
  ADD BX, 2
  DEC CX
  JNZ loop
  HALT
    `)
    expect(cpu.registers.AX).toBe(150)
  })

  it('2D array access', () => {
    const { cpu } = runProgram(`
matrix: DW 1,2,3,4, 5,6,7,8, 9,10,11,12
  MOV AX, 1
  MOV BX, 4
  MUL BX
  ADD AX, 2
  SHL AX, 1
  MOV BX, matrix
  ADD BX, AX
  MOV CX, [BX]
  HALT
    `)
    expect(cpu.registers.CX).toBe(7)
  })

  it('structures', () => {
    const { cpu } = runProgram(`
point1: DW 100, 200
  MOV BX, point1
  MOV AX, [BX]
  MOV CX, [BX+2]
  HALT
    `)
    expect(cpu.registers.AX).toBe(100)
    expect(cpu.registers.CX).toBe(200)
  })

  it('array of structures', () => {
    const { cpu } = runProgram(`
points: DW 10,20, 30,40, 50,60
  MOV BX, points
  MOV AX, 1
  MOV CX, 4
  MUL CX
  ADD BX, AX
  MOV DX, [BX+2]
  HALT
    `)
    expect(cpu.registers.DX).toBe(40)
  })

  it('DW label relocation regression', () => {
    const { cpu } = runProgram(`
a: DW 10, b
b: DW 20, 0
  MOV BX, a
  MOV AX, [BX+2]
  MOV CX, b
  HALT
    `)
    expect(cpu.registers.AX).toBe(cpu.registers.CX)
  })

  it('linked list traversal', () => {
    const { cpu } = runProgram(`
node3: DW 30, node4
node1: DW 10, node2
node4: DW 40, 0
node2: DW 20, node3

  MOV BX, node1
  MOV AX, 0
loop:
  CMP BX, 0
  JE done
  ADD AX, [BX]
  MOV BX, [BX+2]
  JMP loop
done:
  HALT
    `)
    expect(cpu.registers.AX).toBe(100)
  })

  it('stack frame locals', () => {
    const { cpu } = runProgram(`
  PUSH FP
  MOV FP, SP
  SUB SP, 6

  MOV AX, 11
  MOV [FP-2], AX
  MOV AX, 22
  MOV [FP-4], AX
  MOV AX, 33
  MOV [FP-6], AX

  MOV AX, [FP-2]
  MOV BX, [FP-4]
  MOV CX, [FP-6]

  MOV SP, FP
  POP FP
  HALT
    `)
    expect(cpu.registers.AX).toBe(11)
    expect(cpu.registers.BX).toBe(22)
    expect(cpu.registers.CX).toBe(33)
  })

  it('BK is word-aligned after odd-sized data', () => {
    // DB "Hello", 0 is 6 bytes, DW 42 is 2 bytes = 8 bytes (already even)
    // DB "Hi", 0 is 3 bytes (odd) — BK should round up to even
    const bytecode = assemble(`
msg: DB "Hi", 0
  HALT
    `)
    // BK is at header offset 0x0010-0x0011 (big-endian)
    const bk = (bytecode[0x10] << 8) | bytecode[0x11]
    expect(bk % 2).toBe(0)
  })

  it('simple heap allocation', () => {
    const { cpu } = runProgram(`
.MEMORY 8K
  MOV AX, 10
  MOV BX, BK
  ADD BK, AX
  MOV AX, BX
  HALT
    `, 8192)

    // BX held old BK, and BK advanced by 10
    expect(cpu.registers.AX).toBe(cpu.registers.BX)
    expect(cpu.registers.BK).toBe(cpu.registers.BX + 10)
  })

  it('malloc and fill', () => {
    const { cpu, mem } = runProgram(`
.MEMORY 8K
  JMP main
malloc:
  SHL AX, 1
  MOV BX, BK
  ADD AX, BX
  CMP AX, SP
  JGE failed
  MOV BK, AX
  MOV AX, BX
  RET
failed:
  MOV AX, 0
  RET

main:
  MOV AX, 5
  CALL malloc
  MOV BX, AX
  MOV CX, 5
  MOV DX, 10
fill:
  MOV [BX], DX
  ADD DX, 10
  ADD BX, 2
  DEC CX
  JNZ fill
  HALT
    `, 8192)

    // BX advanced past the array, subtract back to find start
    const arrayStart = cpu.registers.BX - 10
    expect(mem.readWord(arrayStart)).toBe(10)
    expect(mem.readWord(arrayStart + 2)).toBe(20)
    expect(mem.readWord(arrayStart + 4)).toBe(30)
    expect(mem.readWord(arrayStart + 6)).toBe(40)
    expect(mem.readWord(arrayStart + 8)).toBe(50)
  })

  it('pointer arithmetic', () => {
    const { cpu } = runProgram(`
array: DW 10, 20, 30, 40, 50
  MOV BX, array
  ADD BX, 2
  ADD BX, 2
  SUB BX, 2
  SUB BX, 2
  MOV AX, [BX]
  HALT
    `)
    expect(cpu.registers.AX).toBe(10)
  })

  it('string iteration counting characters', () => {
    const { cpu } = runProgram(`
message: DB "Hello", 0
  MOV BX, message
  MOV CX, 0
loop:
  MOV AL, [BX]
  CMP AX, 0
  JE done
  INC CX
  INC BX
  JMP loop
done:
  HALT
    `)
    expect(cpu.registers.CX).toBe(5)
  })

  it('register-relative loop with indexed addressing', () => {
    const { cpu } = runProgram(`
arr: DW 10, 20, 30, 40, 50
  MOV BX, arr
  MOV CX, 0
  MOV DX, 0
loop:
  CMP CX, 10
  JGE done
  MOV AX, [BX+CX]
  ADD DX, AX
  ADD CX, 2
  JMP loop
done:
  HALT
    `)
    expect(cpu.registers.DX).toBe(150)
  })
})

function runProgram(source, memSize = 1024) {
  const bytecode = assemble(source)
  const mem = new Memory(memSize)
  mem.load(bytecode)
  const cpu = new CPU(mem)
  let steps = 0
  while (cpu.step() && steps++ < 10000) {}
  return { cpu, mem }
}
