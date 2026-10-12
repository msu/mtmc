  MOV AX, 2
  MOV BX, 10
  CALL pow
  SYSCALL PRINT_INT   ; 1024
  SYSCALL EXIT

  pow:                ; AX = AX to the BX
    MOV CX, AX        ; CX = b
    MOV AX, 1         ; b to the 0 is 1
  next:
    CMP BX, 0         ; e times?
    JE  done
    MUL CX            ; AX = AX * b
    DEC BX
    JMP next
  done:
    RET