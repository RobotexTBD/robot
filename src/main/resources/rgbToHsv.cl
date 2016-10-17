int constant mask = 0x000000FF;

int imax(int a, int b) {
    return a > b ? a : b;
}

int imin(int a, int b) {
    return a < b ? a : b;
}

int getHsvPixel(int red, int green, int blue) {
    int max = imax(red, imax(green, blue));
    int min = imin(red, imin(green, blue));
    float delta = max - min;

    float hue;
    int saturation;
    int value = max;
    if(max == 0) {
        hue = 0; //undefined, but shouldn't matter
        saturation = 0;
    } else {
        saturation = 255 * delta / max;
        float hueMultiplier = 30 / delta;
        if(red == max) {
            hue = (green - blue) * hueMultiplier;
        } else if(green == max) {
            hue = 60.0f + (blue - red) * hueMultiplier;
        } else {
            hue = 120.0f + (red - green) * hueMultiplier;
        }
    }
    if(hue < 0) {
        hue += 180.0f;
    }
    return (int) hue << 16 | saturation << 8 | value;
}

kernel void rgbToHsvKernel(global const int* rgb, global int* hsv, int numElements) {
    int index = get_global_id(0);
    if (index >= numElements)  {
        return;
    }

    int color = rgb[index];
    int red = color >> 16 & mask;
    int green = color >> 8 & mask;
    int blue = color & mask;

    hsv[index] = getHsvPixel(red, green, blue);
}