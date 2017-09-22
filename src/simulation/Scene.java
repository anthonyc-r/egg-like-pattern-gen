/**
* Author: Anthony Cohn-Richardby
*
**/
package simulation;

import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.awt.*;
import com.jogamp.opengl.util.texture.*;
import com.jogamp.opengl.util.texture.awt.*;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.awt.GLCanvas;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.VectorUtil;

import java.awt.image.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D;
import java.awt.BasicStroke;

import java.util.Random;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;

import javax.imageio.*;

public class Scene {
	public static final int TEXTURE_WIDTH = 512;
	public static final int TEXTURE_HEIGHT = 512;
	//Defined in the uv generating function of Egg.java as 55.
	public static final int SPHERE_RADIUS = 42;
	//Materials
	public static final float[] WHITE = new float[]{1.0f, 1.0f, 1.0f};
	public static final float[] RED = new float[]{1.0f, 0f, 0f};

    private GLU glu = new GLU();
    private GLUT glut = new GLUT();

	private Egg egg;
	private Inkjet[] inkjets;
    private ArrayList<Inkjet> activeInkjets;
    private Camera camera;
	private RotationHandler rotH;
	
	private Random rand = new Random();
	
	//Simulation variables
	//current time in seconds from animation start
	private double time;
	//time in percent completion
	private double pcTime;
	//simulation duration
	private double duration;
	//Current percentage through the layer 0.0-1.0
	private float layer;
	private boolean simStarted;
	private boolean displayInkjets;
	//Current line color
	private Color lineColor;
	
	private Texture eggTexture;
	private BufferedImage image;
	private Color paintColor;
	private Graphics2D graphics;

    
	/**
	* Constructor 
	* Initialize scene objects and texture image
	**/
    public Scene(GL2 gl, Camera camera) {
		this.camera = camera;
		egg = new Egg();
		rotH = new RotationHandler("rotations.xml");
        activeInkjets = new ArrayList<Inkjet>();
		time = 0;
		duration = 10;
		layer = 1.0f;
		simStarted = false;
		displayInkjets = true;
		inkjets = new Inkjet[0];
		initImage(255, 255, 255);
		initTexture(gl);
    }

	/**
	* update
	* Update callback, updates simulation variables, checks and draws collisions,
	* and updates the texture.
	**/
    public void update(GL2 gl) {
		//Check if the end time is reached
		if(time >= duration){
			simStarted = false;
		}
		//Update time according to framerate of 30FPS Only if the sim has been started.
		if(simStarted){
			time += 1/30.0;
		}
		//Else it must have been stopped, time needs resetting.
		else{
			time = 0;
            resetInkjets();
		}

		//layer starts from 1f and goes to 0f as time goes 0f-1f. (used to set color of inkj)
		pcTime = time/duration;
		layer = 1f - (float) pcTime;
		updateActiveInkjets();
		rotH.update(pcTime);
		egg.setRotation(rotH.getRotation());
		//Paint on the backing image squares at the points of intersection & update the texture.
		paintIntersections();
		updateTexture(gl);
    }

