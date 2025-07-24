# MTMC - MonTana state Mini Computer

The MonTana state Mini Computer is a virtual computer intended to show how digital computation works in a fun and visual way.

The MTSC combines ideas from the [PDP-11](https://en.wikipedia.org/wiki/PDP-11), [MIPS](https://en.wikipedia.org/wiki/MIPS_architecture),
 [Scott CPU](https://www.youtube.com/watch?v=RRg5hRlywIg), [Game Boy](https://en.wikipedia.org/wiki/Game_Boy) and
 [JVM](https://en.wikipedia.org/wiki/Java_virtual_machine) to make a relatively simple 16-bit computer that can accomplish basic computing tasks.

The computer is displayed via a web interface that includes all the I/O such as console and display, visual representations of the computer state, and a built in code editor to construct and debug software for the computer.  

 <img width="1347" height="708" alt="Screenshot 2025-07-14 at 2 37 33 PM" src="https://github.com/user-attachments/assets/2e41fdeb-ee90-4137-a4b8-7c04568ddac0" />


## Overall Architecture

- 16-bit binary computer
- byte-addressable
- 2 byte (16-bit) words
- 4k of Memory
    - 4096 bytes/addresses
    - 2048 words
- 16 Registers (see below)
- 160x144 2-bit green scale display
    - `00` - `#2a453b` - darkest
    - `01` - `#365d48` - dark
    - `10` - `#577c44` - light
    - `11` - `#7f860f` - lightest
- Console for text input/output & commands
- Operating System (MTOS)
- Core data types are signed 16-bit integers & bytes

## Documentation

- [Quick Start Guide](docs/MTMC_QUICK_START.md)
- [Computer Specification](docs/MTMC_SPECIFICATION.md)
- [Assembly Guide](docs/MTMC_ASSEMBLY.md)
- [Frequently Asked Questions](docs/MTMC_FAQ.md)
