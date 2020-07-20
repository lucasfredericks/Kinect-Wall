import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.serial.*; 
import com.jogamp.opengl.GL3; 
import com.thomasdiewald.pixelflow.java.DwPixelFlow; 
import com.thomasdiewald.pixelflow.java.flowfieldparticles.DwFlowFieldParticles; 
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture; 
import com.thomasdiewald.pixelflow.java.antialiasing.FXAA.FXAA; 
import com.thomasdiewald.pixelflow.java.imageprocessing.DwFlowField; 
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.*; 
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics; 
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D; 
import com.thomasdiewald.pixelflow.java.utils.DwUtils; 
import processing.core.*; 
import processing.opengl.PGraphics2D; 
import processing.opengl.PGraphicsOpenGL; 
import java.util.Locale; 
import java.security.*; 
import spout.*; 
import com.thomasdiewald.pixelflow.java.fluid.DwFluid2D; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class KinectWall_Core extends PApplet {

 //<>//

// The serial port:
Serial myPort;
int[] serialInArray= new int[2];
int serialCount = 0;
boolean firstContact = false;

/** //<>// //<>// //<>// //<>// //<>//
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */






















float windSpeed;
float rHumidity;
float aHumidity;
float prevAHumidity;
float dewPointC;
float tempF;
float tempC;
float tempK;
float dryAirDensity; 
float waterVaporDensity;
float waterVaporPressure;
float satVaporPressure; 
boolean evaporation;

float transitionFrame;
float transitionTime;




//Gauge rHGauge;
//Gauge aHGauge;
//Gauge vPressGauge;
//Gauge satPressGauge;
//Gauge absHumGauge;
//Gauge dewPointGauge;
ControlPanel controlPanel;


float gravity = 1;
float precipRate;
int windTimer;
boolean rain;
boolean prevRain;
boolean precip;


// some state variables for the GUI/display
int     BACKGROUND_COLOR    = 0;
boolean UPDATE_GRAVITY    = true;
boolean COLLISION_DETECTION = true;
boolean UPDATE_FLUID               = true;
boolean DISPLAY_FLUID_TEXTURES     = true;
boolean DISPLAY_FLUID_VECTORS      = false;
int     DISPLAY_fluid_texture_mode = 0;
//boolean DISPLAY_PARTICLES          = true;

int viewport_w = 1920;
int viewport_h = 1080;
int viewport_x = 0;
int viewport_y = 0;

int fluidgrid_scale = 3;

Spout colorSpout;
Spout depthSpout;

DwFluid2D fluid;
PGraphics2D pg_fluid;
DwFilter filter;

PGraphics depthImage;
PGraphics colorImage;
PGraphics2D depthImage_;
PGraphics2D pg_particles;
PGraphics2D pg_canvas;
PGraphics2D pg_obstacles;
PGraphics2D pg_gravity;
PGraphics2D pg_luminance;

FXAA antialiasing;
PGraphics2D pg_aa;

DwPixelFlow context;

DwFlowFieldParticles particles;
//DwFlowFieldParticles snowParticles;
DwFlowField ff_acc;

DwLiquidFX liquidfx;

//Gauge tempGauge;


PImage snowBuffer;
PImage rainBuffer;

public void settings() {
  //size(viewport_w, viewport_h, P2D);
  fullScreen(P2D);

  smooth(4);
}


public void setup() {
  
  
    // Open the port you are using at the rate you want:
  //printArray(Serial.list());
  myPort = new Serial(this, Serial.list()[0], 9600);

  transitionTime = 80;
  transitionFrame = 0;

  windTimer = 0;
  tempF = 0;
  //tempGauge = new Gauge(100, 300, "temperature");
  //rHGauge = new Gauge(200, 300, "relative humidity");
  //aHGauge = new Gauge(100, 500, "absolute humidity");  
  controlPanel = new ControlPanel();
  aHumidity = 0;
  prevAHumidity = 0;

  rain = true;
  prevRain = rain;

  // main library context
  context = new DwPixelFlow(this);
  context.print();
  context.printGL();

  antialiasing = new FXAA(context);

  colorSpout = new Spout(this);
  colorSpout.createReceiver("colorSpout");
  depthSpout = new Spout(this);
  depthSpout.createReceiver("depthSpout");

  colorImage = createGraphics(width, height, P2D);
  depthImage = createGraphics(width, height, P2D);
  depthImage_ = (PGraphics2D) createGraphics(width, height, P2D);



  surface.setLocation(viewport_x, viewport_y);
  surface.setLocation(viewport_x, viewport_y);
  pg_canvas = (PGraphics2D) createGraphics(width, height, P2D);
  pg_canvas.smooth(0);
  pg_obstacles = (PGraphics2D) createGraphics(width, height, P2D);
  pg_particles = (PGraphics2D) createGraphics(width, height, P2D);
  pg_particles.beginDraw();
  pg_particles.blendMode(MULTIPLY);
  pg_particles.endDraw();

  fluid = new DwFluid2D(context, viewport_w, viewport_h, fluidgrid_scale);

  // set some simulation parameters
  fluid.param.dissipation_density     = 0.99f;
  fluid.param.dissipation_velocity    = 0.99f;
  fluid.param.dissipation_temperature = 0.50f;
  fluid.param.vorticity               = 0.50f;

  // interface for adding data to the fluid simulation
  MyFluidData cb_fluid_data = new MyFluidData();
  fluid.addCallback_FluiData(cb_fluid_data);
  // pgraphics for fluid
  pg_fluid = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
  pg_fluid.smooth(4);
  pg_fluid.beginDraw();
  pg_fluid.background(BACKGROUND_COLOR);
  pg_fluid.endDraw();

  liquidfx = new DwLiquidFX(context);

  //init particles, establish params
  particles = new DwFlowFieldParticles(context, 1024 * 1024);
  //snowParticles = new DwFlowFieldParticles(context, 1024 * 1024);

  setParticles();



  ff_acc = new DwFlowField(context);
  ff_acc.param.blur_iterations = 0;
  ff_acc.param.blur_radius     = 1;

  filter = new DwFilter(context);

  resizeScene();

  frameRate(60);
  surface.setAlwaysOnTop(true);
}

public boolean resizeScene() {
  boolean[] RESIZED = { false };
  pg_canvas     = DwUtils.changeTextureSize(this, pg_canvas, width, height, 0, RESIZED);
  pg_aa         = DwUtils.changeTextureSize(this, pg_aa, width, height, 0, RESIZED);
  pg_particles  = DwUtils.changeTextureSize(this, pg_particles, width, height, 0, RESIZED);
  pg_obstacles  = DwUtils.changeTextureSize(this, pg_obstacles, width, height, 0, RESIZED);
  pg_gravity    = DwUtils.changeTextureSize(this, pg_gravity, width, height, 0, RESIZED);
  pg_fluid      = DwUtils.changeTextureSize(this, pg_fluid, width, height, 0, RESIZED);
  pg_luminance  = DwUtils.changeTextureSize(this, pg_luminance, width, height, 0, RESIZED);


  if (RESIZED[0]) {
    pg_gravity.beginDraw();
    pg_gravity.blendMode(REPLACE);
    pg_gravity.background(0, 255, 0);
    pg_gravity.endDraw();

    setParticleColor(0);
  }
  return RESIZED[0];
}
//////////////////////////////////////////////////////////////////////////////
//
// SCENE
//
//////////////////////////////////////////////////////////////////////////////


int[] BG      = { 0, 0, 0, 0};
int[] FG      = {16, 16, 16, 255};
int[] FG_MOBS = {32, 32, 32, 255};
public void setFill(PGraphicsOpenGL pg, int[] rgba) {
  pg.fill(rgba[0], rgba[1], rgba[2], rgba[3]);
}
public void serialEvent(Serial myPort) {
  int inByte = myPort.read();

  // Add the latest byte from the serial port to array:
  serialInArray[serialCount] = inByte;
  serialCount++;

  // If we have 2 bytes:
  if (serialCount > 1 ) {
    tempF = map(serialInArray[0],0,168,0,100);
    windSpeed = map(serialInArray[1],0,168,-200,200);
    //tempF = serialInArray[1];
    //windSpeed = serialInArray[0];

    // print the values (for debugging purposes only):
    //println(tempF + "\t" + windSpeed + "\t");


    // Reset serialCount:
    serialCount = 0;
  }
}

public void draw() {
  int startTime = millis();
  myPort.write('A');


  //environment variables are calculated with the ideal gas law and magnus-tetens equation.
  tempC = (tempF - 32)*(5.0f / 9.0f);
  tempK = tempC + 273.15f;
  ///aHumidity = map(mouseX, 0, width, 0, 0.046); //kg/m3
  aHumidity = max(aHumidity, 0);
  dryAirDensity = (101325/(287.05f*tempK)); 
  waterVaporDensity = aHumidity/(461.52f*tempK);
  waterVaporPressure =  (aHumidity*461.5f*tempK); //The water vapor partial pressure can be expressed as pw = ρw (461.5 J/kg K) T 
  satVaporPressure = 611.2f * (exp((17.27f*tempC)/(237.3f+tempC)));
  rHumidity = max (0, (waterVaporPressure/satVaporPressure) * 100);  
  float j = log(rHumidity/100) + ((17.27f*tempC)/(237.3f+tempC));
  dewPointC = (237.3f*j)/(17.269f-j);

  if (rHumidity <= 99) {
    precip = false;
    if (tempC > 0) {
      evaporation = true;
      rain = true;
    } else {
      evaporation = true;
    }
  } else {
    if (rHumidity > 99) {
      evaporation = false;
      precip = true;
    }

    if (tempC >= 0) {
      rain = true;
    } else {
      rain = false;
    }
  }
  if (evaporation) {
    aHumidity += .0001f;
    aHumidity = max(0, aHumidity);
  }
  if (precip) {
    aHumidity -= .00005f;
    aHumidity = max(0, aHumidity);
  }
  precip = true;
  if (prevRain != rain || (transitionTime != transitionFrame && transitionTime != 0))  
  {
    setParticles();
  }
  prevRain = rain;


  //variables for debugging:
  //precip = true;
  //rain = false;


  particles.param.timestep = 1f/frameRate;
  //snowParticles.param.timestep = 1f/frameRate;

  //image(pg_obstacles, 0, 0);
  //resizeScene();
  updateScene();
  if (precip) {
    spawnParticles();
  }

  particleSimulation();

  applyLiquidFX();

  // render obstacles + particles
  pg_canvas.beginDraw(); 
  pg_canvas.background(255);
  pg_canvas.image(colorImage, 50, 0);
  //pg_canvas.image(pg_gravity, 0, 0);
  pg_canvas.image(pg_particles, 50, 0);
  pg_canvas.image(pg_fluid, 50, 0);
  pg_canvas.endDraw();

  blendMode(REPLACE);
  image(pg_canvas, 0, 0);
  blendMode(BLEND);

  String txt_fps = String.format(Locale.ENGLISH, "[%s]   [%7.2f fps]   [particles %,d] ", getClass().getSimpleName(), frameRate, particles.getCount() );
  //println(txt_fps);

  image(pg_obstacles, 0, 0, 360, 250);
  //image(colorImage,0,0);

  //tempGauge.drawFrame();
  //tempGauge.updateF1(tempF);
  //rHGauge.updateF1(rHumidity);
  //aHGauge.updateF1(aHumidity);
  //tempGauge.updateF2(max(0, (dewPointC * (9.0/5.0) + 32)));

  //println(frameRate);

  //getContours();

  //checkUpdate();

  controlPanel.display();
  if(particles.getCount()>= 500000){
   particles.reset(); 
  }
  int endTime = millis() - startTime;
  //println("draw thread time elapsed = " + endTime);
}

public void updateScene() {


  colorImage = colorSpout.receiveTexture(colorImage);
  depthImage = depthSpout.receiveTexture(depthImage);

  //pg_fluid.tint(0, map(windSpeed, 0, .5, 0, 255));
  pg_fluid.beginDraw();
  pg_fluid.clear();
  pg_fluid.endDraw();
  // render fluid stuff
  if (DISPLAY_FLUID_TEXTURES) {
    // render: density (0), temperature (1), pressure (2), velocity (3)
    fluid.renderFluidTextures(pg_fluid, DISPLAY_fluid_texture_mode);
  }

  if (DISPLAY_FLUID_VECTORS) {
    // render: velocity vector field
    fluid.renderFluidVectors(pg_fluid, 10);
  }

  antialiasing.apply(pg_canvas, pg_aa);


  if (!rain) {
    DwFilter filter = DwFilter.get(context);
    filter.luminance_threshold.param.threshold = 0f; // when 0, all colors are used
    filter.luminance_threshold.param.exponent  = 7;
    filter.luminance_threshold.apply(pg_aa, pg_luminance);
  }


  pg_obstacles.beginDraw();
  pg_obstacles.clear();
  pg_obstacles.image(depthImage, 0, 0, pg_obstacles.width, pg_obstacles.height);
  filter.gaussblur.apply(pg_obstacles, pg_obstacles, depthImage_, 10, 15);
  pg_obstacles.endDraw();


  fluid.addObstacles(pg_obstacles);
  fluid.update();
  //particles.update(fluid);
}

public void spawnParticles() {

  int maxRate = 12;
  precipRate = map(rHumidity-95, 0, 200, 2, (maxRate*maxRate));
  precipRate = sqrt(precipRate);

  float px, py, vx, vy, radius;
  int count, vw, vh;

  vw = width;
  vh = height;

  //count = int(map(mouseY, 0, height, 0, 3));
  count = 3;
  radius = 250;
  px = random(-500, 2480);
  py = -50;
  vx = 2*windSpeed;
  vy = -100;

  DwFlowFieldParticles.SpawnRadial sr = new DwFlowFieldParticles.SpawnRadial();
  sr.num(PApplet.parseInt(precipRate));
  sr.dim(radius, radius);
  sr.pos(px, vh-1-py);
  sr.vel(vx, vy);

  particles.spawn(vw, vh, sr);

  if (mousePressed) {
    float pr = particles.getCollisionSize() * 0.5f;
    count = ceil(particles.getCount() * 0.01f);
    count = min(max(count, 1), 100);  
    radius = ceil(sqrt(count * pr * pr));
    px = mouseX;
    py = mouseY;
    vx = (mouseX - pmouseX) * +5;
    vy = (mouseY - pmouseY) * -5;
    println(mouseX + ", " + mouseY);

    sr.num(count);
    sr.dim(radius, radius);
    sr.pos(px, vh-1-py);
    sr.vel(vx, vy);
    particles.spawn(vw, vh, sr);
  }
}

public void particleSimulation() {
  // create acceleration texture

  int w = width;
  int h = height;

  ff_acc.resize(w, h);


  {
    float mul_gravity;
    //float mul_windSpeed;
    mul_gravity = UPDATE_GRAVITY ? -gravity/10f : 0;
    //mul_windSpeed = windSpeed/50f;
    //Merge.TexMad ta = new Merge.TexMad(pg_gravity, 0, windSpeed);
    Merge.TexMad tb = new Merge.TexMad(pg_gravity, mul_gravity, 0);
    DwFilter.get(context).merge.apply(ff_acc.tex_vel, tb);
  }

  // resize, create obstacles, update physics
  particles.resizeWorld(w, h);

  particles.createObstacleFlowField(pg_obstacles, BG, true);
  particles.update(ff_acc);
}

public void keyReleased() {
  if (key == 'r') particles.reset();
}

public void applyLiquidFX() {
  pg_particles.beginDraw();
  pg_particles.clear();
  pg_particles.endDraw();
  particles.displayParticles(pg_particles);

  if (rain) {
    liquidfx.param.base_LoD           = 1;
    liquidfx.param.base_blur_radius   = 1;
    liquidfx.param.base_threshold     = .6f;
    liquidfx.param.base_threshold_pow = 25;
    liquidfx.param.highlight_enabled  = true;
    liquidfx.param.highlight_LoD      = 1;
    liquidfx.param.highlight_decay    = .6f;
    liquidfx.param.sss_enabled        = true;
    liquidfx.param.sss_LoD            = 3;
    liquidfx.param.sss_decay          = 0.8f;
    liquidfx.apply(pg_particles);
  }
  if (!rain) {
    liquidfx.param.base_LoD           = 1;
    liquidfx.param.base_blur_radius   = 1;
    liquidfx.param.base_threshold     = .3f;
    liquidfx.param.base_threshold_pow = 7;
    liquidfx.param.highlight_enabled  = false;
    liquidfx.param.highlight_LoD      = 1;
    liquidfx.param.highlight_decay    = 1.9f;
    liquidfx.param.sss_enabled        = true;
    liquidfx.param.sss_LoD            = 3;
    liquidfx.param.sss_decay          = 7.0f;
    liquidfx.apply(pg_particles);
  }
}

public void info() {
  String txt_device = context.gl.glGetString(GL3.GL_RENDERER).trim().split("/")[0];
  String txt_app = getClass().getSimpleName();
  String txt_fps = String.format(Locale.ENGLISH, "[%s]   [%s]   [%d/%d]   [%7.2f fps]   [particles %,d] ", 
    txt_app, txt_device, 
    pg_canvas.width, 
    pg_canvas.height, 
    frameRate, particles.getCount()
    );

  fill(255, 0, 0);
  noStroke();
  rect(0, height, 650, - 20);
  fill(255, 128, 0);
  text(txt_fps, 10, height-6);

  surface.setTitle(txt_fps);
}
public void setParticles() {
  if (!rain) {
    transitionFrame ++;
    transitionFrame = constrain(transitionFrame, 0, transitionTime);
    //transitionFrame = transitionTime;
  }

  if (rain) {
    transitionFrame --;
    transitionFrame = constrain(transitionFrame, 0, transitionTime);
    //particles.displayTrail(pg_particles);
    //transitionFrame = 0;
  }

  particles.param.shader_type = PApplet.parseInt(map(transitionFrame, 0.0f, transitionTime, 0.0f, 0.0f)); 
  particles.param.shader_collision_mult = map(transitionFrame, 0.0f, transitionTime, 0f, 0f);

  particles.param.size_display   = PApplet.parseInt(map(transitionFrame, 0.0f, transitionTime, 11, 12));
  particles.param.size_collision = PApplet.parseInt(map(transitionFrame, 0.0f, transitionTime, 3, 10));
  particles.param.size_cohesion  = PApplet.parseInt(map(transitionFrame, 0.0f, transitionTime, 3, 1));

  particles.param.wh_scale_coh =   PApplet.parseInt(map(transitionFrame, 0.0f, transitionTime, 4, 1));
  particles.param.wh_scale_col =   PApplet.parseInt(map(transitionFrame, 0.0f, transitionTime, 1, 0));
  particles.param.wh_scale_obs =   PApplet.parseInt(map(transitionFrame, 0.0f, transitionTime, 0, 0));

  particles.param.velocity_damping  =map(transitionFrame, 0.0f, transitionTime, .99f, .98f);
  particles.param.display_line_width = map(transitionFrame, 0.0f, transitionTime, 0, 0);
  particles.param.display_line_smooth = true;

  particles.param.mul_coh = map(transitionFrame, 0.0f, transitionTime, 1f, 2.5f);
  particles.param.mul_col = map(transitionFrame, 0.0f, transitionTime, .8f, .8f); 
  particles.param.mul_obs = map(transitionFrame, 0f, transitionTime, 1f, .8f); //
  setParticleColor(0);
}

public void setParticleColor(int val) {
  float r=1f, g=1f, b=1f, a=1f, s=1f;

  float[] ca = particles.param.col_A;

  switch(val) {
  case 0: 
    r = map(transitionFrame, 0.0f, transitionTime, .0f, 10.f); 
    g = map(transitionFrame, 0.0f, transitionTime, .0f, 10.f); 
    b = map(transitionFrame, 0.0f, transitionTime, 1.00f, 10.0f); 
    a = map(transitionFrame, 0.0f, transitionTime, 10.0f, 10.0f); 
    s = map(transitionFrame, 0.0f, transitionTime, .10f, 10.f);  
    break;
  case 1: 
    r = 10.0f; 
    g = 10.0f; 
    b = 10.0f; 
    a = 10.0f; 
    s = 0.25f;  
    break;
  case 2: 
    r = 10.0f; 
    g = 10.0f; 
    b = 10.0f; 
    a = 10.0f; 
    s = 0.25f;  
    break;
  case 3: 
    r = 10.0f; 
    g = 10.0f; 
    b = 10.0f; 
    a = 10.0f; 
    s = 0.25f;  
    break;
  case 4: 
    r = 0.10f; 
    g = 0.10f; 
    b = 0.10f; 
    a = 10.0f; 
    s = 0.25f;  
    break;
  case 5: 
    r = ca[0]; 
    g = ca[1]; 
    b = ca[2]; 
    a =  1.0f; 
    s = 1.00f;  
    break;
  }

  particles.param.col_A = new float[]{ r, g, b, a };
  particles.param.col_B = new float[]{ r*s, g*s, b*s, 0 };
}

class ControlPanel {
  PImage panelMask;
  PImage needle;
  PImage waves;
  PFont digi;
  int gaugeColor = color(0xffb3d237);
  int evapX = 100;
  int evapY = 716;
  int tempX = 103;
  int tempY = 462;
  float lastTemp;

  float lastDP;
  float[] evapRate;
  int evapCount;
  float evap;


  float maxHumidity = 0.045250192f;

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
  public void updateData() {
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


  public void display() {

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
    float dewPoint_ = map(dewPointC, -17.78f, 37.78f, 0, -300);
    rect(-20, 0, 30, tempF_);
    stroke(0, 0, 255);
    strokeWeight(6);
    line(-20, dewPoint_, 20, dewPoint_);
    noStroke();
    popMatrix();


    pushMatrix();
    translate(227, 891);
    fill(gaugeColor);
    int rHumidity_ = PApplet.parseInt(max(map(rHumidity, 0, 100, 0, -258), -255));
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
    rotate(-.25f*PI);
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
    arc(0, 0, windSpeed_, windSpeed_, .9f* PI, 1.1f*PI);
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
  int background = 255;
  int fill1 = color(0, 0, 255);
  int fill2 = color(255, 0, 0);


  Gauge(int x_, int y_, String label_) {
    x = x_;
    y = y_;
    label  = label_;

    drawFrame();
  }

  public void updateF1(float val_) {
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
  public void updateF2(float val_) {
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

  public void drawFrame() {
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
  

private class MyFluidData implements DwFluid2D.FluidData {

  // update() is called during the fluid-simulation update step.
  @Override
    public void update(DwFluid2D fluid) {

    float px, py, vx, vy, radius, vscale, temperature;

    vscale = 100;
    vy     = -10 *  vscale;
    radius = 40;
    px     = width/2;
    py     = height-50;
    // vx     = map(windSpeed, 0, 10, 0, 10);
    vx = 0;

    temperature = 1f;
    //fluid.addDensity(px, py, radius, 0.2f, 0.3f, 0.5f, 1.0f);
    //fluid.addTemperature(px, py, radius, temperature);
    //particles.spawn(fluid, px, py, radius, -100);

    if (evaporation) {

      for ( int i = 0; i < height/radius; i++) {
        fluid.addVelocity(-20, i*radius, radius, vx, 0);
      }

      for (int x = -55; x < width+radius; x+=radius) {
        fluid.addDensity (x, -20, radius, 1.0f, 1.0f, 1.0f, 1.0f, 1);
        fluid.addVelocity(x, -20, radius, 0, 10);
      }
    }

    for (int y = -20; y < height + 20; y+= radius) {
      for (int x = 0; x < width + 1; x += width) {
        fluid.addVelocity(x, y, radius, .1f*windSpeed, 0);
        if (evaporation && y > .75f*width) {
          fluid.addDensity(x, y, radius, 1.0f, 1.0f, 1.0f, 1.0f, 1);
        }
      }
    }
    //}


    //boolean mouse_input = !cp5.isMouseOver() && mousePressed;

    // add impulse: density + velocity, particles
    if (mousePressed && mouseButton== LEFT) {
      radius = 15;
      vscale = 15;
      px     = mouseX;
      py     = height-mouseY;
      vx     = (mouseX - pmouseX) * +vscale;
      vy     = (mouseY - pmouseY) * -vscale;
      fluid.addDensity (px, py, radius, 0.25f, 0.0f, 0.0f, 1.0f, 400);
      fluid.addVelocity(px, py, radius, vx, vy);
      //particles.spawn(fluid, px, py, radius*2, 300);
    }

    // add impulse: density + temperature, particles
    if (mousePressed && mouseButton == CENTER) {
      radius = 15;
      vscale = 15;
      px     = mouseX;
      py     = height-mouseY;
      temperature = 2f;
      fluid.addDensity(px, py, radius, 0.25f, 0.0f, 0.9f, 0);
      fluid.addTemperature(px, py, radius, temperature);
      //particles.spawn(fluid, px, py, radius, 100);
    }

    // particles
    if (mousePressed && mouseButton == RIGHT) {
      px     = mouseX;
      py     = height - 1 - mouseY; // invert
      radius = 50;
      //particles.spawn(fluid, px, py, radius, 300);
    }
  }
}
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
/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




/**
 * 
 * functions for encoding/decoding 2D-velocity
 * 
 * @author Thomas
 *
 */

// namespace Velocity
static public class Velocity{
  
  static final public float TWO_PI = (float) (Math.PI * 2.0f);
  
  // namespace Polar
  static public class Polar{
    
    /**
     * converts an unnormalized vector to polar-coordinates.
     * 
     * @param  vx velocity x, unnormalized
     * @param  vy velocity y, unnormalized
     * @return {arc, mag}
     */
    static public float[] getArc(float vx, float vy){
      // normalize
      float mag_sq = vx*vx + vy*vy;
      if(mag_sq < 0.00001f){
        return new float[]{0,0};
      }
      float mag = (float) Math.sqrt(mag_sq);
      vx /= mag;
      vy /= mag;
      
      float arc = (float) Math.atan2(vy, vx);
      if(arc < 0) arc += TWO_PI;
      return new float[]{arc, mag};
    }
    
    /**
     * encodes an unnormalized 2D-vector as an unsigned 32 bit integer.<br>
     *<br>
     * 0xMMMMAAAA (16 bit arc, 16 bit magnitude<br>
     * 
     * @param x    velocity x, unnormalized
     * @param y    velocity y, unnormalized
      * @return encoded polar coordinates
     */
    static public int encode_vX_vY(float vx, float vy){
      float[] arc_mag = getArc(vx, vy);
      int argb = encode_vA_vM(arc_mag[0], arc_mag[1]);
      return argb;
    }
    
    /**
     * encodes a vector, given in polar-coordinates, into an unsigned 32 bit integer.<br>
     *<br>
     * 0xMMMMAAAA (16 bit arc, 16 bit magnitude<br>
     * 
     * @param vArc
     * @param vMag
     * @return encoded polar coordinates
     */
    static public int encode_vA_vM(float vArc, float vMag){
      float  vArc_nor = vArc / TWO_PI;                           // [0, 1]
      int    vArc_I16 = (int)(vArc_nor * (0xFFFF - 1)) & 0xFFFF; // [0, 0xFFFF[
      int    vMag_I16 = (int)(vMag                   ) & 0xFFFF; // [0, 0xFFFF[
      return vMag_I16 << 16 | vArc_I16;                          // ARGB ... 0xAARRGGBB
    }

    /**
     * decodes a vector, given as 32bit encoded integer (0xMMMMAAAA) to a 
     * normalized 2d vector and its magnitude.
     * 
     * @param rgba 32bit encoded integer (0xMMMMAAAA)
     * @return {vx, vy, vMag}
     */
    static public float[] decode_ARGB(int rgba){
      int   vArc_I16 = (rgba >>  0) & 0xFFFF;            // [0, 0xFFFF[
      int   vMag_I16 = (rgba >> 16) & 0xFFFF;            // [0, 0xFFFF[
      float vArc     = TWO_PI * vArc_I16 / (0xFFFF - 1); // [0, TWO_PI]
      float vMag     = vMag_I16;
      float vx       = (float) Math.cos(vArc);
      float vy       = (float) Math.sin(vArc);
      return new float[]{vx, vy, vMag}; 
    }
  }
  
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "--present", "--window-color=#666666", "--hide-stop", "KinectWall_Core" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
