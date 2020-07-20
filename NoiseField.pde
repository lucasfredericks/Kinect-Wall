//public class NoiseField {
//  float increment = 0.01;
//  // The noise function's 3rd argument, a global variable that increments once per cycle
//  float zoff = 0.0;  
//  // We will increment zoff differently than xoff and yoff
//  float zincrement = 0.02; 
//  PGraphics2D noiseBuffer;
//  float xoff;
//  float yoff;


//  NoiseField() {
//    noiseBuffer = (PGraphics2D) createGraphics(width, height);
//    xoff = 0;
//  }
//  void update() {
//    for (int x = 0; x < noiseBuffer.width; x++) {
//      xoff += increment;   // Increment xoff 
//      yoff = 0.0;   // For every xoff, start yoff at 0
//      for (int y = 0; y < noiseBuffer.height; y++) {
//        yoff += increment; // Increment yoff

//        // Calculate noise and scale by 255
//        float bright = noise(xoff, yoff, zoff)*255;

//        // Try using this line instead
//        //float bright = random(0,255);

//        // Set each pixel onscreen to a grayscale value
//        noiseBuffer.pixels[x+y*noiseBuffer.width] = color(bright, bright, bright);
//      }
//    }
//    updatePixels();

//    zoff += zincrement; // Increment zoff
//  }
//}
