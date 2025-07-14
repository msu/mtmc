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


## Frequently Asked Quesstions

### What does MTMC stand for?

*Montana State Minicomputer* - named after Montana State University where it was
conceived and developed. *MT* is the two-letter abbreviation for "Montana" while *MC*
is short for "Mini Computer*. 

"Mini Computers" were systems like the PDP-11 that fit within a single rack rather than
a full room like a Mainframe Computer. These ultimately gave way to "Micro Computers"
which were home computers like the Commodore 64 and the IBM PC.

### What does the 16 in MTMC-16 stand for?

The 16 is a reference to the MTMC being a 16-bit computer. This means that
integers are 2 bytes or 16 bits wide. 

This is known as the "word size" of the computer and is based upon the size 
of the *Arithmatic Logic Unit* (ALU) in the CPU. The ALU is the part of the 
CPU that does math like addition, subtraction, and boolean operations. 

### What is assembly language?

Computer CPUs "decode" binary instructions to activate different parts of the CPU
like the *Arithmatic Logic Unit* (ALU) and memory *Control Unit* (CU) for instruction
execution. These components work together to complete the instruction.

Assembly language prevents programmers from having to manually encode instructions
in binary by providing a text-based, human friendly (properly known as "symbolic") 
representation of the instructions.

While it was common to hand write binary instructions in the early days of
computing, the advent of assemblers made that practice very rare.


### Is MTMC-16 a CISC or RISC processor?

The MTMC-16 is based on a *Reduced Instruction Set Computing* (RISC) design where 
data is loaded into registers before being operated on. Branching occurs based 
on explicit test instructions (e.g. `eq`, `lt`, `gt`) that set a test flag.

Classic *Complex Instruction Set Computing* (CISC) processors could perform 
operations directly from memory such as adding a memory value to a special 
register called an *accumulator*. The changes to the accumulator would have 
side effects like setting a *zero flag* that could be used to determine a 
conditional jump.

CISC was more efficient in the early days as it required fewer instructions to
accomplish the same work. While a CISC design would have been more represetative
of early processors, they're harder to understnad and not representative of 
how modern CPUs work. 

### Why is the code section in assembly called `.text`?

This is a fun bit of history that has carried all the way into modern assemblers. When
assembly was first created, *code* was a reference to *machine code*—the bytes of data
executed by the CPU. This came from the idea that computer instructions had to be 
*encoded* as binary numbers the computer could understand.

Assembly Language was created as a textual form of machine code and was thus referred
to as "text" rather than "code". Over time the term "code" shifted to refer to any
representation of software. Including higher level languages like C, Java, Python,
and many others.


### Where is the operating system in memory?

Early computers kept the operating system in *Read Only Memory* (ROM) that was
not intended to be accessed by end-user programs. The MTMC-16 keeps up this 
tradition by providing basic services like console, filesystem, and display without
taking up valuable *Random Access Memory* (RAM) reserved for end-user programs.

### How many cycles does each instruction take?

In real world computers the execution of an instruction typically takes more 
than one clock cycle. Knowing how many cycles each instruction takes is important
for *cycle counting*, a practice that was critical in classic game consoles like
the Atari 2600 and Gameboy.

The purpose of cycle counting was to ensure that the program would complete
game updates and drawing operations before the next frame needed to be rendered.

In the MTMC-16, all instructions take a single cycle to execute. This keeps things
simple and lets students focus more on the logic and less on the minutia of how
the computer operates. 
