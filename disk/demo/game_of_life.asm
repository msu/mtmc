#TODO - incomplete

.data
INIT_FILE: "init.gol"
pBIT_MAP: 0

# allocate a 512 byte buffer for a bit map
ldi t0, pBIT_MAP
sw  bp, t0, zero
ldi t0, 512
add bp, t0

# load the init file into the bit map
ldi t0, pBIT_MAP
lw t0, t0, zero
mv a0, t0
ldi t0, 512
mv a1, t0
ldi t0, INIT_FILE
mv a2, t0
syscall rfile

# copy bits to display
blit_to_display:
ldi t1 0
loop_1: ldi t0 4096
      lt t0 t1
      bz end_2
      mv t2, t1
      mv t3, t1
      ldi t0, 64
      div t2, t0
      mod t3, t0
      mv a0, t1
      mv a1, t2
      mv a2, t3
      push fp:t1
      call display_bit
      pop fp:t1
      add t1, one
      j loop_1
end_1:

# compute next gen
ldi t1 0
loop_2: ldi t0 4096
        lt t0 t1
        bz end_2
        mv t2, t1
        mv t3, t1
        ldi t0, 64
        div t2, t0
        mod t3, t0
        mv a0, t1
        mv a1, t2
        mv a2, t3
        push fp:t1
        call update_bit
        pop fp:t1
        ldi t0, 1
        add t1, t0
        j loop_1
end_2:
j blit_to_display

display_bit:
  ldi t0 8
  mv t1, a0
  div t1, t0
  ldi t0, pBIT_MAP
  lw t0, t0, zero
  lb t0, t0, t1
  push t0
  mv t1, a0
  ldi t0 8
  mod t1, t0
  ldi t0, 1
  shl t0, t1
  push t0
  sand
  pop t0
  mv a0, a1
  mv a1, a2
  bz turn_off
    ldi t0, 3
    mv a3, t0
    syscall fbset
    ret
  turn_off:
    ldi t0, 0
    mv a3, t0
    syscall fbset
    ret
