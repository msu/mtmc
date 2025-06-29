################################################################################
#                                                                              #
#   Author:     Jerason Banes                                                  #
#   Version:    1.0                                                            #
#   Updated:    2025-07-29                                                     #
#   Clock Rate: 1Mhz or higher                                                 #
#                                                                              #
#   A game of snake similar to nibbles.bas and the old Nokia phone game. The   #
#   game is somewhat unique because it manually allocates 1440 bytes of memory #
#   at location 1024 to store the game board.                                  #
#                                                                              #
#   This is important to remember as the game grows. If the program takes more #
#   than 1KB of RAM, it will overflow into the game data.                      #
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

  snake:
    start_x:    20
    start_y:    18

    x:          20
    y:          18

    width:      40
    height:     36

    speed:      250
    direction:  0b10000000
    crash:      0

    offset:     0
    length:     1
    max_length: 4

    tail_x: "1234"
    tail_y: "1234"

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
  jal  move_snake
  jal  check_crashed

  j main_loop

  sys  exit  


#######################################
# Initialize game state               #
#######################################
init_game:
  push ra

  # Set starting location
  lw  t0 start_x
  lw  t1 start_y
  sw  t0 x
  sw  t1 y
  sb  t0 tail_x
  sb  t1 tail_y

  jal  clear_buffer
  jal  draw_walls

  pop  ra
  ret


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

  sbo  t4 t3 1024
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
  sbo  t4 t3 1024
  inc  t3           # counter++
  lt   t3 t0        # counter < width
  jnz  top_wall     # loop while previous statement is true

  # Bottom line
  li   t3 0
  li   t4 1         # One for writing walls
bottom_wall:
  sbo  t4 t3 2424   # 40 * (height - 1) + 1400
  inc  t3           # counter++
  lt   t3 t0        # counter < width
  jnz  bottom_wall     # loop while previous statement is true

  # Left line
  li   t3 0
  li   t4 1         # One for writing walls
  li   t5 0         # Row offset
left_wall:
  sbo  t4 t5 1024
  inc  t3           # counter++
  add  t5 t0        # position += width
  lt   t3 t1        # counter < height
  jnz  left_wall    # loop while previous statement is true

  # Right line
  li   t3 0
  li   t4 1         # One for writing walls
  li   t5 0         # Row offset
right_wall:
  sbo  t4 t5 1063   # 1024 + 39 = 1063
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

   li   t5 1
   sbo  t5 t4 1024   # store 1 to address 1024 + ((width * y) + x)

   inc  t0           # offset++
   inc  t2           # counter++
   mod  t0 t3        # offset = offset % max_length

   lt   t2 t1        # counter < length
   jnz  draw_snake_loop

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

  # Compute X
  mov  a0 t2   
  mod  a0 t0        # counter % width
  mul  a0 t3        # x * 4

  # Compute Y
  mov  a1 t2   
  div  a1 t0        # counter / width
  mul  a1 t3        # y * 4

  lbo  t4 t2 1024
  eqi  t4 1         # value == 1
  jz increment

  sys fbrect

increment:

  inc  t2
  lt   t2 t5
  jnz  render_loop

  sys  fbflush

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
  lbo  t5 t4 1024    # mem[1024 + position]

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
