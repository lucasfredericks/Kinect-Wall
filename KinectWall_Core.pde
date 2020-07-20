import processing.serial.*; //<>//

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
  fluid.param.dissipation_density     = 0.99;
  fluid.param.dissipation_velocity    = 0.99;
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
void setFill(PGraphicsOpenGL pg, int[] rgba) {
  pg.fill(rgba[0], rgba[1], rgba[2], rgba[3]);
}
void serialEvent(Serial myPort) {
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
  tempC = (tempF - 32)*(5.0 / 9.0);
  tempK = tempC + 273.15;
  ///aHumidity = map(mouseX, 0, width, 0, 0.046); //kg/m3
  aHumidity = max(aHumidity, 0);
  dryAirDensity = (101325/(287.05*tempK)); 
  waterVaporDensity = aHumidity/(461.52*tempK);
  waterVaporPressure =  (aHumidity*461.5*tempK); //The water vapor partial pressure can be expressed as pw = ρw (461.5 J/kg K) T 
  satVaporPressure = 611.2 * (exp((17.27*tempC)/(237.3+tempC)));
  rHumidity = max (0, (waterVaporPressure/satVaporPressure) * 100);  
  float j = log(rHumidity/100) + ((17.27*tempC)/(237.3+tempC));
  dewPointC = (237.3*j)/(17.269-j);

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
    aHumidity += .0001;
    aHumidity = max(0, aHumidity);
  }
  if (precip) {
    aHumidity -= .00005;
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
  sr.num(int(precipRate));
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

void applyLiquidFX() {
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

void info() {
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
void setParticles() {
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

  particles.param.shader_type = int(map(transitionFrame, 0.0, transitionTime, 0.0, 0.0)); 
  particles.param.shader_collision_mult = map(transitionFrame, 0.0, transitionTime, 0f, 0f);

  particles.param.size_display   = int(map(transitionFrame, 0.0, transitionTime, 11, 12));
  particles.param.size_collision = int(map(transitionFrame, 0.0, transitionTime, 3, 10));
  particles.param.size_cohesion  = int(map(transitionFrame, 0.0, transitionTime, 3, 1));

  particles.param.wh_scale_coh =   int(map(transitionFrame, 0.0, transitionTime, 4, 1));
  particles.param.wh_scale_col =   int(map(transitionFrame, 0.0, transitionTime, 1, 0));
  particles.param.wh_scale_obs =   int(map(transitionFrame, 0.0, transitionTime, 0, 0));

  particles.param.velocity_damping  =map(transitionFrame, 0.0, transitionTime, .99f, .98);
  particles.param.display_line_width = map(transitionFrame, 0.0, transitionTime, 0, 0);
  particles.param.display_line_smooth = true;

  particles.param.mul_coh = map(transitionFrame, 0.0, transitionTime, 1f, 2.5f);
  particles.param.mul_col = map(transitionFrame, 0.0, transitionTime, .8f, .8f); 
  particles.param.mul_obs = map(transitionFrame, 0f, transitionTime, 1f, .8f); //
  setParticleColor(0);
}

public void setParticleColor(int val) {
  float r=1f, g=1f, b=1f, a=1f, s=1f;

  float[] ca = particles.param.col_A;

  switch(val) {
  case 0: 
    r = map(transitionFrame, 0.0, transitionTime, .0f, 10.f); 
    g = map(transitionFrame, 0.0, transitionTime, .0f, 10.f); 
    b = map(transitionFrame, 0.0, transitionTime, 1.00f, 10.0f); 
    a = map(transitionFrame, 0.0, transitionTime, 10.0f, 10.0f); 
    s = map(transitionFrame, 0.0, transitionTime, .10f, 10.f);  
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
