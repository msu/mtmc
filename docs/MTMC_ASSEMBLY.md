# MTMC Assembly Guide

## What is Assembly Language (and why should I care?)

When you use a computer, you’re running programs. Maybe it's a game, a calculator, or something that writes text. But deep inside, every program is just a long list of instructions the computer follows.

These instructions are made of bits. Ones and zeros. Eight bits make a byte. And on the MTMC-16, two bytes make a word. A word is the basic size of data this computer understands. So when the MTMC-16 runs a program, it reads one word at a time and tries to follow the instruction it finds there.

All these instructions, when put together, are called machine code. The machine code tells the computer exactly what to do—like adding numbers, showing text, or moving data around in memory. But machine code is just numbers. It’s very hard for people to read or understand.

That’s why Assembly Language exists. Assembly is a way to write those same instructions using words instead of numbers. For example, instead of writing `0001001000110100`, you might write something like `add t0 t1`. Each Assembly instruction matches one machine instruction, and it’s much easier to read.

Learning Assembly Language helps you see how the computer really works. You’ll learn how it stores data, how it does math, and how it runs functions. You’ll even write your own programs, one instruction at a time.

This guide will teach you how to write Assembly for the MTMC-16—a simple, virtual computer made to help you learn. You’ll see how programs are built from the ground up, and you’ll understand what your computer is really doing behind the scenes.


## Registers and Memory

The brain of a computer is called the CPU. It runs programs by following instructions, one at a time. But just like a person solving a math problem, the CPU needs a place to store numbers and keep track of what it's doing. That’s what registers are for.

Registers are small, fast storage spaces inside the CPU. On the MTMC-16, there are 16 of them. Each register holds one word (that’s 2 bytes) or 16 bits. Some registers are used to hold temporary values. Some are used for passing arguments to functions. Others help the CPU remember where it is in a program or where to find more data.

Registers are the CPU’s short-term memory. They are very fast, but there are only a few of them.

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


What happens if a program needs to store more than 16 values? That’s where memory comes in.

Memory is much bigger than the registers. The MTMC-16 has 4,096 bytes of memory. That’s 2,048 words. This memory lives outside the CPU, but the CPU can read from it and write to it. You can think of it like a notebook the CPU can use when it runs out of space in its head.

To get data from memory, the CPU uses special instructions. It can load a value from memory into a register, or store a value from a register back into memory. This lets the CPU work with much more information than it can fit in its registers alone.

In Assembly, you’ll learn how to move values between memory and registers. You’ll also learn how to use the right register for the right job. Like holding return values, passing function arguments, or keeping track of the program’s progress.

Once you understand how registers and memory work together, you’ll be ready to write your first real instructions.


## Instructions

MTMC assembly has a fairly large number of core instruction (37 or 52, depending on how you count them) split across seven types:

* Miscellaneous Instructions
* ALU Related Instructions
* Stack Related Instructions
* The Call Instruction
* Jump Instructions
* The Load Immediate Instruction
* Load/Store Instructions

The MTMC computer is a 16-bit machine with 4k of memory and 2-byte words.  Most operations work on 2-byte words.  Two data-types are supported: 16-bit signed integers and bytes/characters. Characters are interpreted according to the ASCII standard.  

Some common instructions you will need to get started are:

| instruction | parameters              | description                                                           |
|-------------|-------------------------|-----------------------------------------------------------------------|
| `li`        | `register` `integer`    | Load a static word (16-bit) value into the specified register         | 
| `la`        | `register` `address`    | Load a static memory address (16-bit) value into the specified register  | 
| `lw`        | `register` `address`    | Load the word (16-bit) stored at the memory address given into the specified register |
| `lb`        | `register` `address`    | Load the byte (8-bit) stored at the memory address given into the specified register |
| `sw`        | `register` `address`    | Store the value of the register (16-bit) into the memory address specified |
| `sb`        | `register` `address`    | Store the lower-half of the register (8-bit) into the memory address specified |
| `add`       | `register` `register`   | Add the value of the second register into the first register          |
| `sub`       | `register` `register`   | Subtract the value of the second register from the first register     |
| `sys`       | `sys call name`         | Call an operating system feature like `exit` and `wstr` (write string)|

The full list of instructions are in the [MTMC Specification](MTMC_SPECIFICATION.md) document. 

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
  la  t0 example_str    # load address of the string into t0
  mov a0 t0             # move the first general purpose register into the first argument register
  sys wstr              # write the string to the console
  sys exit              # exit the program  
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

When loading data, be aware of the difference between a constant value and a memory address. For example, `li a0 my_var` loads the memory address of `my_var` into `a0` and *not* the value stored at `my_var`. That's because `li` is used to load constant values. When assembled, `my_var` is transformed into a memory address like `14` and the instruction becomes `li a0 14`. 

This can be easily confused with `lw a0 my_var` which says, "load a word stored at memory address `my_var`". The assembler will still translate `my_var` to `14`, but the CPU will interpret the `14` as a location in memory rather than a literal value.

To help keep prevent confusion, the assembler offers the `la` alias for `li` to separate loading addresses from literals.

Example:

```asm
li t0 14      # Load the number 14 into t0
la t1 my_var  # Load address my_var into t1
lw t2 my_var  # Load a word located at address my_var into t2
```
