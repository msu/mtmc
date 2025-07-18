# HUNT THE WUMPUS
# ---------------
# original game by Gregory Yob
# ported from BASIC by Jerason Banes
# special thanks to Lawnie for inspiration
#
# BASIC source: https://github.com/RayKV423/original-hunt-the-wumpus/

.data
  wumpus_title: .image "wumpus.png"

  newline: "\n"
  comma: ", "
  instructions_option: "\nINSTRUCTIONS (Y-N)\n"
  move_option: "\nSHOOT OR MOVE (S-M)\n"

  title: "\n\nHUNT THE WUMPUS\n\n"
  lost: "HA HA HA - YOU LOSE!\n"
  win: "HEE HEE HEE - THE WUMPUS'LL GETCHA NEXT TIME!!\n"

  instructions: 
    "\nWELCOME TO 'HUNT THE WUMPUS'\n\n"
    "THE WUMPUS LIVES IN A CAVE OF 20 ROOMS. EACH ROOM\n"
    "HAS 3 TUNNELS LEADING TO OTHER ROOMS. (LOOK AT A\n"
    "DODECAHEDRON TO SEE HOW THIS WORKS - IF YOU DON'T\n"
    "KNOW WHAT A DODECAHEDRON IS, ASK SOMEONE)\n\n"

    "HAZARDS:\n\n"
    " BOTTOMLESS PITS: TWO ROOMS HAVE BOTTOMLESS PITS IN\n"
    " THEM IF YOU GO THERE, YOU FALL INTO THE PIT (&\n"
    " LOSE!)\n\n"
    " SUPER BATS: TWO OTHER ROOMS HAVE SUPER BATS. IF\n"
    " YOU GO THERE, A BAT GRABS YOU AND TAKES YOU TO\n"
    " SOME OTHER ROOM AT RANDOM. (WHICH MIGHT BE\n"
    " TROUBLESOME)\n\n"

    "WUMPUS:\n\n"
    " THE WUMPUS IS NOT BOTHERED BY THE HAZARDS (HE HAS\n"
    " SUCKER FEET AND IS TOO BIG FOR A BAT TO LIFT). \n"
    " USUALLY HE IS ASLEEP. TWO THINGS WAKE HIM UP: YOUR\n"
    " ENTERING HIS ROOM OR YOUR SHOOTING AN ARROW.\n\n"

    " IF THE WUMPUS WAKES, HE MOVES (P=.75) ONE ROOM\n"
    " OR STAYS STILL (P=.25). AFTER THAT, IF HE IS WHERE\n"
    " YOU ARE, HE EATS YOU UP (& YOU LOSE!)\n\n"

    "YOU:\n\n"
    " EACH TURN YOU MAY MOVE OR SHOOT A CROOKED ARROW\n\n"
    " MOVING: YOU CAN GO ONE ROOM (THRU ONE TUNNEL)\n"
    " ARROWS: YOU HAVE 5 ARROWS. YOU LOSE WHEN YOU RUN\n"
    " OUT.\n\n"
    " EACH ARROW CAN GO FROM 1 TO 5 ROOMS. YOU AIM BY\n"
    " TELLING THE COMPUTER THE ROOM#S YOU WANT THE ARROW\n"
    " TO GO TO. IF THE ARROW CAN'T GO THAT WAY (I.E. \n"
    " NO TUNNEL) IT MOVES AT RAMDOM TO THE NEXT ROOM.\n\n"
    "     IF THE ARROW HITS THE WUMPUS, YOU WIN.\n"
    "     IF THE ARROW HITS YOU, YOU LOSE.\n\n"

    "WARNINGS:\n"
    " WHEN YOU ARE ONE ROOM AWAY FROM WUMPUS OR HAZARD,\n"
    " THE COMPUTER SAYS:\n\n"
    " WUMPUS - 'I SMELL A WUMPUS'\n"
    " BAT    - 'BATS NEARBY'\n"
    " PIT    - 'I FEEL A DRAFT'\n"

  smell_wumpus:  "I SMELL A WUMPUS!\n"
  feel_draft:    "I FEEL A DRAFT\n"
  bats_nearby:   "BATS NEARBY!\n"

  room_description: "YOU ARE IN ROOM "
  tunnel_description: "TUNNELS LEAD TO "
  number_of_rooms: "\nNO. OF ROOMS(1-5)\n"
  room_number: "ROOM #\n"
  
  arrows_fail: "ARROWS AREN'T THAT CROOKED - TRY ANOTHER ROOM\n"
  missed: "\nMISSED\n\n"
  hit: "AHA! YOU GOT THE WUMPUS!\n"
  wounded: "OUCH! ARROW GOT YOU!\n"
  eaten: "TSK TSK TSK- WUMPUS GOT YOU!\n"

  where_to: "\nWHERE TO\n"
  not_possible: "NOT POSSIBLE -\n"
  bumped_wumpus: "...OOPS! BUMPED A WUMPUS!\n"
  fell_in_pit: "YYYIIIIEEEE . . . FELL IN PIT\n"
  bat_snatch: "\nZAP--SUPER BAT SNATCH! ELSEWHEREVILLE FOR YOU!\n\n"

  play_again: "\nPLAY AGAIN (Y-N)\n"
  same_setup: "SAME SET-UP (Y-N)\n"

  cave_data:
    2               # 1
    5
    8

    1               # 2
    3
    10

    2               # 3 
    4
    12

    3               # 4
    5
    14

    1               # 5
    4
    6

    5               # 6
    7
    15

    6               # 7
    8
    17

    1               # 8
    7
    9

    8               # 9
    10
    18

    2               # 10
    9
    11

    10              # 11
    12
    19

    3               # 12
    11
    13

    12              # 13
    14
    20

    4               # 14
    13
    15

    6               # 15
    14
    16

    15              # 16
    17
    20

    7               # 17
    16
    18

    9               # 18
    17
    19

    11              # 19
    18
    20

    13              # 20
    16
    19


  initial_state:
    initial_you:    -1
    initial_wumpus: -1
    initial_pit_1:  -1
    initial_pit_2:  -1
    initial_bats_1: -1
    initial_bats_2: -1

  current_state:
    you:    -1
    wumpus: -1
    pit_1:  -1
    pit_2:  -1
    bats_1: -1
    bats_2: -1
    arrows: 5
    state:  0                   # 0 - Alive, 1 - Win, -1 - Dead

  arrow_path: .byte 5


