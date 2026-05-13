// NULL pointer check
int main() {
    int* p;
    p = 0;
    if (p != 0) {
        print_int(*p);
    }
    print_string("safe");
    return 0;
}
