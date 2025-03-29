.text
main:
  ldi t0  0       # ant x
  ldi t1  0       # ant y
  ldi t2  3       # color of ant
  loop:
    ldi t3 64     # for moding
    add t0 one    # increment t0
    mod t0 t3     # mod t0 by 64
    jnz cont
      add t1 one  # if it is zero, increment t1 as well
      mod t1 t3   # mod t1 by 64
    cont:
    sys fbreset   # reset the frame buffer
    mv a0 t0
    mv a1 t1
    mv a2 t2
    sys fbset     # draw the ant
    ldi t3 100
    mv a0 t3
    sys sleep     # sleep 100 millis
    j loop
