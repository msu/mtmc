.data

  graphics:
    background: image { "background.png" }
    fish_left: image { "fish-left.png" }
    fish_right: image { "fish-right.png" }

  directions:
    up:    0b10000000
    down:  0b01000000
    left:  0b00100000
    right: 0b00010000

  x: 400
  y: 100
  direction: 1
  speed_x: 0
  speed_y: 0

  max_speed: 4




.text
main:

main_loop:

  li   a0 16
  sys  timer        # Set timer for 16 ms

  jal  move_fish

  sys  fbreset

  jal  draw_background
  jal  draw_fish

  sys  fbflush

  li   a0 0
  sys  timer

  eqi  rv 0
  jnz  main_loop

  mov  a0 rv
  sys  sleep

  j    main_loop



########################
# Draw background      #
########################
draw_background:
  push ra

  lw   a0 background
  li   a1 0
  li   a2 0

  lw   t0 x

  li   t2 419       # 499 - 80 (1/2 screen width)
  gt   t0 t2        # x > 419
  jz   draw_background_scroll

  li   a1 -339      # x = -339 (499 - 160)
  j    draw_background_image

draw_background_scroll:
  li   t1 80
  gt   t0 t1        # Has the player crossed the center point of the screen?
  jz  draw_background_image

  sub  t0 t1
  sub  a1 t0        # background_x = -x

draw_background_image:
  sys  drawimg

  pop  ra
  ret


########################
# Draw hero fish       #
########################
draw_fish:

  lw   a0 fish_right
  lw   a1 x
  lw   a2 y

  lw   t0 direction
  eqi  t0 1
  jnz  draw_fish_end_scroll

  lw   a0 fish_left

draw_fish_end_scroll:

  li   t2 419
  gt   a1 t2        # x > 419 (499 - 80)

  jz   draw_fish_scroll

  sub  a1 t2        # x -= 419
  li   t2 80
  add  a1 t2        # x += 80
  j    draw_fish_image

draw_fish_scroll:

  li   t2 80
  gt   a1 t2        # x > 80
  jz   draw_fish_image

  mov  a1 t2

draw_fish_image:

  sys  drawimg

  ret


#######################################
# Calculate the fish's new position   #
#######################################
move_fish:
  push ra

  lw   t0 x         # x
  lw   t1 y         # y
  lw   t4 direction # direction

  sys  joystick     # get the latest IO
  mov  t2 rv

move_up:
  lw   t3 up
  and  t3 t2

  neqi t3 0
  jz   move_down

  dec t1            # y--

move_down:
  lw   t3 down
  and  t3 t2

  neqi t3 0
  jz   move_left

  inc  t1            # y++

move_left:
  lw   t3 left
  and  t3 t2

  neqi t3 0
  jz   move_right

  dec  t0            # x--
  li   t4 0          # direction = left

move_right:
  lw   t3 right
  and  t3 t2

  neqi t3 0
  jz   move_check_left_bound

  inc  t0            # x++
  li   t4 1          # direction = right

move_check_left_bound:

  li   t2 0
  lt   t0 t2         # x < 0
  jz  move_check_right_bound

  mov  t0 t2         # x = 0

move_check_right_bound:

  li   t2 484
  gt   t0 t2         # x > 484 (499 - 15)
  jz  move_save

  mov  t0 t2         # x = 484

move_save:

  sw   t0 x           # Save x 
  sw   t1 y           # Save y 
  sw   t4 direction   # Save direction

  pop  ra
  ret