.text
main:

  # Show title
  sys  fbreset
  lw   a0 wumpus_title
  li   a1 0
  li   a2 0
  sys  drawimg
  sys  fbflush

  # Ask for instructions
  li   a0 instructions_option
  li   t0 121                   # ASCII 'y'
  li   t1 89                    # ASCII 'Y'
  sys  wstr
  sys  rchr

  # Print instructions if 'Y' or 'y'
  eq   rv t0
  jnz  main_instructions
  eq   rv t1
  jnz  main_instructions

  j    start_game_with_new_setup

main_instructions:
  jal  print_instructions
  
start_game_with_new_setup:
  jal  init_game
start_game:
  jal  reset_game

  li   a0 title
  sys  wstr

game_loop:
  jal  check_hazards
  jal  describe_room
  jal  move_or_shoot
  jal  check_hazards_in_room

  lw   t0 state
  eqi  t0 0
  jnz  game_loop
  lti  t0 0
  jnz  game_lost

game_won:
  li   a0 win
  sys  wstr
  j    game_again

game_lost:
  li   a0 lost
  sys  wstr

game_again:
  li   t0 121                   # ASCII 'y'
  li   t1 89                    # ASCII 'Y'
  li   a0 play_again
  sys  wstr
  sys  rchr
  
  # Play again if 'Y' or 'y'
  eq   rv t0
  jnz  game_setup
  eq   rv t1
  jnz  game_setup

  j    game_end

game_setup:
  li   t0 121                   # ASCII 'y'
  li   t1 89                    # ASCII 'Y'
  li   a0 same_setup
  sys  wstr
  sys  rchr
  
  # Play again if 'Y' or 'y'
  eq   rv t0
  jnz  start_game
  eq   rv t1
  jnz  start_game

  j    start_game_with_new_setup

game_end:
  sys fbreset
  sys fbflush
  sys  exit



#######################################
# Initialize game state               #
#######################################
init_game:
  push ra

  jal  unique_location
  sw   rv initial_you
  sw   rv you

  jal  unique_location
  sw   rv initial_wumpus
  sw   rv wumpus

  jal  unique_location
  sw   rv initial_pit_1
  sw   rv pit_1

  jal  unique_location
  sw   rv initial_pit_2
  sw   rv pit_2

  jal  unique_location
  sw   rv initial_bats_1
  sw   rv bats_1

  jal  unique_location
  sw   rv initial_bats_2
  sw   rv bats_2

  pop  ra
  ret



