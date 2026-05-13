import { describe, it, expect } from 'vitest'
import { compileCmm } from '../cmm-compiler.js'
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

// ==================== Tier 5: Local Arrays & Array Decay ====================

describe('C-- Compiler: Local Arrays & Array Decay', () => {
  it('should support local arrays', () => {
    const { intOutput } = run(`
      int main() {
        int arr[3];
        arr[0] = 10; arr[1] = 20; arr[2] = 30;
        print_int(arr[0]); print_int(arr[1]); print_int(arr[2]);
        return 0;
      }
    `)
    expect(intOutput).toEqual([10, 20, 30])
  })

  it('should support local arrays with loops', () => {
    const { intOutput } = run(`
      int main() {
        int arr[5]; int i;
        i = 0;
        while (i < 5) { arr[i] = i * 10; i = i + 1; }
        i = 0;
        while (i < 5) { print_int(arr[i]); i = i + 1; }
        return 0;
      }
    `)
    expect(intOutput).toEqual([0, 10, 20, 30, 40])
  })

  it('should not clobber adjacent locals', () => {
    const { intOutput } = run(`
      int main() {
        int before; int arr[4]; int after;
        before = 111; after = 222;
        arr[0] = 1; arr[1] = 2; arr[2] = 3; arr[3] = 4;
        print_int(before); print_int(arr[0]); print_int(arr[3]); print_int(after);
        return 0;
      }
    `)
    expect(intOutput).toEqual([111, 1, 4, 222])
  })

  it('should decay global array to pointer parameter', () => {
    const { intOutput } = run(`
      int arr[3] = {10, 20, 30};
      int printArray(int* p, int len) {
        int i; i = 0;
        while (i < len) { print_int(p[i]); i = i + 1; }
        return 0;
      }
      int main() { printArray(arr, 3); return 0; }
    `)
    expect(intOutput).toEqual([10, 20, 30])
  })

  it('should decay local array to pointer parameter', () => {
    const { intOutput } = run(`
      int printArray(int* p, int len) {
        int i; i = 0;
        while (i < len) { print_int(p[i]); i = i + 1; }
        return 0;
      }
      int main() {
        int arr[4];
        arr[0] = 100; arr[1] = 200; arr[2] = 300; arr[3] = 400;
        printArray(arr, 4);
        return 0;
      }
    `)
    expect(intOutput).toEqual([100, 200, 300, 400])
  })

  it('should assign array to pointer and index through it', () => {
    const { intOutput } = run(`
      int main() {
        int arr[3]; int* p;
        arr[0] = 7; arr[1] = 8; arr[2] = 9;
        p = arr;
        print_int(*p); print_int(p[1]); print_int(p[2]);
        return 0;
      }
    `)
    expect(intOutput).toEqual([7, 8, 9])
  })

  it('should support local char arrays', () => {
    const { strOutput } = run(`
      int main() {
        char buf[4];
        buf[0] = 'H'; buf[1] = 'i'; buf[2] = '!'; buf[3] = 0;
        print_string(buf);
        return 0;
      }
    `)
    expect(strOutput).toEqual(['Hi!'])
  })
})

// ==================== Tier 6: Memory Protection ====================

describe('C-- Compiler: Memory Protection', () => {
  it('should segfault on write to string literal', () => {
    expect(() => run(`
      int main() {
        char* s; s = "hello";
        s[0] = 'H';
        return 0;
      }
    `)).toThrow(/Segmentation fault/)
  })

  it('should allow reading string literals', () => {
    const { strOutput } = run(`
      int main() { print_string("hello"); return 0; }
    `)
    expect(strOutput).toEqual(['hello'])
  })

  it('should allow writing to mutable globals', () => {
    const { intOutput } = run(`
      int x;
      int main() { x = 42; print_int(x); return 0; }
    `)
    expect(intOutput).toEqual([42])
  })
})

// ==================== Tier 7: Pointer Arithmetic ====================

describe('C-- Compiler: Pointer Arithmetic', () => {
  it('should scale int pointer addition', () => {
    const { intOutput } = run(`
      int arr[3] = {10, 20, 30};
      int main() {
        int* p; p = arr;
        print_int(*(p + 0)); print_int(*(p + 1)); print_int(*(p + 2));
        return 0;
      }
    `)
    expect(intOutput).toEqual([10, 20, 30])
  })

  it('should scale int pointer addition on local arrays', () => {
    const { intOutput } = run(`
      int main() {
        int arr[4];
        arr[0] = 100; arr[1] = 200; arr[2] = 300; arr[3] = 400;
        int* p; p = arr;
        print_int(*(p + 2)); print_int(*(p + 3));
        return 0;
      }
    `)
    expect(intOutput).toEqual([300, 400])
  })

  it('should scale int pointer subtraction', () => {
    const { intOutput } = run(`
      int arr[5] = {10, 20, 30, 40, 50};
      int main() {
        int* p; p = arr;
        p = p + 4; print_int(*p);
        p = p - 2; print_int(*p);
        return 0;
      }
    `)
    expect(intOutput).toEqual([50, 30])
  })

  it('should not scale char pointer addition', () => {
    const { strOutput } = run(`
      int main() {
        char buf[4];
        buf[0] = 'A'; buf[1] = 'B'; buf[2] = 'C'; buf[3] = 0;
        char* p; p = buf;
        print_char(*(p + 0)); print_char(*(p + 1)); print_char(*(p + 2));
        return 0;
      }
    `)
    expect(strOutput).toEqual(['A', 'B', 'C'])
  })

  it('should walk an array with pointer arithmetic in a loop', () => {
    const { intOutput } = run(`
      int arr[5] = {2, 4, 6, 8, 10};
      int main() {
        int* p; p = arr;
        int sum; sum = 0;
        int i; i = 0;
        while (i < 5) { sum = sum + *(p + i); i = i + 1; }
        print_int(sum);
        return 0;
      }
    `)
    expect(intOutput).toEqual([30])
  })

  it('should support pointer increment pattern', () => {
    const { intOutput } = run(`
      int arr[3] = {11, 22, 33};
      int main() {
        int* p; p = arr;
        print_int(*p);
        p = p + 1; print_int(*p);
        p = p + 1; print_int(*p);
        return 0;
      }
    `)
    expect(intOutput).toEqual([11, 22, 33])
  })
})
