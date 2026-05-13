// while loop: sum 1 to N
int main() {
    int i;
    int sum;
    i = 0;
    sum = 0;
    while (i < 5) {
        sum = sum + i;
        i = i + 1;
    }
    print_int(sum);
    print_char(10);
    return 0;
}