##################################################
# Resets game state w/o generating new scenario  #
##################################################
reset_game:
  push ra
  
  lw   t0 initial_you
  sw   t0 you

  lw   t0 initial_wumpus
  sw   t0 wumpus

  lw   t0 initial_pit_1
  sw   t0 pit_1

  lw   t0 initial_pit_2
  sw   t0 pit_2

  lw   t0 initial_bats_1
  sw   t0 bats_1

  lw   t0 initial_bats_2
  sw   t0 bats_2

  li   t0 5
  sw   t0 arrows

  li   t0 0
  sw   t0 state

  pop  ra
  ret



###################################################################
# Randomly generates a new location not occupied by other objects #
###################################################################
unique_location:
  push ra

unique_location_generate:
  li   t0 0
  jal  fna
  mov  t1 rv
  
unique_location_loop:
  lwo  t2 t0 current_state
  eq   t2 t1
  jnz  unique_location_generate     # Not unique

  inc  t0
  inc  t0
  lti  t0 12
  jnz  unique_location_loop

  mov  rv t1

  pop  ra
  ret



###################################################
# Random function for generating locations (1-20) #
###################################################
fna:
  li   a0 1
  li   a1 20
  sys  rnd

  ret



#################################################
# Random function for generating arrow movement #
#################################################
fnb:
  li   a0 0
  li   a1 2
  sys  rnd

  ret



##################################################
# Random function for generating Wumpus movement #
##################################################
fnc:
  li   a0 0
  li   a1 3
  sys  rnd

  ret



#################################################
# Prints the game instructions to the console   #
#################################################
print_instructions:
  push ra
  li   t0 instructions
  li   t1 0             # count
  li   t2 38            # lines

print_instructions_loop:
  mov  a0 t0
  sys  wstr

  push t0
  push t1
  push t2
  jal  string_length
  pop  t2
  pop  t1
  pop  t0
  add  t0 rv

  inc  t1               # count++
  lt   t1 t2            # count < lines
  jnz  print_instructions_loop

  pop  ra
  ret



############################################################
# Compute the length of a string including zero terminator #
############################################################
string_length:
  push ra
  mov  t0 a0            # offset
  li   t2 0             # count
  
string_length_loop:
  lbo  t1 t0 0
  inc  t0               # offset++
  inc  t2               # count++
  eqi  t1 0
  jz   string_length_loop

  mov  rv t2
  pop  ra
  ret



############################################################
# Describe the current room along with exit tunnels        #
############################################################
describe_room:
  push ra

  li   a0 room_description
  sys  wstr

  lw   a0 you
  sys  wint

  li   a0 newline
  sys  wstr

  li   a0 tunnel_description
  sys  wstr

  lw   t0 you       # index = (room you're in)
  dec  t0           # index--  (make number zero based)
  li   t1 3
  mul  t0 t1        # index *= 3  (connection points per room)
  li   t1 2
  mul  t0 t1        # index *= 2  (2 bytes in a word)

  lwo  a0 t0 cave_data
  sys  wint
  li   a0 comma
  sys  wstr

  inc t0
  inc t0
  lwo  a0 t0 cave_data
  sys  wint
  li   a0 comma
  sys  wstr

  inc t0
  inc t0
  lwo  a0 t0 cave_data
  sys  wint
  li   a0 newline
  sys  wstr

  pop  ra
  ret


############################################################
# Look for hazards and print out warnings                  #
############################################################
check_hazards:
  push ra
  li   t5 0         # counter

  # Load room position
  lw   t0 you       # index = (room you're in)
  dec  t0           # index--  (make number zero based)
  li   t1 3
  mul  t0 t1        # index *= 3  (connection points per room)
  li   t1 2
  mul  t0 t1        # index *= 2  (2 bytes in a word)

check_hazards_loop:
  lwo  t2 t0 cave_data

check_hazards_wumpus:
  lw   t1 wumpus
  eq   t2 t1
  jz   check_hazards_pit_1

  li   a0 smell_wumpus
  sys  wstr

check_hazards_pit_1:
  lw   t1 pit_1
  eq   t2 t1
  jz   check_hazards_pit_2

  li   a0 feel_draft
  sys  wstr

check_hazards_pit_2:
  lw   t1 pit_2
  eq   t2 t1
  jz   check_hazards_bats_1

  li   a0 feel_draft
  sys  wstr

