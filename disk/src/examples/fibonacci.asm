; Fibonacci Sequence
; Print first 10 fibonacci numbers
; Uses SI and DI to avoid push/pop

MOV SI, 0      ; fib(n-1)
MOV DI, 1      ; fib(n)
MOV CX, 0      ; counter

fib_loop:
; Print current fibonacci number
MOV AX, SI
SYSCALL PRINT_INT
MOV AX, 10
SYSCALL PRINT_CHAR

; Calculate next: fib(n+1) = fib(n) + fib(n-1)
MOV AX, SI
ADD AX, DI     ; AX = fib(n-1) + fib(n)
MOV SI, DI     ; fib(n-1) = old fib(n)
MOV DI, AX     ; fib(n) = new value

; Increment counter
INC CX
CMP CX, 10
JL fib_loop

HALT