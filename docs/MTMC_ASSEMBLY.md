# MTMC Assembly Guide

## Overview

MTMC assembly has a fairly large number of core instruction (37 or 52, depending on how you count them) split across seven types:

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
| 0     | `t0`   | temp register 0, holds temporary values                               |
| 1     | `t1`   | temp register 1, holds temporary values                               |
| 2     | `t2`   | temp register 2, holds temporary values                               |
| 3     | `t3`   | temp register 3, holds temporary values                               |
| 4     | `t4`   | temp register 4, holds temporary values                               |
| 5     | `t5`   | temp register 5, holds temporary values                               |
| 6     | `a0`   | arg register 0, holds the first argument for a function call          |
| 7     | `a1`   | arg register 1, holds the second argument for a function call         |
| 8     | `a2`   | arg register 2, holds the third argument for a function call          |
| 9     | `a3`   | arg register 3, holds the fourth argument for a function call         |
| 10    | `rv`   | return value register, holds the return value for a function call     |
| 11    | `ra`   | return address register, holds the return address for a function call |
| 12    | `fp`   | frame pointer, points to the top of the current function frame        |
| 13    | `sp`   | stack pointer, points to the bottom of the current function frame     |
| 14    | `bp`   | break pointer, points to the top of the current heap space            |
| 15    | `pc`   | program counter, points to the next instruction to execute            |


## MTMC Assembly Basics

MTMC Assembly programs consist of up to two sections, a data section, in which you can declare data, and a text section in which you declare code.

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

Following this optional data section is a text section for code. If you have a data section you must begin the code segment with a `.text` directive.  If you do not have a `.data` segment, then that is not necessary.

Execution of programs begin at the first instruction in the code section, which will be placed in memory location 0 by the operating system when running a program.

Here is a sample program continuing with the data section above:

```asm
.text
  ldi t0, example_str # load address of the string
  mv a0, t0           # move it into the arg reg
  sys wstr            # write the string to the console
  sys exit            # exit the program  
```

This program loads the address of the string into `t0` then moves it to `a0`, then invokes the `wstr` system call, asking the operating system to write the string to the console.  Finally, the program exits.

## Data Types

Data can specified in a variety of formats. These representations provide convenient methods of specifying data.

Note that some representations can only be used in the `.data` section. 

| Type       | Examples        | Size           | .data | .text | Description                                      |
|------------|-----------------|----------------|-------|-------|--------------------------------------------------|
| Decimal    | `1`, `7`, `-10` | Word (2 bytes) | ✅    | ✅    | Any base-10 intger value from -32,768 to 32,767  |
| Hexadecimal| `0x02`, `0xFA`  | Word (2 bytes) | ✅    | ✅    | Any base-16 value from 0x0000 to 0xFFFF          |
| Binary     | `0b0000_0001`   | Word (2 bytes) | ✅    | ✅    | Binary encoded numbers start with '0b' followed by 1s and 0s. Underscores are ignored. |
| String     | `"This is a string"` | 1 byte per character + 1 byte | ✅  | ❌ | Strings are an array of bytes encoded in ASCII and automatically terminated with a null terminator (0x00 byte) |
| Byte Array | `.byte 256`     | Specified bytes| ✅    | ❌    | Allocates an empty array of bytes to the length specified |
| Word Array | `.int 256`      | Specified words (count * 2 bytes)| ✅   | ❌   | Allocates an empty array of words to the length specified |
| Image      | `.image "title.png"` | Word (2 bytes) | ✅ | ❌  | A reference to an image stored in the program ROM. The reference only takes two bytes and references the full data stored in the executable. |

Note that the MTMC-16 lacks CPU support for the following data types and thus the assembly language does not understand them:

* floating point numbers (e.g. `3.14159`)
* binary coded decimal (e.g. `'12345'`)

## Order of Operands

MTMC-16 assembly follows the common ordering of operands used by most assembly langagues.

Load instructions like `li a0 10` always move data from the rightmost operand to the leftmost operand. They can be thought of as saying, "let a0 = 10". 

Store instructions like `sw a0 my_var` always move data from the leftmost operand to the rightmost operand. They can be thought of as saying "let mem[my_var] = a0".

When loading data, be aware of the difference between a constant value and a memory address. For example, `li a0 my_var` loads the memory address of `my_var` into `a0` and *not* the value stored at `my_var`. That's becuase `li` is used to load contant values. When assembled, `my_var` is transformed into a memory address like `14` and the instruction becomes `li a0 14`. 

This can be easily confused with `lw a0 my_var` which says, "load a word stored at memory address `my_var`". The assembler will still translate `my_var` to `14`, but the CPU will interpet the `14` as a location in memory rather than a literal value.

To help keep prevent confusion, the assembler offers the `la` alias for `li` to separate loading addresses from literals.

Example:

```asm
li t0 14      # Load the number 14 into t0
la t1 my_var  # Load address my_var into t1
lw t2 my_var  # Load a word located at address my_var into t2
```
