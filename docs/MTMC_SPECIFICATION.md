# MTMC - MonTana state Mini Computer

The MonTana state Mini Computer is a virtual computer intended to show how digital computation works in a fun and visual 
way.

## Overall Architecture

- 16-bit binary computer
- byte-addressable
- 2 byte words
- 4k of Memory 
  - 4096 bytes/addresses
  - Upper 1k is a frame buffer for a display
  - 2048 total words
  - 1536 words/3072 bytes free excluding frame buffer
- 16 Registers (see below)
- 64x64 2-bit green scale display
  - `00` - #2a453b - black
  - `01` - #365d48 - dark
  - `10` - #577c44 - light
  - `11` - #7f860f - white
- Console for text input/output & commands
- Operating System (MTOS)
- Core data types are signed 16-bit integers & bytes

## Registers

The MTMC has a total of 16 user-facing register.  They are outlined below.

| index | name   | description                                                                      |
|-------|--------|----------------------------------------------------------------------------------|
| 0     | `t0`   | temp register 0 (aka "the accumulator") holds temporary values, tested for jumps |
| 1     | `t1`   | temp register 1, holds temporary values                                          |
| 2     | `t2`   | temp register 2, holds temporary values                                          |
| 3     | `t3`   | temp register 3, holds temporary values                                          |
| 4     | `a0`   | arg register 0, holds the first argument for a function call                     |
| 5     | `a1`   | arg register 1, holds the second argument for a function call                    |
| 6     | `a2`   | arg register 2, holds the third argument for a function call                     |
| 7     | `a3`   | arg register 3, holds the fourth argument for a function call                    |
| 8     | `rv`   | return value register, holds the return value for a function call                |
| 9     | `ra`   | return address register, holds the return address for a function call            |
| 10    | `fp`   | frame pointer, points to the top of the current function frame                   |
| 11    | `sp`   | stack pointer, points to the bottom of the current function frame                |
| 12    | `bp`   | break pointer, points to the top of the current heap space                       |
| 13    | `pc`   | program counter, points to the next instruction to execute                       |
| 14    | `zero` | a read-only register that always holds the value zero                            |
| 15    | `one`  | a read-only register that always holds the value one                             |

In addition to these registers, there are the following non-user facing registers:

| name | description                                    |
|------|------------------------------------------------|
| `ir` | that holds the current instruction to execute  |
| `cb` | a pointer to the boundary of the code segment  |
| `db` | a pointer to the boundary of the data segment  |
| `io` | the value of the latest non-console user input |

These registers are managed by the underlying operating system.

## Instructions (16 bit)

