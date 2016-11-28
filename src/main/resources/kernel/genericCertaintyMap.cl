int constant mask = 0x000000FF;

bool isInRange(int value, int min, int max) {
    return min <= value && value <= max;
}

kernel void genericCertaintyMapKernel(global const int* input, global int* output,
                                int minColor, int maxColor, int numElements) {
    int index = get_global_id(0);
    if (index >= numElements)  {
        return;
    }

    int inputColor = input[index];
    int result = 255;
    for(int i = 0; i < 3; i++) {
        int curInputComponent = (inputColor >> (i * 8)) & mask;
        int curMinComponent = (minColor >> (i * 8)) & mask;
        int curMaxComponent = (maxColor >> (i * 8)) & mask;
        result = isInRange(curInputComponent, curMinComponent, curMaxComponent) ? result : 0;
    }
    output[index] = result;
}

