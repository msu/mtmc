# MTMC - Quick Start Guide

This guide will walk you through getting started with the MTMC-16 simulated computer,
going through installation, setup, and your first programs.

## Installation

1. You will need a Java 21 virtual machine. If you don't have one or are unsure, visit 
[adoptium.net](https://adoptium.net) to download a VM. The Eclipse Temurin VM is an
implementation of OpenJDK and supports Java 21.

1. Download a copy of `mtmc.jar` from [https://mtmc.cs.montana.edu](https://mtmc.cs.montana.edu). Save it to a place
you can easily find it.

1. In most cases simply double-clicking on the `mtmc.jar` file will launch the server
and make the machine ready for use. However, you can also run the system from the 
command-line by running `java -jar mtmc.jar`.

1. Once `mtmc.jar` is running, open a web browser to [http://localhost:8080](http://localhost:8080). Note that
a modern web browser like Chrome or Safari is required. 

1. You should see the following interface and be ready to use the MTMC-16!

**NOTE:** If port `8080` is already in use, the system will increment the port number until it finds a free port. In that case, try [http://localhost:8081](http://localhost:8081) or look at the command line console to see what the correct URL is.


<img width="1955" height="1137" alt="MTMC Components" src="https://github.com/user-attachments/assets/60247b4a-c0c4-4fa7-b22c-40359b52d645" />

## Using the MTMC-16

The display contains the following components:

- **File Explorer** - This is where you can view the file system and edit your programs. Clicking on a file will bring up the code editor.
- **Command Line Console** - Here is where you can compile programs, start programs, interact with text prompts, and use system commands to adjust the computer's settings. The interface is similar to the Linux command line.
- **Memory Viewer** - The MTMC-16 has 4 kilobytes of RAM. You can see the status of each byte or word in this window. By default, the viewer shows a dynamic rendering of memory that decodes instructions and strings. Clicking the `dyn` button will switch between dynamic, hexidecimal (byte), decimal (word), instruction, and string views.
- **CPU State** - Shows the state of CPU registers including the Stack Pointer and Program Counter. The currently decoded instruction is shown in the `ir` register while `flags` shows `test` and `error` states of the program. Registers are shown in both binary form and decimal form. Hovering over a register value will highlight that location in the memory viewer.
- **Execution Controls** - The dropdown allows you to slow down and speed up the computer. The buttons are context sensitive and will provide a `run` option when a program is not executing, a `pause` option when the computer is executing, `step` and `back` buttons for stepping through program execution, and a `reset` button to return the computer to its default state.
- **4 color display** - Games and other programs needing a graphical display will render their content into this window. There is an on-screen gamepad that can be used to control games, however the keyboard and an external gamepad are both supported as well.

## Command Line Console

The command-line console provides a simplified, Unix-like interface. Typing `?` will provide a list of built-in tools such as the assembler (`asm`) and `load` commands. Programs located in the `/bin` directory or in the current directory can be executed by typing their name. (e.g. `snake` to run the snake game).

A set of Unix-like utilities are provided in the `/bin` directory, including:

- `echo` - Echoes the text passed to the program back to the command line
- `cd` - Changes the current directory to the one passed to the command
- `ls` - Lists the contents of the current directory. You can pass in a path to list another directory.'
- `pwd` - Print working directory. Tells you what directory you're in.
- `rm` - Removes the file passed to the program. Note that directories must be empty to be deleted.

The command line console also allows Assembly Instructions to be typed in for immediate execution. For example, typing `li t0 42` will load `42` into the the `t0` register.

## Adding Two Numbers

We can control the CPU from the command line to load two numbers and add them together. Type the following commands to try adding `42` and `47`:

```
li t0 42
li t1 47
add t0 t1
```

If you check the CPU state, you should find `89` in the `t0` register. 

We can print this number out by moving the value to the `a0` register and making the `wint` syscall.

```
mov a0 t0
sys wint
```

<img width="1806" height="1272" alt="image" src="https://github.com/user-attachments/assets/08a698f2-22c7-4447-b578-d84e9002c6be" />

## Hello World Program

We can very easily create a program in Assembly that prints out `Hello World!` when run. 

First we need to create a blank Assembly file.

1. Click the `+ New File` icon in the File Explorer
2. Make sure "Assembly Language" is selected and enter the filename of "hello".
3. Click `Save` to create the file

Next, copy/paste the following code into the editor:

```
.data
  hello: "Hello world!\n"

.text
  li   a0 hello
  sys  wstr

  sys  exit
```

Click the Save floppy icon in the top-right to save your program. Or use the Ctrl+S (Command+S on macOS) keyboard shortcut. The floppy icon should go dark.

Now type the following command in the Command Line Console to assemble your program into an executable:

```
asm hello.asm hello
```

Then type `hello` in the Command Line Console to run your program. 

Congratulations! You've written your first assembly program. 

## Using the Step Debugger

Make sure your `hello.asm` program is open in the editor. 

Go to the Command Line Console and type `load hello`. This will load the program into memory and prepare for execution, but not start the execution of the program. 

Note the line with the red highlight in the editor. This is the line the computer is about to execute.

Click the `Step` button in the Execution Controls to see the effect of each line of code on the computer's state. If you're unsure what an instruction did, click `Back` to reverse the last instruction and examine the state changes again.