There are 7 instruction types in the MTSC, with a total of 37 instructions (52 instructions if you count the stack 
operations instructions separately).  The MTSC is not a RISC computer, but combines ideas from RISC (MIPS in particular),
from the [Scott CPU](https://www.youtube.com/watch?v=RRg5hRlywIg) and the [JVM](https://en.wikipedia.org/wiki/Java_virtual_machine) 

Instruction types can be determined by looking at the four high-order bits (nibble) of an instruction.

| top nibble | hex   | type            |
|------------|-------|-----------------|
| `0000`     | `0`   | MISC            |
| `0001`     | `1`   | ALU             |
| `0010`     | `2`   | STACK           |
| `0011`     | `3`   | STACK IMMEDIATE |
| `01xx`     | `4-7` | LOAD/STORE      |
| `10xx`     | `8-B` | LOAD IMMEDIATE  |
| `11xx`     | `C-F` | JUMP            |

### MISC

Misc (miscellaneous) instructions start with the nibble `0000`.  There are three such instructions:

| Instruction | Form                  | Description                                                               | Example            |
|-------------|-----------------------|---------------------------------------------------------------------------|--------------------|
| `move`      | `0000 rrrr ssss hhhh` | Moves the value in register `ssss` to `rrrr`, right shifting it by `hhhh` | `move t0 t1`       |
| `mask`      | `0000 1110 vvvv vvvv` | Does a bitwise AND of `t0` with the given byte value `vvvv vvvv`          | `mask 0b0000_1111` |
| `sys`       | `0000 1111 vvvv vvvv` | Issues syscall `vvvv vvvv`                                                | `sys wstr`         |

Note that the `zero` and `one` registers are not writeable, so the `mask` and `sys` instructions do not conflict with `move`

The instruction `0000 0000 0000 0000` is `mv t0 t0 0`, which does nothing and is aliased as `noop`

### ALU 
 
ALU operations start with the nibble `0001` and come in two forms:

- Binary operations
- Unary operations

Binary operations take the form `0001 oooo rrrr ssss`, where `oooo` is the operation, `rrrr` is the first register and
`ssss` is the second register.  Any registers can be referenced with ALU operations.  Logically, binary operations
end up looking like this:

```
rrrr = rrrr OP ssss
```

Unary operations take the form `0001 oooo rrrr 0000`, where `oooo` is the operation, `rrrr` is the register to modify.
The last four bits are ignored and should be `0`.

| Instruction | Form                  | Description                                                                                   | Example      |
|-------------|-----------------------|-----------------------------------------------------------------------------------------------|--------------|
| `add`       | `0001 0000 rrrr ssss` | Adds the value of `rrrr` to `ssss` and saves it to `rrrr`                                     | `add t0 t1`  |
| `sub`       | `0001 0001 rrrr ssss` | Subtracts the value of `rrrr` to `ssss` and saves it to `rrrr`                                | `sub t0 t1`  |
| `mul`       | `0001 0010 rrrr ssss` | Multiplies the value of `rrrr` to `ssss` and saves it to `rrrr`                               | `mul t0 t1`  |
| `div`       | `0001 0011 rrrr ssss` | Divides the value of `rrrr` by `ssss` and saves it to `rrrr`                                  | `div t0 t1`  |
| `mod`       | `0001 0100 rrrr ssss` | Computes the mod the value of `rrrr` by `ssss` and saves it to `rrrr`                         | `mod t0 t1`  |
| `and`       | `0001 0101 rrrr ssss` | Bitwise ANDs the value of `rrrr` by `ssss` and saves it to `rrrr`                             | `and t0 t1`  |
| `or`        | `0001 0110 rrrr ssss` | Bitwise ORs the value of `rrrr` by `ssss` and saves it to `rrrr`                              | `or t0 t1`   |
| `xor`       | `0001 0111 rrrr ssss` | Bitwise XORs the value of `rrrr` by `ssss` and saves it to `rrrr`                             | `xor t0 t1`  |
| `shl`       | `0001 1000 rrrr ssss` | Shifts the value in `rrrr` the unsigned value `vvvv` bits to the left and saves it to `rrrr`  | `shl t0 t1`  |
| `shr`       | `0001 1001 rrrr ssss` | Shifts the value in `rrrr` the unsigned value `vvvv` bits to the right and saves it to `rrrr` | `shr t0 t1`  |
| `eq`        | `0001 1010 rrrr ssss` | If the values in `rrrr` and `ssss` are equal, leaves `1` in `rrrr`, `0` otherwise             | `eq t0 t1`   |
| `lt`        | `0001 1011 rrrr ssss` | If the value in `rrrr` is less than `ssss` leaves `1` in `rrrr`, `0` otherwise                | `lt t0 t1`   |
| `lteq`      | `0001 1100 rrrr ssss` | If the value in `rrrr` is less than or equal to `ssss` leaves `1` in `rrrr`, `0` otherwise    | `lteq t0 t1` |
| `bnot`      | `0001 1101 rrrr 0000` | Bitwise NOTs the value of `rrrr` and saves it to `rrrr`                                       | `not t0`     |
| `not`       | `0001 1110 rrrr 0000` | Logical NOT: if the the value of `rrrr` is `0` set `rrrr` value to `1`, `0` otherwise         | `lnot t0`    |
| `neg`       | `0001 1111 rrrr 0000` | Negates the value of `rrrr` and saves it to `rrrr`                                            | `neg t0`     |

The second nibble of the instruction determines the ALU operation.  Here is a table of those operations:

| Operation | Hex | binary |
|-----------|-----|--------|
| `add`     | `0` | `0000` |
| `sub`     | `1` | `0001` |
| `mul`     | `2` | `0010` |
| `div`     | `3` | `0011` |
| `mod`     | `4` | `0100` |
| `and`     | `5` | `0101` |
| `or`      | `6` | `0110` |
| `xor`     | `7` | `0111` |
| `shl`     | `8` | `1000` |
| `shr`     | `9` | `1001` |
| `eq`      | `A` | `1010` |
| `lt`      | `B` | `1011` |
| `lteq`    | `C` | `1100` |
| `bnot`    | `D` | `1101` |
| `not`     | `E` | `1110` |
| `neg`     | `F` | `1111` |

### STACK

Stack operations start with the nibble `0010`.

The MTMC offers the following stack manipulation instructions: 

* push
* pop
* dup
* swap
* drop
* over
* rot
* stack operations (sop) ALU instructions

In the case of ALU stack operations, if the ALU operator is a binary operation, the top two values of the stack are 
consumed and the result is pushed back onto the stack.  If the operator is unary operation, the top value of the stack 
is consumed and the result is pushed back onto the stack.

Note that stacks always grow _down_ in memory on the MTMC.

#### Assembly Notes

Stack assembly instructions do not require you specify a stack pointer register.  If it is omitted then the assembler 
will assume you want to use the `sp` register.

| Instruction | Form                  | Description                                                                                                                                           | Example                                    |
|-------------|-----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------|
| `push`      | `0010 0000 rrrr ssss` | Pushes the word value of `rrrr` onto the stack pointed at by `ssss`, `ssss` is decrement by 2 bytes and the value in `rrrr` is saved to that location | `push t0` (`sp` is implied)                |
| `pop`       | `0010 0001 rrrr ssss` | Pops the word at the top of the stack pointed at by `ssss` into `rrrr`, `ssss` is incremented by 2 bytes.                                             | `pop ra, t4` (explicit stack pointer `t4`) |
| `dup`       | `0010 0010 0000 ssss` | Duplicates the word at the top of the stack pointed to by `ssss`.                                                                                     | `dup`                                      |
| `swap`      | `0010 0010 0001 ssss` | Swaps the two words at the top of the stack pointed to by `ssss`.                                                                                     | `swap`                                     |
| `drop`      | `0010 0010 0010 ssss` | Drops the top word of the stack pointed to by `ssss`.                                                                                                 | `drop`                                     |
| `over`      | `0010 0010 0011 ssss` | Copies the second word to the top of the stack pointed to by `ssss`.                                                                                  | `over`                                     |
| `rot`       | `0010 0010 0100 ssss` | Rotates the third word to the top of the stack pointed to by `ssss`.                                                                                  | `rot`                                      |
| `sop`       | `0010 0011 oooo ssss` | Applies the ALU operation `oooo` to the stack pointed at by the `ssss` register.                                                                      | `sop add`                                  |

### STACK IMMEDIATE

Stack immediate instructions start with the nibble `0011`.  They push the value of the lower byte of the instruction
onto the stack referred to by the register in the second nibble.

| Instruction | Form                  | Description                                                      | Example    |
|-------------|-----------------------|------------------------------------------------------------------|------------|
| `pushi`     | `0011 ssss vvvv vvvv` | Pushes the value `vvvv vvvv` onto the stack pointed to by `ssss` | `pushi 22` |

### LOAD/STORE

The MTMC allows you to load and store words and bytes in memory with "load" and "store" instructions.  These instructions
start with the two bits `01`. The next two bits then specify which type of load/store instruction it is.  The next
byte specifies a register holding the address to save to or read from.  The next byte specifies the register with the
address of the memory location.  The final byte specified an offset register, which holds a value to offset the address
register by.

| Instruction | Form                  | Description                                                                                        | Example       |
|-------------|-----------------------|----------------------------------------------------------------------------------------------------|---------------|
| `lw`        | `0100 rrrr aaaa oooo` | Loads the word (16-bit) value at the address in `aaaa`, offset by the value in `oooo`, into `rrrr` | `lw t0 fp t3` |
| `lb`        | `0101 rrrr aaaa oooo` | Loads the byte (8-bit) value at the address in `aaaa`, offset by the value in `oooo`, into `rrrr`  | `lb t0 fp t3` |
| `sw`        | `0110 rrrr aaaa oooo` | Saves the word (16-bit) value in `rrrr` to the address in `aaaa`, offset by the value in `oooo`    | `sw t0 fp t3` |
| `sb`        | `0111 rrrr aaaa oooo` | Saves the byte (8-bit) value in `rrrr` to the address in `aaaa`, offset by the value in `oooo`     | `sw t0 fp t3` |

### LOAD IMMEDIATE

The "load immediate" instruction starts with the two bits `10`, followed by two bits that specify one of the temporary
registers.  After this come 12 bits that specify an unsigned value to be loaded into the temp register specified.  If you
want to load an immediate value into a non-temp register you must load it first into a temp register and then move it to
another register.

| Instruction | Form                  | Description                                             | Example     |
|-------------|-----------------------|---------------------------------------------------------|-------------|
| `ldi`       | `10rr vvvv vvvv vvvv` | Puts the value `vvvv vvvv vvvv` into temp register `rr` | `ldi t0 22` |

### JUMPS

The MTMC supports four jump commands, which all start with the first two bits `11`.  The next two bits specify the type
of jump, followed by 12-bits that specify the address to jump to.

All conditional jumps are based on the value in `t0`.

The Jump & Link (`jal`) instruction is used to implement function calls.  It sets
the program counter to the address encoded in the lower three bytes of the instruction, while setting the `ra`
register to the address of the instruction after itself.

| Instruction | Form                  | Description                                                                                       | Example                                    |
|-------------|-----------------------|---------------------------------------------------------------------------------------------------|--------------------------------------------|
| `j`         | `1100 vvvv vvvv vvvv` | Jumps unconditionally to the location `vvvv vvvv vvvv`                                            | `jump loop`                                |
| `jz`        | `1101 vvvv vvvv vvvv` | Jumps to the location `vvvv vvvv vvvv` if `t0` is 0                                               | `jz end`                                   |
| `jnz`       | `1110 vvvv vvvv vvvv` | Jumps to the location `vvvv vvvv vvvv` if `t0` not 0                                              | `jnz end`                                  |
| `jal`       | `1111 vvvv vvvv vvvv` | Sets `ra` to the address of the next instruction (`pc` + 1) and sets the `pc` to `vvvv vvvv vvvv` | `jal square` (jump to function `square()`) |



## MTMC Calling Conventions

* t0-ra:     caller saved
* fp, sp:    callee saved
* parameters passed in `a0`-`a3`, additional parameters on stack, last parameter lowest
* `ra` should be saved on the stack below any other values but above any additional parameters
* return value placed in `rv`

## MTMC IO

The MTMC has two forms of input/output: console and interactive

Console I/O is done via the text console attached to the machine.  String and integer values can be read and written
using system calls.

Interactive I/O is done via the screen, with the input event left in the user-invisible `io` register.  This register
can be accessed via the `io` syscall.  This call can either be blocking or non-blocking.  If the `a0` register is set to
`1`, the call will block, otherwise it will not block.

Calling the `io` syscall returns the current value of `io` in `rv` and clears `io`.

The value in `io` is a 16-bit value split in the following manner:

`xxxx xxyy yyyy eeee`

The first six bits indicate the x position of the event, if any.

The next six bits indicate the y position of the event, if any.

The final nibble of the value indicate what event occurred, with the following values:

| Name       | Hex | binary | Description                                  |
|------------|-----|--------|----------------------------------------------|
| `none`     | `0` | `0000` | no event has occurred since the last read    |
| `up`       | `1` | `0001` | the up arrow key was pressed                 |
| `right`    | `2` | `0010` | the right arrow key was pressed              |
| `down`     | `3` | `0011` | the down arrow key was pressed               |
| `left`     | `4` | `0100` | the left arrow key was pressed               |
| `space`    | `5` | `0101` | the space bar was pressed                    |
| `a`        | `6` | `0110` | the a key was pressed                        |
| `s`        | `7` | `0111` | the s key was pressed                        |
| `d`        | `8` | `1000` | the d key was pressed                        |
| `f`        | `9` | `1001` | the f key was pressed                        |
| `esc`      | `A` | `1010` | the escape key was pressed                   |
| `down`     | `B` | `1011` | the mouse was pressed down (but not clicked) |
| `up`       | `C` | `1100` | the mouse was released                       |
| `move`     | `D` | `1101` | the mouse was moved                          |
| `click`    | `E` | `1110` | the mouse was clicked                        |
| `dblclick` | `F` | `1111` | the mouse was double clicked                 |

#### System Codes

Here are the syscodes supported by MTOS - WORK IN PROGRESS

| Syscall   | Hex  | Description                                                                                                                                           |
|-----------|------|:------------------------------------------------------------------------------------------------------------------------------------------------------|
| `rint`    | 0x00 | Reads an int value from the console into `rv`                                                                                                         |
| `wint`    | 0x01 | Writes the int value in `a0` to the console                                                                                                           |
| `rstr`    | 0x02 | Reads a string into the memory location pointed to by `a0` of max length `a1` from the console.  The bytes read are left in `rv`.                     |
| `wstr`    | 0x03 | Writes a null terminated string to the console from the memory location pointed to by `a0`                                                            |
| `rfile`   | 0x04 | Reads a file into the memory location pointed to by `a0` of max length `a1` from the file whose name is in `a2`.  The bytes read are left in `rv`.    |
| `wfile`   | 0x05 | Writes the bytes from the memory location pointed to by `a0` of length `a1` into teh file whose name is in `a2`.                                      |
| `rnd`     | 0x06 | Puts a random number between `a0` and `a1` (inclusive) into `rv`                                                                                      |
| `sleep`   | 0x07 | Sleeps the system for the number of milliseconds found in `a0`                                                                                        |
| `fbreset` | 0x11 | Resets the frame buffer to all zeros                                                                                                                  |
| `fbstat`  | 0x12 | Sets `rv` to the 2-bit value of the pixel location `a0`, `a1` (out of bounds pixels will always be 0)                                                 |
| `fbset`   | 0x13 | Sets the 2-bit value of the pixel location `a0`, `a1`, to the value found in `a3` (values may be 0, 1, 2 or 3, all other values will be treated as 0) |
| `fbss`    | 0x14 | Draws the sprite found at sprite index `a0` into the framebuffer at location `a1`, `a2`.  If `a3` is `1`, transparency will be respected.             |
| `fbds`    | 0x14 | Draws the sprite found at sprite index `a0` into the framebuffer at location `a1`, `a2`.  If `a3` is `1`, transparency will be respected.             |
| `fbsync`  | 0x14 | Synchronizes the frame buffer to the screen                                                                                                           |
| `error`   | 0xF0 | Aborts the current program execution with an error message, `a0` is a pointer to the error message                                                    |
| `halt`    | 0xFF | Shuts the computer down                                                                                                                               |