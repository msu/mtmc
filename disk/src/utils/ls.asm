.data
   count: -1
   path: -1
   newline: "\n"
   slash: "/"
   invalid_dir: "INVALID DIRECTORY"

file:
   # Output Directory Entry
   file_flags: 0
   file_name_size: 256
   file_name: .byte 256

dir:
   # Input Directory
   dir_name_size: 256
   dir_name: .byte 256


.text
main:
  # Check if a path was passed
  eqi a0  0
  sw  a0  path
  jnz get_cwd

  # Copy the passed in name to dir_name
  mov t0  a0
  li  t1  dir_name
  lw  t2  dir_name_size

loop_dir_load:
  sw  t0  path
  lbo t3  t0  0
  sbo t3  t1  0

  inc t0
  inc t1
  dec t2

  # NULL terminator found
  eqi t3  0
  jnz start

  # Check max size
  eqi t2  0
  jz  loop_dir_load

  j   start

get_cwd:
  # Get current working directory
  li  a0  dir_name
  lw  a1  dir_name_size

  sys cwd

start:
  li  a0  dir_name
  li  a1  0
  
  sys dirent

  sw  rv  count
  lw  t0  count
  li  t1  -1
  eq  t0  t1
  jnz print_invalid_dir

  # If no files, jump to end
  eqi t0  0
  jnz done

  # loop over files
  li  t0  0

loop_file_list:
  li  a0 dir_name
  li  a1  1
  mov a2  t0
  li  a3  file

  sys dirent

  li  a0  file_name
  sys wstr

  # Print / if directory
  li  t2  1
  lw  t3  file_flags
  eq  t2  t3
  jz  print_newline

  li  a0  slash
  sys wstr

print_newline:
  li  a0  newline
  sys wstr
    
  inc t0
  lw  t1  count
  eq  t0  t1
  jz  loop_file_list
  
  j   done

print_invalid_dir:
  li  a0  invalid_dir
  li  a1  256
  li  t3  invalid_dir
  sys wstr
  
done:
  sys exit
