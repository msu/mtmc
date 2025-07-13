.data
INIT_FILE: "/data/gun.cells"
NOT_FOUND: "FILE NOT FOUND"
OLD_WORLD: .byte 1440 # 40x36 = 1440 bytes
NEW_WORLD: .byte 1440

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
    li a2 1440
    sys memcopy

    seti a0 0 # col
    seti a1 0 # row
    update_row_loop:
        nop
        update_col_loop:

            # get the old val of the cell
            la a3 OLD_WORLD
            jal get_byte_val
            mov a2 rv

            # get neighbor count of the cell
            push a2
            jal get_neighbor_count
            pop a2
            mov a3 rv

            # update the cell in the new world
            jal update_new_world_cell

            inc a0
            modi a0 40
            jnz update_col_loop
        inc a1
        modi a1 36
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
    li a2 OLD_WORLD
    jal get_byte_val
    add t0 rv

    # col, row - 1
    inc a0
    li a2 OLD_WORLD
    jal get_byte_val
    add t0 rv

    # col + 1, row - 1
    inc a0
    li a2 OLD_WORLD
    jal get_byte_val
    add t0 rv

    # col - 1, row
    dec a0 2
    inc a1
    li a2 OLD_WORLD
    jal get_byte_val
    add t0 rv

    # col + 1, row
    inc a0 2
    li a2 OLD_WORLD
    jal get_byte_val
    add t0 rv

    # col - 1, row + 1
    dec a0 2
    inc a1 1
    li a2 OLD_WORLD
    jal get_byte_val
    add t0 rv

    # col, row + 1
    inc a0
    li a2 OLD_WORLD
    jal get_byte_val
    add t0 rv

    # col + 1, row + 1
    inc a0
    li a2 OLD_WORLD
    jal get_byte_val
    dec a0
    dec a1
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
    seti t0 0 # assume value will be zero

    # if current value is zero
    eqi a2 0
    jz update_new_world_cell_non_zero
        # curr val is zero, so if neighbor count is three
        eqi a3 3
        jz update_new_world_cell_done
        # set new value to 1
        seti t0 1
        j update_new_world_cell_done
    update_new_world_cell_non_zero:
        # curr val is one, so if neighbor count is greater than one
        gti a3 1
        jz update_new_world_cell_done
        # and less than 4
        lti a3 4
        jz update_new_world_cell_done
        # set new value to 1
        seti t0 1
    update_new_world_cell_done:
    la a2 NEW_WORLD
    mov a3 t0
    jal set_byte_val
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

    # If the col or row is out of bounds, return zero
    lti a0 0
    jz get_bit_val_row_gt_zero
      seti rv 0
      ret
    get_bit_val_row_gt_zero:

    li rv 39
    gt a0 rv
    jz get_bit_val_row_gt_39
      seti rv 0
      ret
    get_bit_val_row_gt_39:

    lti a1 0
    jz get_bit_val_col_gt_zero
      seti rv 0
      ret
    get_bit_val_col_gt_zero:

    li rv 35
    gt a1 rv
    jz get_bit_val_col_lt_35
      seti rv 0
      ret
    get_bit_val_col_lt_35:


    # compute offset in array
    mov t4 a0
    mov t5 a1
    muli t5 40
    add t5 t4
    mov t4 t5


    # compute byte offset in array
    divi t4 8

    # compute bit offset in byte
    modi t5 8

    # load the byte
    lbr rv a2 t4

    # mask the bit
    seti t4 1
    shl t4 t5
    and rv t4

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
    muli t1 40
    add t1 t0
    mov t0 t1

    # compute byte offset in array in t0
    divi t0 8

    # compute bit offset in byte in t1
    modi t1 8

    # load the byte value into t2
    lbr t2 a2 t0

    # compute bit-mask in t3
    seti t3 1
    shl t3 t1

    # test if the value is 1 or 0
    eqi a3 1
    jz set_bit_val_zero
      # new val is 1 so or current byte w/t0
      or t2 t3
      j set_bit_val_end
    set_bit_val_zero:
      # new val is 0 so and current byte w/~t0
      not t3
      and t2 t3
    set_bit_val_end:

    # save the byte back to memory
    sbr t2 a2 t0
    ret

