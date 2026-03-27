import { describe, it, expect } from 'vitest'
import { compileCmm } from '../cmm-compiler.src.js'
import { assemble } from '../assembler.js'
import { CPU, Memory } from '../emulator.js'
import { OS } from '../os.js'

// Helper: compile C--, assemble, run, return output
function run(source) {
  const result = compileCmm(source)
  if (result.error) throw new Error(result.error)

  const bytecode = assemble(result.assembly)
  const memory = new Memory(8192)
  const loadResult = memory.loadBinary(bytecode)

  const intOutput = []
  const strOutput = []

  const os = new OS(null, memory)
  const cpu = new CPU(memory, os)
  os.cpu = cpu
  cpu.registers.HP = loadResult.breakPointer
  cpu.registers.CB = loadResult.codeBase

  os.setOutputCallback((text) => {})
  os.setHaltCallback(() => { cpu.halted = true })

  // Intercept OS print methods (CPU handles IP increment)
  os.sysPrintInt = function() {
    const v = this.cpu.registers.AX
    intOutput.push(v > 32767 ? v - 65536 : v)
  }
  os.sysPrintChar = function() {
    strOutput.push(String.fromCharCode(this.cpu.registers.AX & 0xFF))
  }
  os.sysPrintString = function() {
    let addr = this.cpu.registers.AX
    let s = ''
    let ch
    while ((ch = this.memory.readByte(addr)) !== 0) {
      s += String.fromCharCode(ch)
      addr++
    }
    strOutput.push(s)
  }

  let steps = 0
  while (!cpu.halted && steps < 100000) {
    cpu.step()
    steps++
  }

  if (!cpu.halted) throw new Error('Program did not halt within ' + steps + ' steps')
  return { intOutput, strOutput }
}

// ==================== Tier 1: Basics ====================

describe('C-- Compiler: Basics', () => {
  it('should print a literal', () => {
    const { intOutput } = run('int main() { print_int(42); return 0; }')
    expect(intOutput).toEqual([42])
  })

  it('should handle variable assignment', () => {
    const { intOutput } = run('int main() { int x; x = 42; print_int(x); return 0; }')
    expect(intOutput).toEqual([42])
  })

  it('should add', () => {
    const { intOutput } = run('int main() { print_int(10 + 5); return 0; }')
    expect(intOutput).toEqual([15])
  })

  it('should subtract', () => {
    const { intOutput } = run('int main() { print_int(20 - 7); return 0; }')
    expect(intOutput).toEqual([13])
  })

  it('should multiply', () => {
    const { intOutput } = run('int main() { print_int(6 * 7); return 0; }')
    expect(intOutput).toEqual([42])
  })

  it('should divide', () => {
    const { intOutput } = run('int main() { print_int(20 / 4); return 0; }')
    expect(intOutput).toEqual([5])
  })

  it('should modulo', () => {
    const { intOutput } = run('int main() { print_int(17 % 5); return 0; }')
    expect(intOutput).toEqual([2])
  })

  it('should negate', () => {
    const { intOutput } = run('int main() { int x; x = 10; print_int(-x); return 0; }')
    expect(intOutput).toEqual([-10])
  })

  it('should respect operator precedence', () => {
    const { intOutput } = run('int main() { int a; a = 2 + 3 * 4; print_int(a); return 0; }')
    expect(intOutput).toEqual([14])
  })

  it('should handle modulo in comparison', () => {
    const { intOutput } = run(`
      int main() {
        int n;
        n = 6;
        if (n % 2 == 0) { print_int(1); } else { print_int(0); }
        n = 7;
        if (n % 2 == 0) { print_int(1); } else { print_int(0); }
        return 0;
      }
    `)
    expect(intOutput).toEqual([1, 0])
  })
})

// ==================== Tier 2: Control Flow ====================

