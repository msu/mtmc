.text
main:
  seti t0  0       # ant col
  seti t1  0       # ant row

  loop:
    inc t0 4         # increment col
    modi t0 160    # mod col by 160
    jnz cont       # if mod was zero
      inc t1 4       # increment y by one
      modi t1 140  # mod it by 140
    cont:

    sys fbreset   # reset the frame buffer

    mov a0 t0     # move x and y into args
    mov a1 t1
    li a2 4       # 4x4 rectangle
    li a3 4
    sys fbrect   # draw the ant

    sys fbflush   # sync the screen

    li a0 100
    sys sleep     # sleep 100 millis

    j loop        # do it all again
