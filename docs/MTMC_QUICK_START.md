# MTMC - Quick Start Guide

This guide will walk you through getting started with the MTMC-16 simualted computer,
going through installation, setup, and your first programs.

## Installation

1. You will need a Java 21 virtual machine. If you don't have one or are unsure, visit 
[adoptium.net](https://adoptium.net) to download a VM. The Eclipse Temurin VM is an
implementation of OpenJDK and supports Java 21.

1. Download a copy of `mtmc.jar` from [https://mtmc.cs.montana.edu]. Save it to a place
you can easily find it.

1. In most cases simply double-clicking on the `mtmc.jar` file will launch the server
and make the machine ready for use. However, you can also run the system from the 
command-line by running `java -jar mtmc.jar`.

1. Once `mtmc.jar` is running, open a web browser to [http://localhost:8081]. Note that
a modern web browser like Chrome or Safari is required. 

1. You should see the following interface and be ready to use the MTMC-16!


<img width="1955" height="1137" alt="MTMC Components" src="https://github.com/user-attachments/assets/60247b4a-c0c4-4fa7-b22c-40359b52d645" />

## Using the MTMC-16

The display contains the following components:

- **File Explorer** - This is where you can view the file system and edit your programs. Clicking on a file will bring up the code editor.
- **Command Line Console** - Here is where you can compile programs, start programs, interact with text prompts, and use system commands to adjust the computer's settings. The interface is similar to the Linux command line.
- **Memory Viewer** - The MTMC-16 has 4 kilobytes of RAM. You can see the status of each byte or word in this window. By default, the viewer shows a dynamic rendering of memory that decodes instructions and strings. Clicking the `dyn` button will switch between dynamic, hexidecimal (byte), decimal (word), instruction, and string views.
- **CPU State** - Shows the state of CPU registers including the Stack Pointer and Program Counter. The currently decoded instruction is shown in the `ir` register while `flags` shows `test` and `error` states of the program. Registers are shown in both binary form and decimal form. Hovering over a register value will highlight that location in the memory viewer.
- **Execution Controls** - The dropdown allows you to slow down and speed up the computer. The buttons are context sensitive and will provide a `run` option when a program is not executing, a `pause` option when the computer is executing, `step` and `back` buttons for stepping through program execution, and a `reset` button to return the computer to its default state.
- **4 color display** - Games and other programs needing a graphical display will render their content into this window. There is an on-screen gamepad that can be used to control games, however the keyboard and an external gamepad are both supported as well.