describe('C-- Compiler: Control Flow', () => {
  it('should handle if-else', () => {
    const { intOutput } = run(`
      int main() {
        if (10 > 5) { print_int(1); } else { print_int(0); }
        if (3 > 5) { print_int(1); } else { print_int(0); }
        return 0;
      }
    `)
    expect(intOutput).toEqual([1, 0])
  })

  it('should handle all comparisons', () => {
    const { intOutput } = run(`
      int main() {
        int a; int b; a = 5; b = 5;
        if (a == b) { print_int(1); } else { print_int(0); }
        if (a != b) { print_int(1); } else { print_int(0); }
        a = 3;
        if (a < b) { print_int(1); } else { print_int(0); }
        if (a > b) { print_int(1); } else { print_int(0); }
        return 0;
      }
    `)
    expect(intOutput).toEqual([1, 0, 1, 0])
  })

  it('should handle while loops', () => {
    const { intOutput } = run(`
      int main() {
        int i; i = 1;
        while (i < 4) { print_int(i); i = i + 1; }
        return 0;
      }
    `)
    expect(intOutput).toEqual([1, 2, 3])
  })

  it('should handle logical operators', () => {
    const { intOutput } = run(`
      int main() {
        if (1 && 1) { print_int(1); } else { print_int(0); }
        if (1 && 0) { print_int(1); } else { print_int(0); }
        if (0 || 1) { print_int(1); } else { print_int(0); }
        if (0 || 0) { print_int(1); } else { print_int(0); }
        return 0;
      }
    `)
    expect(intOutput).toEqual([1, 0, 1, 0])
  })
})

// ==================== Tier 3: Functions ====================

describe('C-- Compiler: Functions', () => {
  it('should handle simple function', () => {
    const { intOutput } = run(`
      int getValue() { return 42; }
      int main() { print_int(getValue()); return 0; }
    `)
    expect(intOutput).toEqual([42])
  })

  it('should handle function with params', () => {
    const { intOutput } = run(`
      int add(int a, int b) { return a + b; }
      int main() { print_int(add(7, 3)); return 0; }
    `)
    expect(intOutput).toEqual([10])
  })

  it('should handle recursion', () => {
    const { intOutput } = run(`
      int factorial(int n) {
        if (n < 2) { return 1; }
        return n * factorial(n - 1);
      }
      int main() { print_int(factorial(5)); return 0; }
    `)
    expect(intOutput).toEqual([120])
  })

  it('should handle nested function calls', () => {
    const { intOutput } = run(`
      int square(int x) { return x * x; }
      int sumOfSquares(int a, int b) { return square(a) + square(b); }
      int main() { print_int(sumOfSquares(3, 4)); return 0; }
    `)
    expect(intOutput).toEqual([25])
  })

  it('should handle global variables', () => {
    const { intOutput } = run(`
      int counter = 0;
      int increment() { counter = counter + 1; return counter; }
      int main() { increment(); increment(); increment(); print_int(counter); return 0; }
    `)
    expect(intOutput).toEqual([3])
  })
})

// ==================== Tier 4: Advanced ====================

describe('C-- Compiler: Advanced', () => {
  it('should handle global arrays', () => {
    const { intOutput } = run(`
      int arr[3];
      int main() {
        arr[0] = 10; arr[1] = 20; arr[2] = 30;
        print_int(arr[0]); print_int(arr[1]); print_int(arr[2]);
        return 0;
      }
    `)
    expect(intOutput).toEqual([10, 20, 30])
  })

  it('should handle strings', () => {
    const { strOutput } = run(`
      int main() { print_string("Hello"); return 0; }
    `)
    expect(strOutput).toEqual(['Hello'])
  })

  it('should handle isPrime', () => {
    const { intOutput } = run(`
      int isPrime(int n) {
        int i;
        if (n < 2) { return 0; }
        i = 2;
        while (i < n) {
          if (n % i == 0) { return 0; }
          i = i + 1;
        }
        return 1;
      }
      int main() {
        int n; n = 2;
        while (n <= 20) {
          if (isPrime(n)) { print_int(n); }
          n = n + 1;
        }
        return 0;
      }
    `)
    expect(intOutput).toEqual([2, 3, 5, 7, 11, 13, 17, 19])
  })

  it('should handle GCD', () => {
    const { intOutput } = run(`
      int gcd(int a, int b) {
        while (b != 0) {
          int temp; temp = b;
          b = a % b;
          a = temp;
        }
        return a;
      }
      int main() {
        print_int(gcd(12, 8));
        print_int(gcd(100, 75));
        return 0;
      }
    `)
    expect(intOutput).toEqual([4, 25])
  })

  it('should handle Collatz sequence', () => {
    const { intOutput } = run(`
      int main() {
        int n; n = 6;
        print_int(n);
        while (n != 1) {
          if (n % 2 == 0) { n = n / 2; } else { n = n * 3 + 1; }
          print_int(n);
        }
        return 0;
      }
    `)
    expect(intOutput).toEqual([6, 3, 10, 5, 16, 8, 4, 2, 1])
  })
})
