################################################################################
#                                                                              #
#   Author:     Jerason Banes                                                  #
#   Version:    1.0                                                            #
#   Updated:    2025-07-30                                                     #
#   Clock Rate: 1Mhz or higher                                                 #
#                                                                              #
#   A game of snake similar to nibbles.bas and the old Nokia phone game. The   #
#   game allocates 1440 bytes of memory on the heap to store the game board.   #
#                                                                              #
#   A 1MHz clock rate is required due to the way in which the screen is        #
#   rendered. The full loop over the game board (40 x 36) means a few          #
#   thousand instructions are needed for each game loop.                       #
#                                                                              #
################################################################################

.data
  directions:
    up:    0b10000000
    down:  0b01000000
    left:  0b00100000
    right: 0b00010000

  screen:
    buffer:     -1

  snake:
    start_x:    20
    start_y:    18

    x:          20
    y:          18

    width:      40
    height:     36

    speed:      200
    direction:  0b10000000
    crash:      0

    offset:     0
    length:     1
    max_length: 80

    grow_size:  1
    tail_size:  3
    tail_x:     .byte 80
    tail_y:     .byte 80

  food:
    food_x:     0
    food_y:     0

  messages:
    separator: "--------------------------------\n"
    game_over: "         **CRASH**\n  Game over, man. Game Over!\n"
    space: " "

.text
main:

  lw  t0 start_x
  lw  t1 start_y
  sb  t0 tail_x
  sb  t1 tail_y

  jal  init_game
main_loop:
  jal  draw_snake
  jal  render_screen
  jal  poll_controls
  jal  erase_tail
  jal  move_snake
  jal  check_crashed
  jal  check_food

  j main_loop

  sys  exit


#######################################
# Initialize game state               #
#######################################
init_game:
  push ra

  # Allocate screen buffer
  li  a0 1440
  jal malloc
  sw  rv buffer

  # Set starting location
  lw  t0 start_x
  lw  t1 start_y
  sw  t0 x
  sw  t1 y
  sb  t0 tail_x
  sb  t1 tail_y

  jal  clear_buffer
  jal  draw_walls
  jal  place_food
  
  pop  ra
  ret



####################################################
# Clear the screen buffer in preperation for use   #
####################################################
clear_buffer:
  push ra

  # Allocate back buffer
  lw   t0 width
  lw   t1 height

  # Malloc and clear memory at 1024 - 2464
  mov  t2 t0
  mul  t2 t1
  li   t3 0         # Counter
  li   t4 0         # Zero

clear_loop:

  lw   t5 buffer
  add  t5 t3        # position = buffer + counter
  sbo  t4 t5 0

  inc  t3           # counter++
  lt   t3 t2        # counter < (width * height)
  jnz  clear_loop   # loop while previous statement is true

  pop  ra
  ret


#######################################
# Draws the 4 walls into the buffer   #
#######################################
draw_walls:
  push ra

  lw   t0 width
  lw   t1 height

  # Top line
  li   t3 0
  li   t4 1         # One for writing walls
top_wall:
  lw   t5 buffer
  add  t5 t3
  sbo  t4 t5 0
  inc  t3           # counter++
  lt   t3 t0        # counter < width
  jnz  top_wall     # loop while previous statement is true

  # Bottom line
  li   t3 0
  li   t4 1         # One for writing walls
bottom_wall:
  lw   t5 buffer
  add  t5 t3
  sbo  t4 t5 1400   # buffer + counter + (40 * (height - 1))
  inc  t3           # counter++
  lt   t3 t0        # counter < width
  jnz  bottom_wall     # loop while previous statement is true

  # Left line
  li   t3 0
  li   t4 1         # One for writing walls
  li   t5 0         # Row offset
left_wall:
  lw   t0 buffer
  add  t0 t5
  sbo  t4 t0 0

  lw   t0 width
  lw   t1 height

  inc  t3           # counter++
  add  t5 t0        # position += width
  lt   t3 t1        # counter < height
  jnz  left_wall    # loop while previous statement is true

  # Right line
  li   t3 0
  li   t4 1         # One for writing walls
  li   t5 39        # Row offset
