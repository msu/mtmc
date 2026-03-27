
prompt: DB "Enter number: "

main:
    MOV AX, prompt
    SYSCALL PRINT_STRING
    SYSCALL READ_INT
    MOV BX, AX           ; BX = num (the number we want the square root of)
    MOV CX, AX           ; CX = x (our current guess, starts at num)

loop:
    MOV AX, BX           ; AX = num
    DIV CX               ; AX = num / x (integer division)
    ADD AX, CX           ; AX = x + num/x
    SHR AX, 1            ; AX = (x + num/x) / 2  (this is "next", the improved guess)

    CMP AX, CX           ; is next >= x?
    JGE done             ; if so, we've converged - stop

    MOV CX, AX           ; x = next (use the improved guess)
    JMP loop             ; try again

done:
    MOV AX, CX           ; move result to AX for printing
    SYSCALL PRINT_INT
    SYSCALL EXIT
