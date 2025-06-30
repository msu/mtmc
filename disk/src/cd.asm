.data
   invalid_dir: "INVALID DIRECTORY"

.text
main:
  li    a1  256

  sys   chdir
  eqi   rv  0
  jnz   done
  
  li    a0  invalid_dir
  li    a1  256
  sys   wstr
  
  done:
    sys exit

