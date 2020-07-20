class ControlPanel {
  PImage panelMask;
  PImage needle;
  PImage waves;
  PFont digi;
  color gaugeColor = color(#b3d237);
  int evapX = 100;
  int evapY = 716;
  int tempX = 103;
  int tempY = 462;
  float lastTemp;

  float lastDP;
  float[] evapRate;
  int evapCount;
  float evap;


  float maxHumidity = 0.045250192;

  ControlPanel() {
    panelMask = loadImage("panelMask.png");
    needle = loadImage("needle.png");
    waves = loadImage("waves.png");
    String[] fontList = PFont.list();
    printArray(fontList);
    digi = createFont("DS-DIGI.TTF", 64);
    textFont(digi);

    lastDP = dewPointC;
    lastTemp = tempF;

    evapRate = new float[81];
    for (int i = 0; i < evapRate.length; i++) {
      evapRate[i] = 0;
    }
    evapCount = 0;
    maxHumidity = 0;
  }
  void updateData() {
    if (evaporation) {
      evapRate[evapCount] = 1;
    } else {
      evapRate[evapCount] = 0;
    }
    evapCount++;

    if (evapCount >= evapRate.length) {
      evapCount = 0;
    }
    evap = 0;
    for (int i = 0; i < evapRate.length; i++) {
      evap += evapRate[i];
    }
    evap = evap/evapRate.length;
  }


  void display() {

    updateData();

    //evaporation gauge:
    imageMode(CORNER);
    image(waves, 0, 0, 350, 1080);
    noStroke();
    fill(gaugeColor);
    arc(evapX, evapY, 200, 200, PI, map(evap, 0, 1, PI, 2*PI));

    pushMatrix();
    translate(evapX, 500);
    popMatrix();

    pushMatrix();
    translate(102, 462);
    rectMode(CORNERS);
    float tempF_ = map(min(tempF, lastTemp), 0, 100, 0, -300);
    lastTemp = tempF;
    float dewPoint_ = map(dewPointC, -17.78, 37.78, 0, -300);
    rect(-20, 0, 30, tempF_);
    stroke(0, 0, 255);
    strokeWeight(6);
    line(-20, dewPoint_, 20, dewPoint_);
    noStroke();
    popMatrix();


    pushMatrix();
    translate(227, 891);
    fill(gaugeColor);
    int rHumidity_ = int(max(map(rHumidity, 0, 100, 0, -258), -255));
    //println(rHumidity);
    rect(0, 0, 60, rHumidity_);    
    if (rHumidity>=96) {
      stroke(0, 0, 255);
      strokeWeight(8);
      line(0, -252, 60, -252);
    }
    noStroke();

    popMatrix();


    image(panelMask, 0, 0, 350, 1080);


    pushMatrix();
    imageMode(CENTER);
    translate(evapX, evapY);
    rotate(-.25*PI);
    rotate(map(evap, 0, 1, 0, PI));
    image(needle, 0, 0, 110, 110);
    popMatrix();
    imageMode(CORNER);

    pushMatrix();
    translate(20, 890);
    textFont(digi);
    textAlign(LEFT, BASELINE);
    String dewPointStr = (nfs(min(lastDP, dewPointC), 2, 1)+ " C");
    text(dewPointStr, 0, 0);
    lastDP = dewPointC;

    popMatrix();

    pushMatrix();
    translate(258, 399);
    float windSpeed_ = map(windSpeed, -200, 200, 126, -126);
    //println(windSpeed);
    //if (windSpeed > .5*width) {
    arc(0, 0, windSpeed_, windSpeed_, .9* PI, 1.1*PI);
    popMatrix();

    pushMatrix();
    translate(tempX + 15, tempY+dewPoint_);
    fill(255, 255, 255);
    stroke(0, 0, 0);
    strokeWeight(2);
    textFont(digi);
    textSize(18);
    textAlign(LEFT, CENTER);
    //text(dewPointStr, 0, 0);
    text("Dew Point", 0, 0);
    noStroke();
    popMatrix();
  }
}
