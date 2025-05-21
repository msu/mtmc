.text
main:
  seti a0  0       # ant col
  seti a1  0       # ant row
  seti a2  3       # color of ant

  loop:
    inc a0         # increment col
    modi a0 160    # mod col by 160
    jnz cont       # if mod was zero
      inc a1       # increment y by one
      modi a1 140  # mod it by 140
    cont:

    sys fbreset   # reset the frame buffer

    sys fbset     # draw the ant, x in a0, y in a1, color in a3

    mov t0 a0       # save x

    li a0 20
    sys sleep     # sleep 100 millis

    mov a0 t0       # restore x

    j loop        # do it all again
