; Factorial Example
; Calculate 5!
; Uses SI to avoid push/pop

MOV AX, 5      ; n = 5
CALL factorial
; Result in AX
SYSCALL PRINT_INT
MOV AX, 10
SYSCALL PRINT_CHAR
HALT

factorial:
; Calculate factorial of AX
; Result in AX
; Uses SI for counter (no push/pop needed)

MOV SI, AX     ; SI = n (counter)
MOV AX, 1      ; result = 1

fact_loop:
CMP SI, 1
JLE fact_done
MUL SI         ; result *= n
DEC SI
JMP fact_loop

fact_done:
RET