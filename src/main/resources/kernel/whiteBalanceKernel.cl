int constant mask = 0x000000FF;

int imax(int a, int b) {
    return a > b ? a : b;
}

int imin(int a, int b) {
    return a < b ? a : b;
}

int iu8(int a) {
    return min(255, max(0, a));
}

int whiteBalance(int colorIn, int white, int black) {
    int colorOut = 0;
    for(int i = 0; i < 3; i++) {
        int curColorIn = colorIn >> (i * 8) & mask;
        int curWhite = white >> (i * 8) & mask;
        int curBlack = black >> (i * 8) & mask;
        int colorWidth = curWhite - curBlack;
        int curColorOut = iu8((curColorIn - curBlack) * 255 / colorWidth);
        colorOut |= curColorOut << (i * 8);
    }
    return colorOut;
}

kernel void whiteBalanceKernel(global const int* colorIn, global int* colorOut,
                           int white, int black, int numElements) {
    int index = get_global_id(0);
    if (index >= numElements)  {
        return;
    }
    colorOut[index] = whiteBalance(colorIn[index], white, black);
}