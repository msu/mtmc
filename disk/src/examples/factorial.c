// Compute factorial iteratively
int factorial(int n) {
    int result;
    result = 1;
    while (n > 1) {
        result = result * n;
        n = n - 1;
    }
    return result;
}

int main() {
    int f;
    f = factorial(5);
    print_int(f);
    print_char(10);
    return 0;
}
