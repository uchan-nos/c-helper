void f(int x)
{
    int y = x;
    int z = 1;

    while (y > 1) {
        z = z * y;
        y = y - 1;
    }

    y = 0;
}
