int constant mask = 0x000000FF;
int constant ORANGE_HUE = 11;

int imax(int a, int b) {
    return a > b ? a : b;
}

int imin(int a, int b) {
    return a < b ? a : b;
}

int isquare(int value) {
    return value * value;
}

float limit(float value) {
    return fmax(0.0f, fmin(1.0f, value));
}

int getHueDistance(int hue1, int hue2) {
    int minHue = imin(hue1, hue2);
    int maxHue = imax(hue1, hue2);
    return imin(maxHue - minHue, minHue + 180 - maxHue);
}

float getHueCertainty(float hueDistance) {
    return 1.0f - (isquare(hueDistance) / 180.0f);
}

float getSaturationCertainty(float saturation) {
    return 1.0f - (isquare(255 - saturation) / 50000.0f);
}

float getValueCertainty(float value) {
    return 1.0f - (isquare(255 - value) / 50000.0f);
}

int calculateCertainty(int hue, int saturation, int value) {
    float result = 1.0f;
    int hueDistance = getHueDistance(hue, ORANGE_HUE);
    result *= limit(getHueCertainty(hueDistance));
    result *= limit(getSaturationCertainty(saturation));
    result *= limit(getValueCertainty(value));
    return (int) (result * 255.0f);
}

kernel void ballCertaintyKernel(global const int* input, global int* output, int numElements) {
    int index = get_global_id(0);
    if (index >= numElements)  {
        return;
    }

    int color = input[index];
    int hue = (color >> 16) & mask;
    int saturation = (color >> 8) & mask;
    int value = color & mask;

    output[index] = calculateCertainty(hue, saturation, value);
}

