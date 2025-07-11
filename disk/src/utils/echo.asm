.data
NEW_LINE: "\n"

.text

neqi a0 0
jz no_string
# echo the string passed in as an arg in a0
sys wstr

no_string:
# write a newline
la a0 NEW_LINE
sys wstr

# exit
sys exit
