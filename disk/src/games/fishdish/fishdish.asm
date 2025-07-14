
# This version is a good example of highly "coupled" code. The scrolling 
# information is scattered throughout the code rather than being centralized
# and the sprite coordinates translated by a separate scroll x/y position.

.data

  graphics:
    background: .image "background.png" 
    fish_left:  .image "fish-left.png" 
    fish_right: .image "fish-right.png" 
    shark_left: .image "shark-left.png" 

  directions:
    up:    0b10000000
    down:  0b01000000
    left:  0b00100000
    right: 0b00010000

  states:
    DEAD:  0
    ALIVE: 1
    BLINK: 2
    DYING: 3

  fish:
    x: 40
    y: 100
    direction: 1
    speed_x: 0
    speed_y: 0

    min_speed: -3
    max_speed: 3

  shark:
    shark_x:   600
    shark_y:   30
    shark_dir: 0


.text
main:

main_loop:

  li   a0 16
  sys  timer        # Set timer for 16 ms

  jal  move_fish
  jal  move_shark

  sys  fbreset

  jal  draw_background
  jal  draw_fish
  jal  draw_shark

  sys  fbflush

  li   a0 0
  sys  timer

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

  lw   t0 speed_x     # speed x
  lw   t1 speed_y     # speed y
  lw   t4 direction   # direction

  sys  joystick     # get the latest IO
  mov  t2 rv

move_up:
  lw   t3 up
  and  t3 t2

  neqi t3 0
  jz   move_down

  dec t1            # y--
  dec t1            # y--

move_down:
  lw   t3 down
  and  t3 t2

  neqi t3 0
  jz   move_left

  inc  t1            # y++
  inc  t1            # y++

move_left:
  lw   t3 left
  and  t3 t2

  neqi t3 0
  jz   move_right

  dec  t0            # x--
  dec  t0            # x--
  li   t4 0          # direction = left

move_right:
  lw   t3 right
  and  t3 t2

  neqi t3 0
  jz   move_max_speed

  inc  t0            # x++
  inc  t0            # x++
  li   t4 1          # direction = right

move_max_speed:
  sw   t0 speed_x    # Save speed x
  sw   t1 speed_y    # Save speed y

  jal  slow_fish

  lw   t0 speed_x     # speed x
  lw   t1 speed_y     # speed y

  mov  a0 t0
  lw   a1 max_speed
  jal  min_value     # min(speed_x, max_speed)
  mov  t0 rv
  
  mov  a0 t1
  lw   a1 max_speed
  jal  min_value     # min(speed_y, max_speed)
  mov  t1 rv

  mov  a0 t0
  lw   a1 min_speed
  jal  max_value     # max(speed_x, min_speed)
  mov  t0 rv

  mov  a0 t1
  lw   a1 min_speed
  jal  max_value     # max(speed_y, min_speed)
  mov  t1 rv

  sw   t0 speed_x    # Save speed x
  sw   t1 speed_y    # Save speed y

move_add_speed:
  lw   t2 x
  add  t0 t2         # x = speed_x + x

  lw   t2 y
  add  t1 t2         # y = speed_y + x

move_check_left_bound:
  li   t2 0
  lt   t0 t2         # x < 0
  jz   move_check_right_bound

  mov  t0 t2         # x = 0

move_check_right_bound:

  li   t2 484
  gt   t0 t2         # x > 484 (499 - 15)
  jz   move_check_top_bound

  mov  t0 t2         # x = 484

move_check_top_bound:
  li   t2 -2
  lt   t1 t2         # y < -2
  jz   move_check_bottom_bound

  mov  t1 t2         # y = -2

move_check_bottom_bound:

  li   t2 131
  gt   t1 t2         # y > 131 (144 - 15 + 2)
  jz   move_save

  mov  t1 t2         # y = 131

move_save:

  sw   t0 x           # Save x 
  sw   t1 y           # Save y 
  sw   t4 direction   # Save direction

  pop  ra
  ret


#######################################
# Min value                           #
#######################################
min_value:
  lt   a0 a1
  jnz  min_value_a0

  mov  rv a1
  ret

min_value_a0:
  mov  rv a0
  ret


#######################################
# Max value                           #
#######################################
max_value:
  gt   a0 a1
  jnz  min_value_a0

  mov  rv a1
  ret

max_value_a0:
  mov  rv a0
  ret


#######################################
# Slow down when keys are released    #
#######################################
slow_fish:
  push ra

slow_fish_check_x:
  lw   t0 speed_x

  eqi  t0 0
  jnz  slow_fish_check_y

  lti  t0 0
  jz   slow_fish_positive_x

slow_fish_negative_x:
  li   t1 1
  add  t0 t1
  sw   t0 speed_x
  j    slow_fish_check_y

slow_fish_positive_x:
  li   t1 -1
  add  t0 t1
  sw   t0 speed_x

slow_fish_check_y:
  lw   t0 speed_y

  eqi  t0 0
  jnz  slow_fish_done

  lti  t0 0
  jz   slow_fish_positive_y

slow_fish_negative_y:
  li   t1 1
  add  t0 t1
  sw   t0 speed_y
  j    slow_fish_done

slow_fish_positive_y:
  li   t1 -1
  add  t0 t1
  sw   t0 speed_y

slow_fish_done:
  pop  ra
  ret


#######################################
# Draw the shark                      #
#######################################
draw_shark:
  push ra  

  lw   t0 x             # Fish x for scrolling
  lw   a0 shark_left
  lw   a1 shark_x
  lw   a2 shark_y

draw_shark_check_right:
  li   t1 419
  gte  t0 t1
  jz   draw_shark_check_left  

  # Fixed scroll if we're at the right of the screen

  li   t1 339
  sub  a1 t1            # shark_x -= 339

  j    draw_shark_done

draw_shark_check_left:
  li   t1 80
  lt   t0 t1
  jnz  draw_shark_done  # Don't scroll, we're at the left of the screen

  li   t1 80
  sub  a1 t0            # shark_x -= x
  add  a1 t1            # shark_x += 80


draw_shark_done:
  sys  drawimg

  pop  ra
  ret



#######################################
# Move the shark                      #
#######################################
move_shark:
  push ra  

  lw   t0 shark_x

  dec  t0               # shark_x--

  li   t1 -300
  lt   t0 t1
  jz   move_shark_done

  li   t0 600
  
move_shark_done:
  sw   t0 shark_x

  pop  ra
  ret


