.text
main:
  li a0  0       # ant x
  li a1  0       # ant y
  li a2  3       # color of ant

  loop:
    inc a0         # increment x
    imm mod a0 64  # mod x by 64
    jnz cont       # if mod was zero
      inc a1          # increment y by one
      imm mod a1 64   # mod it by 64
    cont:

    sys fbreset   # reset the frame buffer

    sys fbset     # draw the ant, x in a0, y in a1, color in a3

    push a0       # save x

    li a0 100
    sys sleep     # sleep 100 millis

    pop a0        # restore x

    j loop        # do it all again
