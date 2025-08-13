
## Frequently Asked Questions

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
of the *Arithmetic Logic Unit* (ALU) in the CPU. The ALU is the part of the 
CPU that does math like addition, subtraction, and boolean operations. 

### What is assembly language?

Computer CPUs "decode" binary instructions to activate different parts of the CPU
like the *Arithmetic Logic Unit* (ALU) and memory *Control Unit* (CU) for instruction
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
accomplish the same work. While a CISC design would have been more representative
of early processors, they're harder to understand and not representative of 
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
