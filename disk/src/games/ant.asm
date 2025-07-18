.text
main:
  li t0  78       # ant col
  li t1  68       # ant row

  loop:

    # get the latest IO
    sys joystick
    mov t2 rv

    mov t3 t2
    andi t3 0b1000_0000 # up
    jz l1
      dec t1 4
    l1:

    mov t3 t2
    andi t3 0b0100_0000 # down
    jz l2
      inc t1 4
    l2:

    mov t3 t2
    andi t3 0b0010_0000 # left
    jz l3
      dec t0 4
    l3:

    mov t3 t2
    andi t3 0b0001_0000 # right
    jz l4
      inc t0 4
    l4:

    lti t0 0
    jz l5
      li t0 159
    l5:

    lti t1 0
    jz l6
      li t1 139
    l6:

    modi t0 160
    modi t1 144

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
