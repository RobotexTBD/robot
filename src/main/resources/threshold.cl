kernel void thresholdKernel(global const int* input, global int* output, int min, int max, int numElements) {
    int index = get_global_id(0);
    if (index >= numElements)  {
        return;
    }
    output[index] = input[index] >= min && input[index] <= max ? 1 : 0;
}

