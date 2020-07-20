class Gauge {
  int x; 
  int y;
  int max = 100;
  int min = 0;
  int w = 20;
  int h = 100;
  String label;
  float val1;
  float val2;
  color background = 255;
  color fill1 = color(0, 0, 255);
  color fill2 = color(255, 0, 0);


  Gauge(int x_, int y_, String label_) {
    x = x_;
    y = y_;
    label  = label_;

    drawFrame();
  }

  void updateF1(float val_) {
    val1 = val_;
    drawFrame();
    noStroke();
    fill(fill1);
    pushMatrix();
    translate(x, y);
    translate(0, max-val1);
    rect(0, 0, w, val1);    

    popMatrix();
  }
  void updateF2(float val_) {
    val2 = val_;
    stroke(fill2);
    strokeWeight(2);
    pushMatrix();
    translate(x, y);
    translate(0, max-val2);
    line(0, 0, w, 0);
    translate(w, 0);
    textSize(12);
    //stroke(0);
    text("Dew Point", 0, 0);
    popMatrix();
  }

  void drawFrame() {
    pushMatrix();
    translate(x, y);
    rectMode(CORNER); 
    fill(background);
    stroke(0);
    strokeWeight(2);
    rect(0, 0, w, h);
    translate(0, h + 10);
    //textMode(CENTER);
    stroke(color(255));
    text(label, 0, 0);
    popMatrix();
  }
}
