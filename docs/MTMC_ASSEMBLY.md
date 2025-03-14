# MTMC Assembly Guide

## Overview

MTMC assembly has a fairly large number of core instruction (37 or 52, depending on how you cound them) split across seven types:

* Miscellaneous Instructions
* ALU Related Instructions
* Stack Related Instructions
* The Call Instruction
* Jump Instructions
* The Load Immediate Instruction
* Load/Store Instructions

The MTMC computer is a 16-bit machine with 4k of memory and 2-byte words.  Most operations work on 2-byte words.  Two data-types are supported: 16-bit signed integers and bytes/characters, which follow the ASCII standard.  

There are 16 user-facing registers usable by name in assembly: 

| index | name   | description                                                           |
|-------|--------|-----------------------------------------------------------------------|
| 0     | `t0`   | temp register 0, holds temporary values, also tested for jumps        |
| 1     | `t1`   | temp register 1, holds temporary values                               |
| 2     | `t2`   | temp register 2, holds temporary values                               |
| 3     | `t3`   | temp register 3, holds temporary values                               |
| 4     | `a0`   | arg register 0, holds the first argument for a function call          |
| 5     | `a1`   | arg register 1, holds the second argument for a function call         |
| 6     | `a2`   | arg register 2, holds the third argument for a function call          |
| 7     | `a3`   | arg register 3, holds the fourth argument for a function call         |
| 8     | `r0`   | return value register 0, holds the return value for a function call   |
| 9     | `ra`   | return address register, holds the return address for a function call |
| 10    | `fp`   | frame pointer, points to the top of the current function frame        |
| 11    | `sp`   | stack pointer, points to the bottom of the current function frame     |
| 12    | `bp`   | break pointer, points to the top of the current heap space            |
| 13    | `pc`   | program counter, points to the next instruction to execute            |
| 14    | `zero` | a register that holds the value zero                                  |
| 15    | `one`  | a register that  holds the value one                                  |

Note that the `zero` and `one` registers are initialized with the values `0` and `1` respectively and it is highly recommended that you not overwrite them.

## MTMC Assembly Basics

MTMC Assembly programs consist of up to two sections, a data section, in which you can declare data, and a code section, in which you declare code.

If you wish to have a data section, you must begin your assembly file with a line with the `.data` directive.

Following this line you may declare data in the following forms:

```asm
.data
  example_int: 10
  example_str: "This is a string" # example string
  example_byte: 0xFF
```

Labels must be the first token on a line and must end with a colon.  If you wish to refer to a label, the name of the label is the text without the colon.

Note that strings will be implicitly zero terminated.

The above data will be placed after the code segment in memory, in a data segment.  The break pointer will point to the next memory cell after the data segment.

Also note that the `#` character can be used for line comments.

Following this optional data section is a code section. If you have a data section you must begin the code segment with a `.code` directive.  If you do not have a `.data` segment, then that is not necessary.

Execution of programs begin at the first instruction in the code section, which will be placed in memory location 0 by the operating system when running a program.

Here is a sample program continuing with the data section above:

```asm
.code
  ldi t0, example_str # load address of the string
  mv a0, t0           # move it into the arg reg
  sys wstr            # write the string to the console
  sys exit            # exit the program  
```

This program loads the address of the string into `t0` then moves it to `a0`, then invokes the `wstr` system call, asking the operating system to write the string to the console.  Finally, the program exits.