right_wall:
  lw   t0 buffer
  add  t0 t5
  sbo  t4 t0 0

  lw   t0 width
  lw   t1 height

  inc  t3           # counter++
  add  t5 t0        # position += width
  lt   t3 t1        # counter < height
  jnz  right_wall   # loop while previous statement is true

  pop  ra
  ret


#######################################
# Draws the snake to the buffer       #
#######################################
draw_snake:
  push ra

  lw   t0 offset
  lw   t1 length
  li   t2 0         # counter
  lw   t3 max_length

draw_snake_loop:
  lbo  a0 t0 tail_x # x
  lbo  a1 t0 tail_y # y
  lw   a2 width     # width
  lw   a3 height    # height

  mov  t4 a2        # position = width
  mul  t4 a1        # position = width * y
  add  t4 a0        # position = (width * y) + x

  push t0

  lw   t0 buffer
  add  t0 t4
  li   t5 1
  sbo  t5 t0 0      # store 1 to address buffer + ((width * y) + x)

  pop  t0

draw_snake_increment_offset:
  inc  t0           # offset++
  mod  t0 t3        # offset = offset % max_length

draw_snake_counter:
  inc  t2           # counter++
  lt   t2 t1        # counter < length
  jnz  draw_snake_loop

  pop  ra
  ret



#######################################
# Erase the end of the snake's tail   #
#######################################
erase_tail:
  push ra

erase_tail_check_length:
  lw   t0 length
  lw   t1 tail_size
  lt   t0 t1            # length < tail_size
  jnz  erase_tail_done  # do not erase tail, let it grow

erase_tail_load:
  lw   t5 offset
  lbo  t0 t5 tail_x 
  lbo  t1 t5 tail_y

erase_tail_location:
  lw   t2 width
  lw   t3 height
  mov  t4 t1         # position = y
  mul  t4 t2         # position = y * width
  add  t4 t0         # position = (y * width) + x

  lw   t0 buffer
  add  t0 t4
  li   t5 0          # Empty cell value
  sbo  t5 t0 0       # mem[buffer + position]

erase_tail_done:
  pop  ra
  ret



#######################################
# Renders the buffer to the screen    #
#######################################
render_screen:
  push ra
  sys  fbreset

  lw   t0 width
  lw   t1 height

  li   t2 0         # counter
  li   t3 4         # rectangle size
  li   a2 4         # width
  li   a3 4         # height

  mov  t5 t0
  mul  t5 t1        # width * height

render_loop:

  lw   t0 width

  # Compute X
  mov  a0 t2   
  mod  a0 t0        # counter % width
  mul  a0 t3        # x * 4

  # Compute Y
  mov  a1 t2   
  div  a1 t0        # counter / width
  mul  a1 t3        # y * 4

  lw   t1 buffer
  add  t1 t2
  lbo  t4 t1 0
  eqi  t4 1         # value == 1
  jz render_increment

  sys fbrect

render_increment:

  inc  t2
  lt   t2 t5
  jnz  render_loop

render_food:
  li   a0 0b01      # Food color
  sys  scolor

  lw   a0 food_x
  lw   a1 food_y

  mul  a0 t3
  mul  a1 t3
  
  sys fbrect

  sys  fbflush

  pop  ra
  ret


#######################################
# Calculate the snake's new position  #
#######################################
move_snake:
  push ra

  lw   t0 x         # x
  lw   t1 y         # y
  lw   t5 direction # current snake direction

move_up:
  lw   t4 up
  and  t4 t5

  neqi t4 0
  jz   move_down

  dec t1
  j    move_save

move_down:
  lw   t4 down
  and  t4 t5

  neqi t4 0
  jz   move_left

  inc t1
  j    move_save

move_left:
  lw   t4 left
  and  t4 t5

  neqi t4 0
  jz   move_right

  dec t0
  j    move_save

move_right:
  lw   t4 right
  and  t4 t5

  neqi t4 0
  jz   move_save

  inc  t0
  j    move_save

move_save:

  sw   t0 x           # Save x of the head
  sw   t1 y           # Save y of the head

  lw   t2 offset      
  lw   t3 length
  lw   t4 max_length
  add  t2 t3
  mod  t2 t4
  
  sbo  t0 t2 tail_x 
  sbo  t1 t2 tail_y

