// Pointer basics: &, *, and dereferencing
int x;
int main() {
    x = 42;
    int* p;
    p = &x;
    int y;
    y = *p;
    *p = 100;
    print_int(x);
    return 0;
}
