int constant mask = 0x000000FF;

int imax(int a, int b) {
    return a > b ? a : b;
}

int imin(int a, int b) {
    return a < b ? a : b;
}

int iabs(int a) {
    return a >= 0 ? a : -a;
}

int getRgbPixel(int hue, int saturation, int value) {
    int red;
    int green;
    int blue;
    if(saturation == 0) {
        red = value;
        green = value;
        blue = value;
    } else {
        int region = hue / 30;
        int regionReminder = hue % 30;
        int vs = value * saturation / 255;
        int p = value - vs;
        int q = value - vs * regionReminder / 30;
        int t = value - vs * (30 - regionReminder) / 30;
        switch(region) {
            case 0:
                red = value;
                green = t;
                blue = p;
                break;
            case 1:
                red = q;
                green = value;
                blue = p;
                break;
            case 2:
                red = p;
                green = value;
                blue = t;
                break;
            case 3:
                red = p;
                green = q;
                blue = value;
                break;
            case 4:
                red = t;
                green = p;
                blue = value;
                break;
            case 5:
                red = value;
                green = p;
                blue = q;
                break;
        }
    }
    return (int) red << 16 | green << 8 | blue;
}

kernel void hsvToRgbKernel(global const int* hsv, global int* rgb, int numElements) {
    int index = get_global_id(0);
    if (index >= numElements)  {
        return;
    }

    int color = hsv[index];
    int hue = color >> 16 & mask;
    int saturation = color >> 8 & mask;
    int value = color & mask;

    rgb[index] = getRgbPixel(hue, saturation, value);
}