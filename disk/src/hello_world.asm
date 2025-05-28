.data
   hello_world: "Hello World!"

.text
main:
  li a0 hello_world
  sys wstr
  sys exit
