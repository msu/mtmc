.data
INIT_FILE: "/data/gun.gol"
DEBUG: "DEBUG "
SP: " "
NL: "\n"
OLD_WORLD: byte[700] # 80x70 = 700
NEW_WORLD: byte[700]

.text
main:
    jal load_file
    main_loop:
        jal write_to_display
        jal update
        j main_loop
    sys exit

update:
    push ra

    # copy NEW_WORLD to OLD_WORLD
    la a0 NEW_WORLD
    la a1 OLD_WORLD
    li a2 700
    sys memcopy

    seti a0 0 # col
    seti a1 0 # row
    update_row_loop:
        nop
        update_col_loop:

            # get the old val of the cell
            push a0
            push a1
            la a3 OLD_WORLD
            jal get_bit_val
            pop a1
            pop a0
            mov a2 rv

            # get neighbor count of the cell
            push a0
            push a1
            jal get_neighbor_count
            pop a1
            pop a0
            mov a3 rv

            # update the cell in the new world
            push a0
            push a1
            jal update_new_world_cell
            pop a1
            pop a0

            inc a0
            modi a0 80
            jnz update_col_loop
        inc a1
        modi a1 70
        jnz update_row_loop
    pop ra
    ret

######################################################################
# get_neighbor_count(col, row) -
#
# Returns the count of neighbors that are alive surrounding the cell
# col, row
#
# Mutates t0
#
######################################################################
get_neighbor_count:
    push ra

    seti t0 0

    # col - 1, row - 1
    dec a0
    dec a1
    push a1
    push a0
    li a3 OLD_WORLD
    jal get_bit_val
    pop a0
    pop a1
    add t0 rv

    # col, row - 1
    inc a0
    push a1
    push a0
    li a3 OLD_WORLD
    jal get_bit_val
    pop a0
    pop a1
    add t0 rv

    # col + 1, row - 1
    inc a0
    push a1
    push a0
    li a3 OLD_WORLD
    jal get_bit_val
    pop a0
    pop a1
    add t0 rv

    # col - 1, row
    dec a0 2
    inc a0
    push a1
    push a0
    li a3 OLD_WORLD
    jal get_bit_val
    pop a0
    pop a1
    add t0 rv

    # col + 1, row
    inc a0 2
    push a1
    push a0
    li a3 OLD_WORLD
    jal get_bit_val
    pop a0
    pop a1
    add t0 rv

    # col - 1, row + 1
    dec a0 2
    inc a1 1
    push a1
    push a0
    li a3 OLD_WORLD
    jal get_bit_val
    pop a0
    pop a1
    add t0 rv

    # col, row + 1
    inc a0
    push a1
    push a0
    li a3 OLD_WORLD
    jal get_bit_val
    pop a0
    pop a1
    add t0 rv

    # col + 1, row + 1
    inc a0
    push a1
    push a0
    li a3 OLD_WORLD
    jal get_bit_val
    pop a0
    pop a1
    add t0 rv

    mov rv t0

    pop ra
    ret

######################################################################
# update_new_world_cell(col, row, old_val, neighbor_count) -
#
# Returns the count of neighbors that are alive surrounding the cell
# col, row
#
######################################################################
update_new_world_cell:
    push ra
    seti a3 0 # assume value will be zero

    # if current value is zero
    eqi a2 0
    jz update_new_world_cell_non_zero
        # curr val is zero, so if neighbor count is three
        eqi a3 3
        jz update_new_world_cell_done
        # set new value to 1
        seti a3 1
        j update_new_world_cell_done
    update_new_world_cell_non_zero:
        # curr val is one, so if neighbor count is greater than one
        gti a3 1
        jz update_new_world_cell_done
        # and less than 4
        lti a3 4
        jz update_new_world_cell_done
        # set new value to 1
        seti a3 1
    update_new_world_cell_done:
    la a2 NEW_WORLD
    jal set_bit_val
    pop ra
    ret

######################################################################
# get_bit_val(col, row, arr) -> 0 or 1
#
# Returns the bit value the cell[col, row] in the array arr
#
# Mutates a0, a1
#
######################################################################
get_bit_val:

    # compute offset in array
    muli a1 80
    add a1 a0
    mov a0 a1

    # compute byte offset in array
    divi a0 8

    # compute bit offset in byte
    modi a1 8

    # load the byte
    lbr rv a2 a0

    # mask the bit
    seti a0 1
    shl a0 a1
    and rv a1
    sys debug

    # convert to 1 or 0
    lnot rv
    lnot rv

    ret

######################################################################
# set_bit_val(col, row, arr, val)
#
# Sets the bit value the cell[col, row] in the array arr
#
# Mutates t0, t1, t2
#
######################################################################
set_bit_val:
    mov t0 a0
    mov t1 a1

    # compute offset in array into t0 and t1
    muli t1 80
    add t1 t0
    mov t0 t1

    # compute byte offset in array in t0
    divi t0 8

    # compute bit offset in byte in t1
    modi t1 8

    # load the byte value into t2
    lbr t2 a2 t0

    # compute bit-mask in t0
    seti t0 1
    shl t0 a1

    # test if the value is 1 or 0
    eqi a3 1
    jz set_bit_val_zero
      # new val is 1 so or current byte w/t0
      or t2 t0
      j set_bit_val_end
    set_bit_val_zero:
      # new val is 0 so and current byte w/~t0
      not t0
      and t2 t0
    set_bit_val_end:

    # save the byte to memory
    sbr rv a2 a0
    ret

load_file:
    # load the init file into the bit map
    li a0 INIT_FILE
    li a1 NEW_WORLD
    li a2 700   # max bytes
    li a3 42    # gol file
    sys rfile
    ret

write_to_display:
# write to display
sys fbreset   # reset the frame buffer
    push ra

    seti a0 0 # col
    seti a1 0 # row
    write_to_display_row_loop:
        nop
        write_to_display_col_loop:

            # store col, row
            push a0
            push a1

            la a2 NEW_WORLD
            jal get_bit_val
        
            # set the color to DARK or WHITE depending on the bit val
            eqi rv 0
            jnz write_to_display_set_dark
              seti a0 3
            write_to_display_set_dark:
              seti a0 0
            write_to_display_set_end:
            sys scolor
        
            # restore col, row
            pop a1
            pop a0
        
            # draw a 2x2 square scaled by 2 position lens
            muli a0 2        
            muli a1 2
            seti a2 2
            seti a3 2        
            sys fbrect

            # restore a0 and a1
            divi a0 2
            divi a1 2

            sys fbflush   # sync the screen

            inc a0
            modi a0 80
            jnz write_to_display_col_loop
        inc a1
        modi a1 70
        jnz write_to_display_row_loop

    pop ra
    ret