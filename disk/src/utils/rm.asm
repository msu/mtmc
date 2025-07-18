.data
   error: "UNABLE TO DELETE\n"

.text
main:
  sys   dfile
  eqi   rv  0
  jnz   done
  
  li    a0  error
  li    a1  256
  sys   wstr
  
  done:
    sys exit

