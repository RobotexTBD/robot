precision mediump float;

varying vec2 position;
uniform float time;
uniform vec2 resolution;

const float PI = 3.14159;
const float ORANGE_HUE = 14.0;


bool isnan( float val )
{
  return ( val < 0.0 || 0.0 < val || val == 0.0 ) ? false : true;
  // important: some nVidias failed to cope with version below.
  // Probably wrong optimization.
  /*return ( val <= 0.0 || 0.0 <= val ) ? false : true;*/
}

float hueDistance(float hue1, float hue2) {
  if(isnan(hue1)) {
     return 2.0 * PI;
  }
  return min(abs(hue1 - hue2), min(hue1, hue2) + 2.0 * PI - hue2);
}

float limit(float minValue, float maxValue, float value) {
  return max(min(maxValue, value), minValue);
}

void main() {
  float hue = 0.4;
  float saturation = position.x / resolution.x;
  float value = position.y / resolution.y;
  
 
  float expectedHueDistance = radians(pow(10.0 - value / 25.5, 2.25));
  float hueDistance = hueDistance(hue, ORANGE_HUE);
  float hueError = abs(hueDistance - expectedHueDistance);
  float hueCertainty = max(1.0 - pow(hueError * 2.0, 2.25), 0.0);
  float saturationCertainty = limit(0.0, 1.0, 4.0 * saturation - 1.5);
  float valueCertainty = limit(0.0, 1.0, 4.0 * saturation - 1.0);
  float hueDistanceCertainty = max(1.0 - pow(hueDistance, 2.25) * 1.5, 0.0);
  float certaintyValue = hueCertainty * saturationCertainty * valueCertainty * hueDistanceCertainty;
  
  
  gl_FragColor.r = certaintyValue;
  gl_FragColor.g = certaintyValue;
  gl_FragColor.b = certaintyValue;
  gl_FragColor.a = 1.0;
}