move_extend_tail:
  lw   t4 tail_size
  lt   t3 t4        # length < tail_size
  jz   move_increment_offset

  inc  t3
  sw   t3 length
  j    move_check_crash

move_increment_offset:
  lw   t2 offset
  lw   t3 max_length

  inc  t2
  mod  t2 t3
  sw   t2 offset

move_check_crash:
  lw   t2 width
  lw   t3 height
  mov  t4 t1         # position = y
  mul  t4 t2         # position = y * width
  add  t4 t0         # position = (y * width) + x

  lw   t1 buffer
  add  t1 t4
  lbo  t5 t1 0       # mem[buffer + position]

  sw   t5 crash      # If the cell we read is non-zero, crash will be non-zero

  pop ra
  ret
  


###########################################################
# Poll joystick until it's time to render the next frame  #
###########################################################
poll_controls:
  push ra

set_timer:
  lw  a0 speed
  sys timer

read_input:
  
  sys joystick   # get the latest IO
  mov t2 rv

check_up:
  lw  t3 up
  and t3 t2
  neqi t3 0
  jnz set_direction

check_down:
  lw  t3 down
  and t3 t2
  neqi t3 0
  jnz set_direction

check_left:
  lw  t3 left
  and t3 t2
  neqi t3 0
  jnz set_direction

check_right:
  lw  t3 right
  and t3 t2
  neqi t3 0
  jnz set_direction

no_input:
  j   check_timer

set_direction:
  sw t3 direction

check_timer:
  lw  a0 0
  sys timer

  eqi rv 0
  jz  read_input        # Keep looping if the timer is not zero

  pop  ra
  ret



###########################################################
# Check if the player has crashed and end the game        #
###########################################################
check_crashed:
  push ra

  lw   t0 crash
  eqi  t0 1

  jnz  crashed

  pop ra
  ret

crashed:
  li  a0 separator
  sys wstr

  li  a0 game_over
  sys wstr

  li  a0 separator
  sys wstr

  sys exit



###########################################################
# Find an empty spot to place food                        #
###########################################################
place_food:
  push ra

place_food_attempt:
  li   a0 1         # Screen is surrounded with a wall
  lw   a1 width
  dec  a1           # Screen is surrounded with a wall

  sys  rnd
  mov  t0 rv        # random x coordinate

  li   a0 1         # Screen is surrounded with a wall
  lw   a1 height
  dec  a1           # Screen is surrounded with a wall

  sys  rnd
  mov  t1 rv        # random y coordinate

place_food_check:
  lw   t2 width

  mov  t4 t1        # position = y
  mul  t4 t2        # position = y * width
  add  t4 t0        # position = (y * width) + x

  lw   t3 buffer
  add  t3 t4
  lbo  t5 t3 0
  eqi  t5 1         # value == 1
  jnz  place_food_attempt

  sw   t0 food_x
  sw   t1 food_y

  pop  ra
  ret



###########################################################
# Check if our snake has touched the food                 #
###########################################################
check_food:
  push ra

  lw   t0 x
  lw   t1 y

  lw   t2 food_x
  lw   t3 food_y

  eq   t0 t2            # x == food_x
  jz   check_food_done

  eq   t1 t3            # y == food_y
  jz   check_food_done
  
check_food_eaten:
  jal  place_food

  lw   t0 tail_size
  lw   t1 max_length
  lt   t0 t1            # tail_size < max_length
  jz   check_food_done

  lw   t4 grow_size
  li   t5 0             # counter
check_food_extend_tail:
  lt   t0 t1            # tail_size < max_length
  jz   check_food_done  # Tail is as big as it's going to get

  inc  t0               # tail_size = tail_size + 1
  sw   t0 tail_size

  inc  t5               # counter++
  lt   t5 t4            # counter < grow_size
  jnz  check_food_extend_tail

  inc  t4               # grow_size++
  sw   t4 grow_size

check_food_speed:
  lw   t0 speed
  li   t1 10
  sub  t0 t1
  sw   t0 speed

check_food_done:
  pop  ra
  ret


#############################################################################
# Allocate the amount of memory specified in a0 and return a pointer in rv  #
#############################################################################
malloc:

    mov  rv bp
    add  bp a0

    ret