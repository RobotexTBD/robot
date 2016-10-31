kernel void testKernel(global const int* input, global int* output, int numElements) {

    // get index into global data array
    int index = get_global_id(0);

    // bound check, equivalent to the limit on a 'for' loop
    if (index >= numElements)  {
        return;
    }

    // add the vector elements
    output[index] = input[index] * 3;
}