	/**
	* render
	* The render callback, sets camera, draws axes and egg.
	**/
    public void render(GL2 gl) {
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();
        camera.view(glu);
        drawAxes(gl, 60);
		
		drawInkjets(gl);
		
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, WHITE, 0);
		egg.draw(gl);
    }

    /**
	* drawAxes
    * Draws some simple line axes in order to aid testing.
	* @param gl The OpenGL2 context.
	* @param length The length of the axis.
    */
    private void drawAxes(GL2 gl, double length) {
		gl.glDisable(GL2.GL_LIGHTING);
        gl.glLineWidth(4);
        gl.glBegin(GL2.GL_LINES);
		  //x
          gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, new float[]{1f, 0f, 0f}, 0);
          gl.glColor3d(1, 0, 0);
          gl.glVertex3d(0, 0, 0);
          gl.glVertex3d(length, 0, 0);
		  
		  //y
		  gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, new float[]{0f, 1f, 0f}, 0);
		  gl.glColor3d(0, 1, 0);
		  gl.glVertex3d(0, 0, 0);
		  gl.glVertex3d(0, length, 0);
		  
		  //z
		  gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, new float[]{0f, 0f, 1f}, 0);
		  gl.glColor3d(0, 0, 1);
		  gl.glVertex3d(0, 0, 0);
		  gl.glVertex3d(0, 0, length);
        gl.glEnd();
        gl.glLineWidth(1);
		gl.glEnable(GL2.GL_LIGHTING);
    }
	
	/**
	* resetScene
	* Returns the scene to it's original state
	**/
	public void resetScene() {
		initImage(255, 255, 255);
	}
	
	/*=======================BATCH INKJET PROCESSING====================================*/
		
    /**
    * resetInkjets
    * reset the value of the inkjets lastintersect.
    **/
    public void resetInkjets() {
        for(Inkjet inkjet : inkjets){
            inkjet.resetLastIntersect();
        }
    }
    /**
    * updateActiveInkjets
    * Update the list of inkjets currently active (time is between their start and end times.
    **/
    public void updateActiveInkjets() {
        activeInkjets.clear();
        for(Inkjet inkjet : inkjets){
            if(pcTime >= inkjet.getStartTime() && pcTime < inkjet.getEndTime()){
               activeInkjets.add(inkjet); 
            }
        }
    }
	/**
	* drawInkjets
	* Draws all the active inkjets if the simulation has started, or all inkjets if not.
	* TODO: inkjets is currently iterated through twice! This is not needed. See paintIntersections too.
	**/
	public void drawInkjets(GL2 gl) {
		gl.glLineWidth(20);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, RED, 0);
		gl.glBegin(GL2.GL_LINES);
          //If the sim has started draw only the active inkjets
          if(simStarted && displayInkjets){
              for(Inkjet inkjet : activeInkjets){
                  inkjet.draw(gl);
              }
          }
          //Else draw all of them.
          else if(displayInkjets){
              for(Inkjet inkjet : inkjets){
                  inkjet.draw(gl);
              }
          }
		gl.glEnd();
		gl.glLineWidth(1);
	}
	
	public void paintIntersections() {
		for(Inkjet inkjet : activeInkjets){
            //storage
            float[] intersection = sphereRayIntersect(egg.SPHERE_RADIUS, egg.SPHERE_CENTER, inkjet.getP1(), inkjet.getP2(), 100);
            float[] lastIntersect = inkjet.getLastIntersect();
            float distance;
            int width;
            
            //Only bother if it collides
            if(intersection != null){
                //Must be rotated to match the egg
                float[] intersection4 = new float[]{intersection[0], intersection[1], intersection[2], 1f};
                float[] rotatedIntersection = new float[4];
                float[] rotation = rotH.getInverseRotation();
                FloatUtil.multMatrixVec(rotation, intersection4, rotatedIntersection);
                if(lastIntersect == null) {
                    lastIntersect = rotatedIntersection.clone();
                }
                paint(rotatedIntersection, lastIntersect, inkjet.getWidth());
                inkjet.setLastIntersect(rotatedIntersection);
                //System.out.println("X: "+intersection[0]+" Y:"+intersection[1]+" Z:"+intersection[2]);
            }
		}
	}
	
	/**
	* toggleInkjets
	* toggle whether to display inkjets
	**/
	public void toggleInkjets() {
		if(displayInkjets){
			displayInkjets = false;
		}
		else{
			displayInkjets = true;
		}
	}
	/**
	* removeInkjets
	* Remove all inkjets from the current array.
	**/
	public void removeInkjets() {
		inkjets = new Inkjet[0];
	}
	
	/**
	* exportInkjets
	* exports current inkjets to a file, inkjets.xml.
	**/
	public void saveInkjets() {
		Inkjet.exportInkjets(inkjets, "../res/inkjets.xml");
	}	
	/**
	* loadInkjets
	* Loads the inkjets.xml file to create inkjets (adds them rather than overwriting)
	**/
	public void loadInkjets(String filename) {
		Inkjet[] newInkjets = Inkjet.importInkjets("../res/"+filename);
		Inkjet[] oldInkjets = inkjets.clone();
		int newLen = newInkjets.length;
		int oldLen = oldInkjets.length;
		inkjets = new Inkjet[newLen+oldLen];
		
		System.arraycopy(oldInkjets, 0, inkjets, 0, oldLen);
		System.arraycopy(newInkjets, 0, inkjets, oldLen, newLen);
	}
	/**
	* loadRotations
	**/
	public void loadRotations(String filename) {
		rotH.importRotations(filename);
	}
	/**
	* rotateInkjets
	* rotates all inkjets by theta and phi specified.
	**/
	/*public void rotateInkjets(double theta, double phi) {
		for(Inkjet inkjet : inkjets){
			//Only rotate active jets
			if(time >= inkjet.getStartTime() && time < inkjet.getEndTime()){
				inkjet.incrementThetaPhi(theta, phi);
			}
		}
	}*/
	
	/*==============================EGG PARAMETER METHODS===============================*/
	/**
	* incEggRotation
	* Increments the rotation of the egg around the x and y axis by the amount specified
	* @param x The amount to increase rotation around the x axis
	* @param y The amount to increase rotation around the y axis
	**/
	public void incEggRotation(double x, double y) {
		egg.incRotation(x, y);
	}
	
	/*=======================EGG(SPHERE APPROX)-LINE COLLISION METHODS==================*/
	/*Note that as these lines all end at the origin of the sphere, these aren't really needed, but work and may
	* be used later.
	*/
	
	public float[] sphereRayIntersect(float radius, float[] center, float[] a, float[] b, float max_t) {
		float[] intersection = new float[3];

		float[] m = new float[3];
		VectorUtil.subVec3(m, b, a);
		
		float[] dst = new float[]{center[0]-a[0], center[1]-a[1], center[2]-a[2]};
		float t = VectorUtil.dotVec3(dst, m);
		/* is it in front of the eye? */
		if (t <= 0) {
			return null;
		}
		/* depth test */
		float d = t * t - VectorUtil.dotVec3(dst, dst) + radius * radius;
		/* does it intersect the sphere? */
		if (d <= 0) {
			return null;
		}
		/* is it closer than the closest thing we've hit so far */
		t -= (float) Math.sqrt(d);
		if (t >= max_t) {
			return null;
		}

		/* if so, then we have an intersection */
		intersection[0] = a[0] + t * m[0];
		intersection[1] = a[1] + t * m[1];
		intersection[2] = a[2] + t * m[2];

		return intersection;
	}

	//Manual control removed.
	/**
	* addInkjet
	* adds a new inkjet to the scene.
	**/
	/*public void addInkjet(Inkjet inkjet) {
		inkjets.push(inkjet);
	}*/
	/**
	* undoInkjet
	* removes the last inkjet added to the arraylist
	**/
	/*public void undoInkjet() {
		inkjets.pop();
	}*/
	
	/*==============================EGG TEXTURING METHODS=================================*/
	
	/**
	* initImage
	* Initialise the image backing the texture object, with the specified background colour.
	* @param r The red intensity 0-255
	* @param g The green intensity 0-255
	* @param b The blue intensity 0-255
	* @param a The alpha value 0-255
	**/
	public void initImage(int r, int g, int b) {
		image = new BufferedImage(TEXTURE_WIDTH, TEXTURE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		graphics = image.createGraphics();
		
		Color backgroundColor = new Color(r, g, b);
		graphics.setPaint(backgroundColor);
		graphics.fill(new Rectangle2D.Double(0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT));
    }
	
	/**
	* initTexture
	* initialises and binds the egg's texture
	**/
	public void initTexture(GL2 gl) {
		eggTexture = AWTTextureIO.newTexture(GLProfile.getDefault(), image, false);
		eggTexture.enable(gl);
		eggTexture.bind(gl);
		eggTexture.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
		eggTexture.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
		eggTexture.setTexParameteri(gl, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);
	}
	
	/**
	* updateTexture
	* updates the texture's image, resources have to be destroyed first to avoid
	* memory leak.
	**/
	public void updateTexture(GL2 gl) {
		eggTexture.destroy(gl);
		eggTexture = AWTTextureIO.newTexture(GLProfile.getDefault(), image, false);
		eggTexture.enable(gl);
		eggTexture.bind(gl);
		eggTexture.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
		eggTexture.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
		eggTexture.setTexParameteri(gl, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);
	}
	
	/**
	* paint3d
	* Paints on the texture at a location corresponding with an (x, y, z) position on it's surface.
	* Makes the assumption that in a single frame an inkjet won't move more than half way around the egg,
	* this assumption may not be true near the poles, however it should not matter.
	* @param x The x location on the surface
	* @param y The y location on the surface
	* @param z The z location on the surface
	**/
	public void paint(float[] intersect, float[] lastIntersect, int width) {
		//lastIntersect = rotateIntersect(lastIntersect.clone());
		//intersect = rotateIntersect(intersect.clone());
		
		double[] uv0 = egg.calcUV(intersect[0], intersect[1], intersect[2]);
		double[] uv1 = egg.calcUV(lastIntersect[0], lastIntersect[1], lastIntersect[2]);
		int uInt0 = (int) Math.floor(uv0[0]*TEXTURE_WIDTH);
		int vInt0 = (int) Math.floor(uv0[1]*TEXTURE_HEIGHT);
		int uInt1 = (int) Math.floor(uv1[0]*TEXTURE_WIDTH);
		int vInt1 = (int) Math.floor(uv1[1]*TEXTURE_HEIGHT);
		
		//set up paint tool, note:
		BasicStroke solid = new BasicStroke(width);
		graphics.setStroke(solid);
		lineColor = new Color(layer, layer, layer);
		graphics.setPaint(lineColor);
		
		//Given to uv values, we need to check if the two cross the seam (if we don't consider this case, rings form upon crossing)
		//If they do, then draw two lines, uv0 -> seam intersect and seam intersect -> uv1.
		//Assume normally a jet won't move half way around the egg in a single move
		if(Math.abs(uInt0 - uInt1) > TEXTURE_WIDTH/2){
			//Approximate intersection V to be half way between vInt0, vInt1
			int maxV = Math.max(vInt0, vInt1);
			int minV = Math.min(vInt0, vInt1);
			int vInt2 = minV+(maxV-minV);
			
			//Largest U needs to be drawn from itself to TEXTURE_WIDTH,
			//Smallest from itself to 0.
			Line2D.Double line1 = null;
			Line2D.Double line2 = null;
			if(uInt0 > uInt1){
				line1 = new Line2D.Double(uInt0, vInt0, TEXTURE_WIDTH, vInt2);
				line2 = new Line2D.Double(uInt1, vInt1, 0, vInt2);
			}
			else{
				line1 = new Line2D.Double(uInt0, vInt0, 0, vInt2);
				line2 = new Line2D.Double(uInt1, vInt1, TEXTURE_WIDTH, vInt2);
			}
			graphics.draw(line1);
			graphics.draw(line2);
		}
		//The normal case in which the border isn't crossed
		else{
			Line2D.Double line = new Line2D.Double(uInt0, vInt0, uInt1, vInt1);
			graphics.draw(line);
		}
	}
	
	/**
	* blurImage
	* blurs the image backing the sphere texture.
	**/
	public void blurImage() {
		float ninth = 1.0f / 9.0f;
		float[] blurKernel = {ninth, ninth, ninth,
							ninth, ninth, ninth,
							ninth, ninth, ninth};
		BufferedImageOp blur = new ConvolveOp(new Kernel(3, 3, blurKernel));
		image = blur.filter(image, null);
		graphics = (Graphics2D) image.getGraphics();
	}
	
	/**
	* saveTexture
	* saves the texture to a png
	**/
	public void saveTexture(String filename) {
		File output = new File(filename);
		
		try{
		ImageIO.write(image, "png", output);
		System.out.println("Writen texture to: "+filename);
		}catch(IOException e){
		System.out.println("Failed to save texture: "+e.getMessage());
		}
	}
	
	/**
	* set/getSimStarted
	* Set/get the simulated as started (true) or not started/stopped (false)
	* @param simStarted The value of simStarted.
	**/
	public void setSimStarted(boolean simStarted) {
		this.simStarted = simStarted;
	}
	public boolean getSimStarted() {
		return simStarted;
	}
	
	/**
	* getTime
	* Gets the current time in seconds since the simulation started.
	* Assuming framerate is constantly the maximum.
	**/
	public double getTime() {
		return time;
	}
	
	/**
	* get and set duration
	**/
	public double getDuration() {
		return duration;
	}
	public void setDuration(double duration) {
		this.duration = duration;
	}
}
