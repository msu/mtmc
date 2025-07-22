.data:
    NEW_LINE: 10
    SPACE: 32
    NULL: 0
    sp_address: NA
.text
main:
    sw t5 a0
    main_loop:
        la a0 t5 # Loads current address (Each iteration)
        lbo t0 a0 0 #Loads byte into t0(Starts with first char address) 
        neqi t0 SPACE
        jz increment # If test-bit is 0 (value is a SPACE), increment
        neqi t0 NULL # If null, end.
        jz end

        jal parse_and_push
        jal increment
        jal main_loop
end:
    sort:
        TODO
    print:
        # New line - Print Value - Decrement - Need to add null terminator
        li a0 NEW_LINE
        sys wchr
        li a0 sp_address
        sys wint
        dec sp_address
        jal print
    sys exit

parse_and_push:
    #la a0 t5 # Loads current register address(t5) into a0
    sys atoi # Determines if value in register is a short, stores value into rv,else returns 0
    push rv # Pushes onto stack
    ret

increment:
    inc t5
    jal main_loop

#Have a counter that iterates through and prints list with "\n"