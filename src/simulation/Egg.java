/*
*@Author: Anthony Cohn-Richardby
*/
package simulation;

import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.texture.*;
import com.jogamp.opengl.util.texture.awt.*;
import com.jogamp.opengl.util.awt.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.awt.GLCanvas;

import java.nio.ByteBuffer;
import java.nio.Buffer;

import java.io.FileInputStream;

import java.util.Arrays;
import java.util.Scanner;

import java.awt.image.*;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

//Debugging 
import java.util.logging.Logger;

public class Egg {
	/*======================LOGGER FOR DEBUGGING PURPOSES====================================*/
	public static final Logger LOGGER = Logger.getLogger(Egg.class.getName());
	/*======================FILENAME OF EGG OBJECT DECLARED HERE!============================*/
	public static final String FILE_NAME = "../res/full-egg-hollow.obj";
	//Egg spherical approximation radius. 42
	public static final float SPHERE_RADIUS = 42; 
	//Center of sphere approx.
	public static final float[] SPHERE_CENTER = {0f, 0f, 0f};
	
	private double[][] vertexList;
	private double[][] uvList;
	private double[][] normalList;
	private int[][][] faceList;
	
	/*==============================EGG PARAMS===============================================*/
	private double xRotation;
	private double yRotation;
	private float[] rotation;
	
	
	/**
	* Egg constructor
	* Loads the data required to draw the egg from it's wavefront obj file.
	**/
	public Egg() {
		loadFile(FILE_NAME);
		rotation = new float[16];
	}
	
	/*===================PUBLIC METHODS==================*/
	
	/** drawEgg
	* Draw the egg given the stored face-vertex data.
	* @param gl The openGL context.
	**/
	public void draw(GL2 gl) {
		double[] currentVertex;
		double[] currentNormal;
		double[] currentUV;
		
		//Draw at the specified rotation
		//gl.glRotated(yRotation, 0, 1, 0);  //then y
		//gl.glRotated(xRotation, 1, 0, 0);  //first x
		gl.glMultMatrixf(rotation, 0);
		
		
		//Begin drawing the triangles:
		gl.glBegin(GL2.GL_TRIANGLES);
			for(int[][] face : faceList){
				//vertex 0..2
				for(int i=0; i<3; i++){
					currentVertex = new double[]{vertexList[face[i][0]-1][0],
												vertexList[face[i][0]-1][1],
												vertexList[face[i][0]-1][2]};
					currentUV = new double[]{uvList[face[i][1]-1][0],
											uvList[face[i][1]-1][1]};
					currentNormal = new double[]{normalList[face[i][2]-1][0],
												normalList[face[i][2]-1][1],
												normalList[face[i][2]-1][2]};
					gl.glNormal3dv(currentNormal, 0);
					gl.glTexCoord2dv(currentUV, 0);						
					gl.glVertex3dv(currentVertex, 0);
				}
			}
		gl.glEnd();
	}
	
	/**
	* calcUV
	* Calculate the (u, v) coordinates for a given vertex, based upon a spherical coordinate system
	* Given a sphere with radius 1, centered around local origin.
	* @param x The x coordinate of the vertex.
	* @param y The y coordinate of the vertex.
	* @param z The z coordinate of the vertex.
	* @return Returns an array of size 2, representing the (u, v) coordinates.
	**/
	public double[] calcUV(double x, double y, double z) {
		
		//Return values
		double u;
		double v;
		double[] uv;
		//length of vec required to calculate normal
		double length = Math.sqrt((x*x) + (y*y) + (z*z));
		
		//normalized values
		double xn = x/length;
		double yn = y/length;
		double zn = z/length;
		
		u = 0.5 + (Math.atan2(zn, xn)/(Math.PI*2));
		v = 0.5 - (Math.asin(yn)/(Math.PI));
		
		uv = new double[]{u, v};
		
		return uv;
	}
	
	/**
	* incRotation
	* Increments the rotation of the egg around the x and y axes.
	* @param x Increment to the rotation around the x axis
	* @param y Increment to the rotation around the y axis
	**/
	public void incRotation(double x, double y) {
		//System.out.println("Increasing rot by, x: "+x+", y: "+y);
		System.out.println("x rotation: "+xRotation+", y rotation: "+yRotation);
		xRotation += x;
		yRotation += y;
	}
	
	/**
	* setRotation
	*
	**/
	public void setRotation(float[] matrix) {
		this.rotation = matrix.clone();
	}
	
	/**
	* getXRotation
	* Get the current rotation of the egg around the x axis
	* @return The rotation around the x axis
	**/
	public double getXRotation() {
		return xRotation;
	}
	
