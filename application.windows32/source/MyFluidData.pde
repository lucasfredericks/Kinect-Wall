import com.thomasdiewald.pixelflow.java.fluid.DwFluid2D;  

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
        fluid.addVelocity(x, y, radius, .1*windSpeed, 0);
        if (evaporation && y > .75*width) {
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
