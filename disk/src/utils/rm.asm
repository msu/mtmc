.data
   error: "UNABLE TO DELETE\n\nDouble check the filename. If it is a directory, make sure it is empty."

.text
main:
  li    a1  256

  sys   dfile
  eqi   rv  0
  jnz   done
  
  li    a0  error
  li    a1  256
  sys   wstr
  
  done:
    sys exit

