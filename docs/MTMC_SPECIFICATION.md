# MTMC - MonTana state Mini Computer

The MonTana state Mini Computer is a virtual computer intended to show how digital computation works in a fun and visual
way.

The MTSC combines ideas from
the [PDP-11](https://en.wikipedia.org/wiki/PDP-11), 
[MIPS](https://en.wikipedia.org/wiki/MIPS_architecture),
the [Scott CPU](https://www.youtube.com/watch?v=RRg5hRlywIg), 
the [Game Boy](https://en.wikipedia.org/wiki/Game_Boy) and
the [JVM](https://en.wikipedia.org/wiki/Java_virtual_machine) 
to make a relatively simple 16-bit computer that can
accomplish basic computing tasks.

## Overall Architecture

- 16-bit binary computer
- byte-addressable
- 2 byte (16-bit) words
- 4k of Memory 
  - 4096 bytes/addresses
  - 2048 words
- 16 Registers (see below)
- 160x140 2-bit green scale display
  - `00` - `#2a453b` - darkest
  - `01` - `#365d48` - dark
  - `10` - `#577c44` - light
  - `11` - `#7f860f` - lightest
- Console for text input/output & commands
- Operating System (MTOS)
- Core data types are signed 16-bit integers & bytes

## Registers

The MTMC has a total of 16 user-facing register.  They are outlined below.

| index | name | description                                                                      |
|-------|------|----------------------------------------------------------------------------------|
| 0     | `t0` | temp register 0 (aka "the accumulator") holds temporary values, tested for jumps |
| 1     | `t1` | temp register 1, holds temporary values                                          |
| 2     | `t2` | temp register 2, holds temporary values                                          |
| 3     | `t3` | temp register 3, holds temporary values                                          |
| 4     | `t4` | temp register 4, holds temporary values                                          |
| 5     | `t5` | temp register 5, holds temporary values                                          |
| 6     | `a0` | arg register 0, holds the first argument for a function call                     |
| 7     | `a1` | arg register 1, holds the second argument for a function call                    |
| 8     | `a2` | arg register 2, holds the third argument for a function call                     |
| 9     | `a3` | arg register 3, holds the fourth argument for a function call                    |
| 10    | `rv` | return value register, holds the return value for a function call                |
| 11    | `ra` | return address register, holds the return address for a function call            |
| 12    | `fp` | frame pointer, points to the top of the current function frame                   |
| 13    | `sp` | stack pointer, points to the bottom of the current function frame                |
| 14    | `bp` | break pointer, points to the top of the current heap space                       |
| 15    | `pc` | program counter, points to the next instruction to execute                       |

In addition to these registers, there are the following non-user facing registers:

| name    | description                                                            |
|---------|------------------------------------------------------------------------|
| `ir`    | holds the current instruction to execute                               |
| `dr`    | holds data associated with the current instruction if it is multi-word |
| `cb`    | a pointer to the boundary of the code segment                          |
| `db`    | a pointer to the boundary of the data segment                          |
| `flags` | holds flag values                                                      |

### Flags Register

The `flags` register is four bits:

`<test bit> <overflow bit> <nan bit> <error bit>`

* `test bit` - set by a TEST instruction
* `overflow bit` - set if a mathematical operation overflows
* `nan bit` - set if a mathematical operation results in NaN
* `error bit` - set if a system error occurs

## Instructions (16 bit)

There are eight instruction types in the MTSC.  

Instructions can be either one or two words long.

Instruction types can be determined by looking at the four high-order bits (nibble) of an instruction.

| top nibble | hex   | type                |
|------------|-------|---------------------|
| `0000`     | `0`   | MISC                |
| `0001`     | `1`   | ALU                 |
| `0010`     | `2`   | STACK               |
| `0011`     | `3`   | TEST                |
| `01xx`     | `4-7` | LOAD/STORE REGISTER |
| `1000`     | `8`   | LOAD/STORE          |
| `1001`     | `9`   | JUMP REGISTER       |
| `11xx`     | `C-F` | JUMP                |

### MISC

Misc (miscellaneous) instructions start with the nibble `0000`.

| Instruction | Form                  | Description                                                 | Example     |
|-------------|-----------------------|-------------------------------------------------------------|-------------|
| `sys`       | `0000 0000 vvvv vvvv` | Issues syscall `vvvv vvvv`                                  | `sys wstr`  |
| `mov`       | `0000 0001 rrrr ssss` | Moves the value in register `ssss` to `rrrr`                | `mov a0 t0` |
| `inc`       | `0000 0010 rrrr vvvv` | Increments the value in register `rrrr` by the value `vvvv` | `inc t0`    |
| `dec`       | `0000 0011 rrrr vvvv` | Decrements the value in register `rrrr` by the value `vvvv` | `dec t0 2`  |
| `seti`      | `0000 0100 rrrr vvvv` | Sets the value in register `rrrr` to the value `vvvv`       | `seti t0 2` |
| `nop`       | `0000 1111 1111 1111` | A no-op instruction                                         | `nop`       |

### ALU 
 
ALU operations start with the nibble `0001` and come in two forms:

- Binary operations
- Unary operations

Binary operations take the form `0001 oooo rrrr ssss`, where `oooo` is the operation, `rrrr` is the first register and
`ssss` is the second register.  Any registers can be referenced with ALU operations.  

Logically, binary operations end up looking like this:

```
rrrr = rrrr OP ssss
```

Unary operations take the form `0001 oooo 0000 rrrr`, where `oooo` is the operation, `rrrr` is the register to modify.

There is one special instruction, the `imm` ALU instruction.  This instruction takes the ALU instruction found in the
third nibble and applies it to the register in the fourth nibble and the word in memory immediately following the
instruction.

If the result of an ALU operation is zero the `test` bit of the `flag` register will be set to `0`, otherwise it will
be set to `1`

| Instruction | Form                   | Description                                                                                                         | Example     |
|-------------|------------------------|---------------------------------------------------------------------------------------------------------------------|-------------|
| `add`       | `0001 0000 rrrr ssss`  | Adds the value of `rrrr` to `ssss` and saves it to `rrrr`                                                           | `add t0 t1` |
| `sub`       | `0001 0001 rrrr ssss`  | Subtracts the value of `rrrr` to `ssss` and saves it to `rrrr`                                                      | `sub t0 t1` |
| `mul`       | `0001 0010 rrrr ssss`  | Multiplies the value of `rrrr` to `ssss` and saves it to `rrrr`                                                     | `mul t0 t1` |
| `div`       | `0001 0011 rrrr ssss`  | Divides the value of `rrrr` by `ssss` and saves it to `rrrr`                                                        | `div t0 t1` |
| `mod`       | `0001 0100 rrrr ssss`  | Computes the mod the value of `rrrr` by `ssss` and saves it to `rrrr`                                               | `mod t0 t1` |
| `and`       | `0001 0101 rrrr ssss`  | Bitwise ANDs the value of `rrrr` by `ssss` and saves it to `rrrr`                                                   | `and t0 t1` |
| `or`        | `0001 0110 rrrr ssss`  | Bitwise ORs the value of `rrrr` by `ssss` and saves it to `rrrr`                                                    | `or t0 t1`  |
| `xor`       | `0001 0111 rrrr ssss`  | Bitwise XORs the value of `rrrr` by `ssss` and saves it to `rrrr`                                                   | `xor t0 t1` |
| `shl`       | `0001 1000 rrrr ssss`  | Shifts the value in `rrrr` the unsigned value `vvvv` bits to the left and saves it to `rrrr`                        | `shl t0 t1` |
| `shr`       | `0001 1001 rrrr ssss`  | Shifts the value in `rrrr` the unsigned value `vvvv` bits to the right and saves it to `rrrr`                       | `shr t0 t1` |
| `min`       | `0001 1010 rrrr ssss`  | Leaves the minimum of the values in `rrrr` and `ssss` in `rrrr`                                                     | `min t0 t1` |
| `max`       | `0001 1011 rrrr ssss`  | Leaves the minimum of the values in `rrrr` and `ssss` in `rrrr`                                                     | `max t0 t1` |
| `not`       | `0001 1100 rrrr 0000`  | Bitwise NOTs the value of `rrrr` and saves it to `rrrr`                                                             | `not t0`    |
| `lnot`      | `0001 1101 rrrr 0000`  | Logical NOT: if the the value of `rrrr` is `0` set `rrrr` value to `1`, `0` otherwise                               | `lnot t0`   |
| `neg`       | `0001 1110 rrrr 0000 ` | Negates the value of `rrrr` and saves it to `rrrr`                                                                  | `neg t0`    |
| `imm`       | `0001 1111 rrrr aaaa ` | Applies the ALU operation found in `aaaa` and applies it to `rrr` and the next word in memory after the instruction | `neg t0`    |

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
| `min`     | `A` | `1010` |
| `max`     | `B` | `1011` |
| `not`     | `C` | `1100` |
| `lnot`    | `D` | `1101` |
| `neg`     | `E` | `1110` |
| `imm`     | `F` | `1111` |

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
* pushi - push an immediate value

In the case of ALU stack operations, if the ALU operator is a binary operation, the top two values of the stack are 
consumed and the result is pushed back onto the stack.  

If the operator is unary operation, the top value of the stack 
is consumed and the result is pushed back onto the stack.

Note that stacks always grow _down_ in memory on the MTMC.

#### Assembly Notes

Stack assembly instructions do not require you specify a stack pointer register.  

If it is omitted then the assembler will assume you want to use the `sp` register.

| Instruction | Form                  | Description                                                                                                                                           | Example                                    |
|-------------|-----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------|
| `push`      | `0010 0000 rrrr ssss` | Pushes the word value of `rrrr` onto the stack pointed at by `ssss`, `ssss` is decrement by 2 bytes and the value in `rrrr` is saved to that location | `push t0` (`sp` is implied)                |
| `pop`       | `0010 0001 rrrr ssss` | Pops the word at the top of the stack pointed at by `ssss` into `rrrr`, `ssss` is incremented by 2 bytes.                                             | `pop ra, t4` (explicit stack pointer `t4`) |
| `dup`       | `0010 0010 0000 ssss` | Duplicates the word at the top of the stack pointed to by `ssss`.                                                                                     | `dup`                                      |
| `swap`      | `0010 0011 0000 ssss` | Swaps the two words at the top of the stack pointed to by `ssss`.                                                                                     | `swap`                                     |
| `drop`      | `0010 0100 0000 ssss` | Drops the top word of the stack pointed to by `ssss`.                                                                                                 | `drop`                                     |
| `over`      | `0010 0101 0000 ssss` | Copies the second word to the top of the stack pointed to by `ssss`.                                                                                  | `over`                                     |
| `rot`       | `0010 0110 0000 ssss` | Rotates the third word to the top of the stack pointed to by `ssss`.                                                                                  | `rot`                                      |
| `sop`       | `0010 0111 aaaa ssss` | Applies the ALU operation `aaaa` to the stack pointed at by the `ssss` register.                                                                      | `sop add`                                  |
| `pushi`     | `0010 1111 0000 ssss` | Pushes the value in the memory location after the instruction onto the top of the stack pointed to by `ssss`.                                         | `pushi 1024`                               |

### TEST

Test instructions start with the nibble `0011`.

These instructions all set the `test bit` of the `flags` register with their result, which can then be used with the `jz`
(jump if the test bit is zero) instruction for conditionals.

| Instruction | Form                  | Description                                                                                               | Example     |
|-------------|-----------------------|-----------------------------------------------------------------------------------------------------------|-------------|
| `eq`        | `0011 0000 rrrr ssss` | Sets `test bit` to `1` if the values in `rrrr` and `ssss` are equal, `0` otherwise                        | `eq t0 t1`  |
| `neq`       | `0011 0001 rrrr ssss` | Sets `test bit` to `1` if the values in `rrrr` and `ssss` are not equal, `0` otherwise                    | `neq t0 t1` |
| `gt`        | `0011 0010 rrrr ssss` | Sets `test bit` to `1` if the value in `rrrr` is greater than `ssss`, `0` otherwise                       | `gt t0 t1`  |
| `gte`       | `0011 0011 rrrr ssss` | Sets `test bit` to `1` if the value in `rrrr` is greater than or equal to `ssss`, `0` otherwise           | `gte t0 t1` |
| `lt`        | `0011 0100 rrrr ssss` | Sets `test bit` to `1` if the value in `rrrr` is less than `ssss`, `0` otherwise                          | `lt t0 t1`  |
| `lte`       | `0011 0101 rrrr ssss` | Sets `test bit` to `1` if the value in `rrrr` is less than or equal to `ssss`, `0` otherwise              | `lte t0 t1` |
| `eqi`       | `0011 1000 rrrr vvvv` | Sets `test bit` to `1` if the value in `rrrr` is equal to the value `vvvv`, `0` otherwise                 | `eq0`       |
| `neqi`      | `0011 1001 rrrr vvvv` | Sets `test bit` to `1` if the value in `rrrr` is not equal to the value `vvvv`, `0` otherwise             | `eq1 t1`    |
| `gti`       | `0011 1010 rrrr vvvv` | Sets `test bit` to `1` if the value in `rrrr` is greater than the value `vvvv`, `0` otherwise             | `gt0 t1`    |
| `gtei`      | `0011 1011 rrrr vvvv` | Sets `test bit` to `1` if the value in `rrrr` is greater than or equal to the value `vvvv`, `0` otherwise | `gte0 t1`   |
| `lti`       | `0011 1100 rrrr vvvv` | Sets `test bit` to `1` if the value in `rrrr` is less than the value `vvvv`, `0` otherwise                | `lt0 t1`    |
| `ltei`      | `0011 1101 rrrr vvvv` | Sets `test bit` to `1` if the value in `rrrr` is less than or equal to the value `vvvv`, `0` otherwise    | `lte0 t1`   |

### LOAD/STORE REGISTER

The MTMC allows you to load and store words and bytes in memory with "register" load & store instructions, that is
relative to the values in registers.

These instructions start with the two bits `01`.

The next two bits then specify which type of load/store instruction it is.

The next nibble specifies a register holding the address to save to or read from.

The next nibble specifies the register holding the address of the memory location to read from or write to.

The final nibble specifies an offset register, which holds a value to offset the address register by.

| Instruction | Form                  | Description                                                                                        | Example        |
|-------------|-----------------------|----------------------------------------------------------------------------------------------------|----------------|
| `lwr`       | `0100 rrrr aaaa oooo` | Loads the word (16-bit) value at the address in `aaaa`, offset by the value in `oooo`, into `rrrr` | `lwr t0 fp t3` |
| `lbr`       | `0101 rrrr aaaa oooo` | Loads the byte (8-bit) value at the address in `aaaa`, offset by the value in `oooo`, into `rrrr`  | `lbr t0 fp t3` |
| `swr`       | `0110 rrrr aaaa oooo` | Saves the word (16-bit) value in `rrrr` to the address in `aaaa`, offset by the value in `oooo`    | `swr t0 fp t3` |
| `sbr`       | `0111 rrrr aaaa oooo` | Saves the byte (8-bit) value in `rrrr` to the address in `aaaa`, offset by the value in `oooo`     | `swr t0 fp t3` |

### LOAD/STORE

The MTMC allows you to load and store words and bytes in memory with "load" and "store" instructions.  

These instructions start with the nibble `1000`. 

The next nibble specify which type of load/store instruction it is.  

The next nibble specifies a register holding the address to save to or read from.

If applicable, the final nibble specifies an offset register, which holds a value to offset the address register by.

Assembly programmers may omit the offset:

```asm
  lw t0 BUFFER # zero offset
```

The address or value to load or store from is found in the word immediately after the instruction.  

| Instruction | Form                  | Description                                                                                                                      | Example                  |
|-------------|-----------------------|----------------------------------------------------------------------------------------------------------------------------------|--------------------------|
| `lw`        | `1000 0000 rrrr 0000` | Loads the word (16-bit) value at the address found immediately after the instruction into `rrrr`                                 | `lw t0 GLOBAL_VAR_1`     |
| `lwo`       | `1000 0001 rrrr oooo` | Loads the word (16-bit) value at the address found immediately after the instruction, offset by the value in `oooo`, into `rrrr` | `lwo t0 GLOBAL_VAR_1 t1` |
| `lb`        | `1000 0010 rrrr 0000` | Loads the word (8-bit) value at the address found immediately after the instruction into `rrrr`                                  | `lb t0 GLOBAL_VAR_1`     |
| `lbo`       | `1000 0011 rrrr oooo` | Loads the word (8-bit) value at the address found immediately after the instruction, offset by the value in `oooo`, into `rrrr`  | `lbo t0 GLOBAL_VAR_1 t1` |
| `sw`        | `1000 0100 rrrr 0000` | Saves the word (16-bit) value in `rrrr` to the address found immediately after the instruction                                   | `sw t0 GLOBAL_VAR_1`     |
| `swo`       | `1000 0101 rrrr oooo` | Saves the word (16-bit) value in `rrrr` to the address found immediately after the instruction, offset by the value in `oooo`    | `swo t0 GLOBAL_VAR_1 t1` |
| `sb`        | `1000 0110 rrrr 0000` | Saves the byte (8-bit) value in `rrrr` to the address found immediately after the instruction                                    | `sb t0 GLOBAL_VAR_2`     |
| `sbo`       | `1000 0111 rrrr oooo` | Saves the byte (8-bit) value in `rrrr` to the address found immediately after the instruction, offset by the value in `oooo      | `sbo t0 GLOBAL_VAR_2 t1` |
| `li`        | `1000 1111 rrrr 0000` | Loads the word (16-bit) value found immediately after the instruction into `rrrr`                                                | `lwi t0 1024`            |

### JUMP REGISTER

The MTMC supports one jump command that uses a register as the address to jump to, `jr`

| Instruction | Form                   | Description                                                                                       | Example                                    |
|-------------|------------------------|---------------------------------------------------------------------------------------------------|--------------------------------------------|
| `jr`        | `10001 0000 0000 rrrr` | Jumps to the location found in register `rrrr`                                                    | `jr ra` (aliased as `ret`)                 |

### JUMPS

The MTMC supports four absolute jump commands, which all start with the first two bits `11`.  

The next two bits specify the type of jump, followed by 12-bits that specify the address to jump to.

Conditional jumps are based on the `test bit` value in the `flags` register.

The Jump & Link (`jal`) instruction is used to implement function call.  It sets the program counter to the address 
encoded in the lower three bytes of the instruction, while setting the `ra` register to the address of the instruction 
after itself.

| Instruction | Form                  | Description                                                                                       | Example                                    |
|-------------|-----------------------|---------------------------------------------------------------------------------------------------|--------------------------------------------|
| `j`         | `1100 vvvv vvvv vvvv` | Jumps unconditionally to the location `vvvv vvvv vvvv`                                            | `j loop`                                   |
| `jz`        | `1101 vvvv vvvv vvvv` | Jumps to the location `vvvv vvvv vvvv` if `test bit` of `flags` is 0                              | `jz end`                                   |
| `jnz`       | `1110 vvvv vvvv vvvv` | Jumps to the location `vvvv vvvv vvvv` if `test bit` of `flags` is 1                              | `jz end`                                   |
| `jal`       | `1111 vvvv vvvv vvvv` | Sets `ra` to the address of the next instruction (`pc` + 1) and sets the `pc` to `vvvv vvvv vvvv` | `jal square` (jump to function `square()`) |


## MTMC Calling Conventions

* t0-ra:     caller saved
* fp, sp:    callee saved
* parameters passed in `a0`-`a3`, additional parameters on stack, last parameter lowest
* `ra` should be saved on the stack below any other values but above any additional parameters
* return value placed in `rv`

## MTMC IO - WORK IN PROGRESS


| Name       | Hex | binary | Description                                  |
|------------|-----|--------|----------------------------------------------|
| `up`       | `1` | `0001` | the up arrow key was pressed                 |
| `right`    | `2` | `0010` | the right arrow key was pressed              |
| `down`     | `B` | `1011` | the mouse was pressed down (but not clicked) |
| `up`       | `C` | `1100` | the mouse was released                       |
| `down`     | `3` | `0011` | the down arrow key was pressed               |
| `left`     | `4` | `0100` | the left arrow key was pressed               |
| `a`        | `6` | `0110` | the A button was pressed                     |
| `b`        | `7` | `0111` | the B key was pressed                        |
| `start`    | `8` | `1000` | the d key was pressed                        |
| `select`   | `9` | `1001` | the f key was pressed                        |

#### System Codes

Here are the syscodes supported by MTOS - WORK IN PROGRESS

| Syscall   | Hex  | Description                                                                                                                                           |
|-----------|------|:------------------------------------------------------------------------------------------------------------------------------------------------------|
| `halt`    | 0x00 | Shuts the computer down                                                                                                                               |
| `rint`    | 0x01 | Reads an int value from the console into `rv`                                                                                                         |
| `wint`    | 0x02 | Writes the int value in `a0` to the console                                                                                                           |
| `rstr`    | 0x03 | Reads a string into the memory location pointed to by `a0` of max length `a1` from the console.  The bytes read are left in `rv`.                     |
| `wstr`    | 0x04 | Writes a null terminated string to the console from the memory location pointed to by `a0`                                                            |
| `rfile`   | 0x05 | Reads a file into the memory location pointed to by `a0` of max length `a1` from the file whose name is in `a2`.  The bytes read are left in `rv`.    |
| `wfile`   | 0x06 | Writes the bytes from the memory location pointed to by `a0` of length `a1` into teh file whose name is in `a2`.                                      |
| `rnd`     | 0x07 | Puts a random number between `a0` and `a1` (inclusive) into `rv`                                                                                      |
| `sleep`   | 0x08 | Sleeps the system for the number of milliseconds found in `a0`                                                                                        |
| `fbreset` | 0x11 | Resets the frame buffer to all zeros                                                                                                                  |
| `fbstat`  | 0x12 | Sets `rv` to the 2-bit value of the pixel location `a0`, `a1` (out of bounds pixels will always be 0)                                                 |
| `fbset`   | 0x13 | Sets the 2-bit value of the pixel location `a0`, `a1`, to the value found in `a3` (values may be 0, 1, 2 or 3, all other values will be treated as 0) |
| `fbss`    | 0x14 | Draws the sprite found at sprite index `a0` into the framebuffer at location `a1`, `a2`.  If `a3` is `1`, transparency will be respected.             |
| `fbds`    | 0x14 | Draws the sprite found at sprite index `a0` into the framebuffer at location `a1`, `a2`.  If `a3` is `1`, transparency will be respected.             |
| `fbsync`  | 0x14 | Synchronizes the frame buffer to the screen                                                                                                           |
| `error`   | 0xF0 | Aborts the current program execution with an error message, `a0` is a pointer to the error message                                                    |