######################################################################
# get_byte_val(col, row, arr) -> 0 or 1
#
# Returns the bit value the cell[col, row] in the array arr
#
# Mutates a0, a1
#
######################################################################
get_byte_val:

    # If the col or row is out of bounds, return zero
    lti a0 0
    jz get_byte_val_row_gt_zero
      seti rv 0
      ret
    get_byte_val_row_gt_zero:

    li rv 39
    gt a0 rv
    jz get_byte_val_row_gt_39
      seti rv 0
      ret
    get_byte_val_row_gt_39:

    lti a1 0
    jz get_byte_val_col_gt_zero
      seti rv 0
      ret
    get_byte_val_col_gt_zero:

    li rv 35
    gt a1 rv
    jz get_byte_val_col_lt_35
      seti rv 0
      ret
    get_byte_val_col_lt_35:


    # compute offset in array
    mov t4 a0
    mov t5 a1
    muli t5 40
    add t5 t4
    mov t4 t5

    # load the byte
    lbr rv a2 t4

    ret


######################################################################
# set_byte_val(col, row, arr, val)
#
# Sets the bit value the cell[col, row] in the array arr
#
# Mutates t0, t1, t2
#
######################################################################
set_byte_val:
    mov t0 a0
    mov t1 a1

    # compute offset in array into t0 and t1
    muli t1 40
    add t1 t0
    mov t0 t1

    # save the byte back to memory
    sbr a3 a2 t0
    ret


load_file:
    eqi a0 0
    jz file_given
        # no file given, load the default file into the new world
        li a0 INIT_FILE
    file_given:
    li a1 NEW_WORLD
    li a2 40    # 40 cols
    li a3 36    # 36 rows
    sys rfile

    eqi rv 0
    jnz load_file_found
        li a0 NOT_FOUND
        sys wstr
        sys exit

    load_file_found:
    li t0 0
    li t1 0
    li t2 1440
    
    load_file_decode_loop:
    lbo t3 t0 NEW_WORLD
    li  t4 1
    and t4 t3
    sbo t4 t1 OLD_WORLD
    li  t4 1
    shr t3 t4
    inc t1

    li  t4 1
    and t4 t3
    sbo t4 t1 OLD_WORLD
    li  t4 1
    shr t3 t4
    inc t1

    li  t4 1
    and t4 t3
    sbo t4 t1 OLD_WORLD
    li  t4 1
    shr t3 t4
    inc t1

    li  t4 1
    and t4 t3
    sbo t4 t1 OLD_WORLD
    li  t4 1
    shr t3 t4
    inc t1

    li  t4 1
    and t4 t3
    sbo t4 t1 OLD_WORLD
    li  t4 1
    shr t3 t4
    inc t1

    li  t4 1
    and t4 t3
    sbo t4 t1 OLD_WORLD
    li  t4 1
    shr t3 t4
    inc t1

    li  t4 1
    and t4 t3
    sbo t4 t1 OLD_WORLD
    li  t4 1
    shr t3 t4
    inc t1

    li  t4 1
    and t4 t3
    sbo t4 t1 OLD_WORLD
    li  t4 1
    shr t3 t4
    inc t1

    inc t0
    lt  t1 t2
    jnz load_file_decode_loop

    # copy OLD_WORLD to NEW_WORLD
    la a0 OLD_WORLD
    la a1 NEW_WORLD
    li a2 1440
    sys memcopy

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

            la a2 NEW_WORLD
            jal get_byte_val
        
            # set the color to DARK or WHITE depending on the bit val
            eqi rv 0
            jz write_to_display_set_dark
              seti a0 3
              j write_to_display_set_end
            write_to_display_set_dark:
              seti a0 0
            write_to_display_set_end:
            sys scolor
        
            # restore col, row
            pop a0
        
            # draw a 4x4 square scaled by 2 position lens
            muli a0 4        
            muli a1 4
            seti a2 4
            seti a3 4        
            sys fbrect

            # restore a0 and a1
            divi a0 4
            divi a1 4

            inc a0
            modi a0 40
            jnz write_to_display_col_loop
        inc a1
        modi a1 36
        jnz write_to_display_row_loop
    sys fbflush   # sync the screen
    pop ra
    ret