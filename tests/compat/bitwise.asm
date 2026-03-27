; Cross-Platform Test 07: Bitwise Operations
; Tests AND, OR, XOR, NOT, SHL, SHR, TEST
; Expected output (decimal): 8, 15, 7, 65530, 32, 4, 255, 240

; Test 1: AND (12 & 10 = 8)
MOV AX, 12    ; 1100
MOV BX, 10    ; 1010
AND AX, BX    ; 1000 = 8
SYSCALL PRINT_INT
MOV AX, 10
SYSCALL PRINT_CHAR

; Test 2: OR (12 | 3 = 15)
MOV AX, 12    ; 1100
MOV BX, 3     ; 0011
OR AX, BX     ; 1111 = 15
SYSCALL PRINT_INT
MOV AX, 10
SYSCALL PRINT_CHAR

; Test 3: XOR (12 ^ 11 = 7)
MOV AX, 12    ; 1100
MOV BX, 11    ; 1011
XOR AX, BX    ; 0111 = 7
SYSCALL PRINT_INT
MOV AX, 10
SYSCALL PRINT_CHAR

; Test 4: NOT (~5 = -6 in signed 16-bit)
MOV AX, 5
NOT AX
SYSCALL PRINT_INT
MOV AX, 10
SYSCALL PRINT_CHAR

; Test 5: SHL (8 << 2 = 32)
MOV AX, 8
SHL AX, 2
SYSCALL PRINT_INT
MOV AX, 10
SYSCALL PRINT_CHAR

; Test 6: SHR (16 >> 2 = 4)
MOV AX, 16
SHR AX, 2
SYSCALL PRINT_INT
MOV AX, 10
SYSCALL PRINT_CHAR

; Test 7: TEST does not modify register (0xFF & 0x0F, AX stays 0xFF)
MOV AX, 0xFF
TEST AX, 0x0F
SYSCALL PRINT_INT
MOV AX, 10
SYSCALL PRINT_CHAR

; Test 8: TEST sets ZF correctly (0xF0 & 0x0F = 0, so JZ taken)
MOV AX, 0xF0
TEST AX, 0x0F
JZ test8_zero
MOV AX, 999     ; should not reach here
JMP test8_done
test8_zero:
MOV AX, 0xF0    ; AX still 0xF0, print it to confirm unchanged
test8_done:
SYSCALL PRINT_INT
MOV AX, 10
SYSCALL PRINT_CHAR

SYSCALL EXIT