	/**
	* getYRotation
	* Get the current rotation of the egg around the y axis
	* @return The rotation around the y axis
	**/
	public double getYRotation() {
		return yRotation;
	}
	
	/*====================PRIVATE METHODS==================*/
	
	/**
	* loadFile
	* Load the vertex and face list from the file.
	* @param filename egg object file name.
	**/
	private void loadFile(String filename) {
		
		int verts = 0;
		int faces = 0;
		int norms = 0;
		
		//Temp storage
		String[] line;
		String identifier;
		String[] vert1;
		String[] vert2;
		String[] vert3;
		String[][] sFace = new String[3][3];
		int[][] face = new int[3][3];
		
		
		//useful indexes
		int currentVert = 0;
		int currentNorm = 0;
		int currentFace = 0;
		
		//DEBUGGING
		LOGGER.info("Egg.loadFile Entered.");
		//DEBUGGING END
		
		//IO setup
		FileInputStream in = null;
		Scanner scanner = null;
		
		//Set up the file input must throw exception
		try {
			in = new FileInputStream(FILE_NAME);
			scanner = new Scanner(in);
		}
		catch(Exception e) {
			LOGGER.severe("FILE NOT FOUND!");
			e.printStackTrace();
		}
		
		//DEBUG
		LOGGER.info("Found wavefront object file!");
		//DEBUG END
		
		//Quick scan to count the number of faces and vertices.
		while(scanner.hasNextLine()) {
			//more useful split by spaces
			line = scanner.nextLine().split(" ");
			//the identifier is the first part
			identifier = line[0];
			
			//DEBUG
			//LOGGER.info("Line identifier:"+identifier);
			//DEBUG END
			
			//count verts and faces.
			if(identifier.equals("v")) {
				verts++;
			}
			else if(identifier.equals("f")){
				faces++;
			}
			else if(identifier.equals("vn")){
				norms++;
			}
		}
		
		//Set up storage arrays.
		vertexList = new double[verts][3];
		uvList = new double[verts][2];
		normalList = new double[norms][3];
		//faces num of 3x3 values, v1i,v1uvi,v1ni,...,v3i,v3uvi,v3ni
		faceList = new int[faces][3][3]; //faces*sizeof(int) of wasted space

		//scanner.reset();
		//Scan through again for vertices and normals, calc uv while im at it.
		scanner.close();
		try {
			in = new FileInputStream(FILE_NAME);
			scanner = new Scanner(in);
		}
		catch(Exception e) {
			LOGGER.severe("FILE NOT FOUND!");
			e.printStackTrace();
		}
		
		while(scanner.hasNextLine()) {
			line = scanner.nextLine().split(" ");
			//the identifier is the first section split by a space.
			identifier = line[0];
			//DEBUG
			//LOGGER.info("Line identifier:"+identifier);
			//DEBUG END
			
			//vertices are listed first
			if(identifier.equals("v")) {
				vertexList[currentVert] = new double[]{Double.valueOf(line[1]),
													Double.valueOf(line[2]),
													Double.valueOf(line[3])};
				uvList[currentVert] = calcUV(Double.valueOf(line[1]), 
										Double.valueOf(line[2]), 
										Double.valueOf(line[3]));
				currentVert++;
			}
			//then normals...
			else if(identifier.equals("vn")){
				normalList[currentNorm] = new double[]{Double.valueOf(line[1]), 
													Double.valueOf(line[2]), 
													Double.valueOf(line[3])};
				currentNorm++;
			}
			//finally read face indexes
			else if(identifier.equals("f")){
				vert1 = line[1].split("/");
				vert2 = line[2].split("/");
				vert3 = line[3].split("/");
				sFace = new String[][]{vert1, vert2, vert3};
				
				//mass conversion
				for(int i=0; i<3; i++){
					for(int j=0; j<3; j++){
						//Deal with the empty string being passed to valueOf, for "v"/""/"vn".
						if(j == 1){
							//will have been set in prev iter...
							//Index is aligned with vertex index as defined above.
							sFace[i][j] = sFace[i][0];
						}

						face[i][j] = Integer.valueOf(sFace[i][j]);
					}
				}
				
				//store the 3 vertex,uv,normal indexes.
				faceList[currentFace] = new int[][]{{face[0][0], face[0][1], face[0][2]},
													{face[1][0], face[1][1], face[1][2]},
													{face[2][0], face[2][1], face[2][2]}};
				currentFace++;
			}
		}
		LOGGER.info("Verts:"+verts+" Faces:"+faces+" Norms:"+norms);
		
	}
	
}

