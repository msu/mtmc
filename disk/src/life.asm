.data
INIT_FILE: "/gol/gun.gol"
OLD: byte[512]
NEW: byte[512]

.text
# load the init file into the bit map
li a0 FRAME_BUFFER
li a1 INIT_FILE
sys rfile
