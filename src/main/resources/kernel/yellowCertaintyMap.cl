int constant mask = 0x000000FF;
int constant YELLOW_HUE = 25;

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

int iabs(int a) {
    return a > 0 ? a : -a;
}

int ilimit(int value, int minValue, int maxValue) {
    return imin(maxValue, imax(minValue, value));
}

int igapDistance(int value, int minValue, int maxValue) {
    return iabs(value - ilimit(value, minValue, maxValue));
}

int getHueDistance(int hue1, int hue2) {
    int minHue = imin(hue1, hue2);
    int maxHue = imax(hue1, hue2);
    return imin(maxHue - minHue, minHue + 180 - maxHue);
}

float getHueCertainty(float hueDistance) {
    return 1.0f - (isquare(hueDistance) / 180.0f) * 10.0f;
}

float getSaturationCertainty(float saturation) {
    return 1.0f - isquare(igapDistance(saturation, 245, 255)) / 1000.0f;
}

float getValueCertainty(float value) {
    return 1.0f - isquare(igapDistance(value, 180, 200)) / 1000.0f;
}

int calculateCertainty(int hue, int saturation, int value) {
    float result = 1.0f;
    int hueDistance = getHueDistance(hue, YELLOW_HUE);
    result *= limit(getHueCertainty(hueDistance));
    result *= limit(getSaturationCertainty(saturation));
    result *= limit(getValueCertainty(value));
    return (int) (result * 255.0f);
}

kernel void yellowCertaintyKernel(global const int* input, global int* output, int numElements) {
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

