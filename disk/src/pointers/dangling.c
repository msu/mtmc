// Dangling pointer: returning address of a local
int* make_number() {
    int x;
    x = 42;
    return &x;
}

int clobber() {
    int a;
    a = 999;
    return a;
}

int main() {
    int* p;
    p = make_number();
    int val1;
    val1 = *p;
    print_int(val1);
    print_char('\n');
    clobber();
    int val2;
    val2 = *p;
    print_int(val2);
    return 0;
}