check_hazards_bats_1:
  lw   t1 bats_1
  eq   t2 t1
  jz   check_hazards_bats_2

  li   a0 bats_nearby
  sys  wstr

check_hazards_bats_2:
  lw   t1 bats_2
  eq   t2 t1
  jz   check_hazards_continue

  li   a0 bats_nearby
  sys  wstr

check_hazards_continue:
  inc  t0
  inc  t0
  inc  t5
  lti  t5 3
  jnz  check_hazards_loop

  pop  ra
  ret



############################################################
# Ask user to move to a new room or shoot an arrow         #
############################################################
move_or_shoot:
  push ra

  li   a0 move_option
  sys  wstr

move_or_shoot_read:
  sys  rchr
  li   t0 77           # ASCII 'M'
  eq   rv t0
  jnz  move_or_shoot_move
  li   t0 109          # ASCII 'm'
  eq   rv t0
  jnz  move_or_shoot_move
  li   t0 83           # ASCII 'S'
  eq   rv t0
  jnz  move_or_shoot_shoot
  li   t0 115          # ASCII 's'
  eq   rv t0
  jnz  move_or_shoot_shoot

  j    move_or_shoot_read

move_or_shoot_move:
  li   a0 where_to
  sys  wstr

  sys  rint
  lti  rv 1
  jnz  move_or_shoot_move_not_possible
  li   t0 20
  gt   rv t0
  jnz  move_or_shoot_move_not_possible

  # Load room position
  lw   t0 you       # index = (room you're in)
  dec  t0           # index--  (make number zero based)
  li   t1 3
  mul  t0 t1        # index *= 3  (connection points per room)
  li   t1 2
  mul  t0 t1        # index *= 2  (2 bytes in a word)

  lwo  t2 t0 cave_data
  eq   rv t2
  jnz  move_or_shoot_move_accept
  inc  t0
  inc  t0
  lwo  t2 t0 cave_data
  eq   rv t2
  jnz  move_or_shoot_move_accept
  inc  t0
  inc  t0
  lwo  t2 t0 cave_data
  eq   rv t2
  jnz  move_or_shoot_move_accept

move_or_shoot_move_not_possible:
  li   a0 not_possible
  sys  wstr
  j    move_or_shoot_move

move_or_shoot_move_accept:
  sw   rv you
  j    move_or_shoot_done

move_or_shoot_shoot:

  jal  shoot_arrow

move_or_shoot_done:

  pop  ra
  ret


############################################################
# Look for hazards in current room                         #
############################################################
check_hazards_in_room:
  push ra

  # Check we're still playing
  lw   t2 state
  eqi  t2 0
  jz   check_hazards_in_room_continue

  # Load room position
  lw   t2 you       # index = (room you're in)

check_hazards_in_room_wumpus:
  lw   t1 wumpus
  eq   t2 t1
  jz   check_hazards_in_room_pit_1

  li   a0 bumped_wumpus
  sys  wstr

  jal  move_wumpus

check_hazards_in_room_pit_1:
  lw   t1 pit_1
  eq   t2 t1
  jz   check_hazards_in_room_pit_2

  li   a0 fell_in_pit
  sys  wstr

  li   t0 -1
  sw   t0 state

check_hazards_in_room_pit_2:
  lw   t1 pit_2
  eq   t2 t1
  jz   check_hazards_in_room_bats_1

  li   a0 fell_in_pit
  sys  wstr

  li   t0 -1
  sw   t0 state

check_hazards_in_room_bats_1:
  lw   t1 bats_1
  eq   t2 t1
  jz   check_hazards_in_room_bats_2

  li   a0 bat_snatch
  sys  wstr

  jal  unique_location
  sw   rv you

check_hazards_in_room_bats_2:
  lw   t1 bats_2
  eq   t2 t1
  jz   check_hazards_in_room_continue

  li   a0 bat_snatch
  sys  wstr

  jal  unique_location
  sw   rv you

check_hazards_in_room_continue:
  pop  ra
  ret



############################################################
# Randomly move the wumpus to an adjacent room             #
############################################################
move_wumpus:
  push ra

  jal  fnc
  eqi  rv 3
  jnz  move_wumpus_done     # Wumpus didn't move

  # Load room position
  lw   t0 wumpus            # index = (room wumpus is in)
  dec  t0                   # index--  (make number zero based)
  li   t1 3
  mul  t0 t1                # index *= 3  (connection points per room)
  add  t0 rv                # index += fnc
  li   t1 2
  mul  t0 t1                # index *= 2  (2 bytes in a word)

  lwo  t2 t0 cave_data
  sw   t2 wumpus

