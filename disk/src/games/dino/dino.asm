.text
main:
  li sp, 0x8000        # Set up stack pointer if required by your system

  li a0, rex_buffer    # destination buffer
  li a1, 1024          # max bytes to read
  li a2, rex_path      # path to rex.png
  sys 0x10             # rfile syscall

  teq rv, 0
  jne file_error

  li t0, 78            # rex x position (col)
  li t1, 68            # rex y position (row)

loop:
  sys fbreset          # clear the screen

  mov a0, rex_buffer   # pointer to image data
  mov a1, t0           # x position
  mov a2, t1           # y position
  sys 0x50             # drawimg

  sys fbflush          # update screen

  li a0, 100           # sleep 100 ms
  sys sleep

  j loop               # loop forever

file_error:
  li a0, err_msg
  sys 0x06             # wstr
  sys 0x00             # exit

rex_path:
  .string "src/games/dino/rex.png"

err_msg:
  .string "Error loading rex.png\n"

rex_buffer:
  .space 1024          # buffer for image
