.data
   hello_world: "Hello World!"

.text
main:
  ldi t0, hello_world
  mv a0, t0
  syscall wstr
  syscall exit
