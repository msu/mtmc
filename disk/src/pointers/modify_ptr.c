// Modifying a variable through a pointer
int x;
int set_to_99(int* p) {
    *p = 99;
    return 0;
}

int main() {
    x = 10;
    print_int(x);
    print_char('\n');
    set_to_99(&x);
    print_int(x);
    return 0;
}