move_wumpus_done:
  lw   t0 you
  lw   t1 wumpus
  eq   t0 t1
  jnz  move_wumpus_death    # In the same room

  pop  ra
  ret

move_wumpus_death:
  li   a0 eaten
  sys  wstr

  li   t0 -1
  sw   t0 state

  pop  ra
  ret  



############################################################
# Shoot an arrow between 1-5 rooms away                    #
############################################################
shoot_arrow:
  push ra

shoot_arrow_ammo_update:
  lw   t0 arrows
  dec  t0
  sw   t0 arrows

shoot_arrow_number_of_rooms:
  li   a0 number_of_rooms
  sys  wstr
  sys  rint
  mov  t0 rv

  # Check if less than 1
  lti  t0 1
  jnz  shoot_arrow_number_of_rooms

  # Check if greater than 5
  gti  t0 5
  jnz  shoot_arrow_number_of_rooms


  li   t5 0                     # counter
shoot_arrow_room_number:
  li   a0 room_number
  sys  wstr
  sys  rint

  lti  t5 2
  jnz  shoot_arrow_room_number_continue

  li   t1 -2
  add  t1 t5                    # index = counter - 2
  lbo  t2 t1 arrow_path
  eq   t2 rv                    # Check if arrow_path[room] != arrow_path[room-2]
  jz   shoot_arrow_room_number_continue

shoot_arrow_room_number_error:
  li   a0 arrows_fail
  sys  wstr
  j    shoot_arrow_room_number

shoot_arrow_room_number_continue:
  sbo  rv t5 arrow_path
  inc  t5                       # counter++
  lt   t5 t0                    # counter < number_of_rooms
  jnz  shoot_arrow_room_number


  li   t5 0                     # counter
  lw   t1 you                   # current room
shoot_arrow_travel:
  mov  t2 t1
  dec  t2                       # index--  (make number zero based)
  li   t3 3
  mul  t2 t3                    # index *= 3  (connection points per room)
  li   t3 2
  mul  t2 t3                    # index *= 2  (2 bytes in a word)

  lbo  t4 t5 arrow_path
  lwo  t3 t2 cave_data
  eq   t3 t4                    # arrow_path[counter] == tunnels[0]
  jnz  shoot_arrow_travel_set_room

  inc t2
  inc t2
  lwo  t3 t2 cave_data
  eq   t3 t4                    # arrow_path[counter] == tunnels[1]
  jnz  shoot_arrow_travel_set_room

  inc t2
  inc t2
  lwo  t3 t2 cave_data
  eq   t3 t4                    # arrow_path[counter] == tunnels[2]
  jnz  shoot_arrow_travel_set_room
  
shoot_arrow_travel_set_random:
  mov  t2 t1
  dec  t2                       # index--  (make number zero based)
  li   t3 3
  mul  t2 t3                    # index *= 3  (connection points per room)
  jal  fnb                      # No tunnel available, randomize arrow path
  add  t2 rv
  li   t3 2
  mul  t2 t3                    # index *= 2  (2 bytes in a word)
  lwo  t3 t2 cave_data

shoot_arrow_travel_set_room:
  mov  t1 t3

shoot_arrow_travel_check_wumpus:
  lw   t3 wumpus
  eq   t1 t3                    # room == wumpus
  jz   shoot_arrow_travel_check_self

  li   a0 hit
  sys  wstr
  li   t0 1
  sw   t0 state
  j    shoot_arrow_done

shoot_arrow_travel_check_self:
  lw   t3 you
  eq   t1 t3                    # room == you
  jz   shoot_arrow_travel_continue

  li   a0 wounded
  sys  wstr
  li   t0 -1
  sw   t0 state
  j    shoot_arrow_done

shoot_arrow_travel_continue:
  inc  t5                       # counter++
  lt   t5 t0                    # counter < number_of_rooms
  jnz  shoot_arrow_travel

shoot_arrow_travel_check_missed:
  li   a0 missed
  sys  wstr

  jal  move_wumpus

shoot_arrow_done:
  lw   t0 arrows
  eqi  t0 0
  jnz  shoot_arrow_ammo_out

  pop  ra
  ret

shoot_arrow_ammo_out:
  li   t0 -1
  sw   t0 state                 # No ammo left, the hunt has failed!

  pop  ra
  ret