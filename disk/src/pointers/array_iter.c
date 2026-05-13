// Array iteration with index
int arr[5];
int main() {
    arr[0] = 10;
    arr[1] = 20;
    arr[2] = 30;
    arr[3] = 40;
    arr[4] = 50;
    int i;
    i = 0;
    while (i < 5) {
        print_int(arr[i]);
        print_char(' ');
        i = i + 1;
    }
    print_char('\n');
    return 0;
}
