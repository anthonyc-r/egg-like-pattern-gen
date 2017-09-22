/**
* Author: Anthony Cohn-Richardby
**/
package simulation;

import generation.RotationGenGUI;
import generation.InkjetGenGUI;

import java.awt.*;
import java.awt.event.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.*;

import java.text.DecimalFormat;

public class Main extends Frame implements GLEventListener, MouseMotionListener, ActionListener {
        
    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;
    private static final float NEAR_CLIP = 0.1f;
    private static final float FAR_CLIP = 200.0f;
	
	//Mouse movement
	private Point lastpoint;
	private int width, height;
	
	//Menu params
	private TextField loadFile;
	private TextField loadRotFile;
	private TextField layerIncrement;
	private TextField saveTexFile;
	private TextField durationField;
	private Label infoLabel;
	DecimalFormat df = new DecimalFormat("#.##");

    private GLCanvas canvas;
    private Scene scene;
	private Camera camera;
	private InkjetGenGUI inkjetGenGUI;
	private RotationGenGUI rotationGenGUI;

        
    public static void main(String[] args) {
        Main main = new Main();
        main.setVisible(true);
    }

    public Main() {
        super("Egg-Inkjet");
        setSize(WIDTH, HEIGHT);
		
		//Start up inkjetgengui hidden
		inkjetGenGUI = new InkjetGenGUI();
		rotationGenGUI = new RotationGenGUI();
		
		//Setup opengl stuff
        GLProfile glp = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(glp);
        canvas = new GLCanvas(caps);
        add(canvas, "Center");

		//Listener and gui stuff
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
		/*=======================TOP MENU==========================*/
		MenuBar menuBar = new MenuBar();
		setMenuBar(menuBar);
		//Generate menu
		Menu genMenu = new Menu("Generate...");
		menuBar.add(genMenu);
		//generate inkjets
		MenuItem genInkjets = new MenuItem("Inkjets");
		genInkjets.setActionCommand("openinkjetgen");
		genInkjets.addActionListener(this);
		//generate movements
		MenuItem genMovement = new MenuItem("Movements");
		genMovement.setActionCommand("openmovementgen");
		genMovement.addActionListener(this);	
		//Add to gen menu
		genMenu.add(genInkjets);
		genMenu.add(genMovement);
		/*=======================SIDE PANEL========================*/
		//Panel for adding a new inkjet
		Panel menuPanel = new Panel(new GridLayout(10,1));
		//TOGGLE REMOVE BUTTONS
		Panel addUndoButtonP = new Panel(new GridLayout(1, 2));
		Button add = new Button("Toggle Inkjets");
		add.setActionCommand("toggleinkjets");
		add.addActionListener(this);
		Button undo = new Button("Remove Inkjets");
		undo.setActionCommand("removeinkjets");
		undo.addActionListener(this);
		addUndoButtonP.add(add);
		addUndoButtonP.add(undo);
		menuPanel.add(addUndoButtonP);
		//LOAD BUTTON
		Panel loadP = new Panel(new GridLayout(1, 2));
		Button loadB = new Button("Load Inkjets: ");
		loadB.setActionCommand("load");
		loadB.addActionListener(this);
		loadFile = new TextField("inkjets.xml");
		loadP.add(loadB);
		loadP.add(loadFile);
		menuPanel.add(loadP);
		//LOAD ROTATIONS BUTTON
		Panel loadRotP = new Panel(new GridLayout(1, 2));
		Button loadRotB = new Button("Load Rotations: ");
		loadRotB.setActionCommand("loadrot");
		loadRotB.addActionListener(this);
		loadRotFile = new TextField("rotations.xml");
		loadRotP.add(loadRotB);
		loadRotP.add(loadRotFile);
		menuPanel.add(loadRotP);
		//DURATION Panel
		Panel durationP = new Panel(new GridLayout(1, 2));
		Label durationLabel = new Label("Simulation Duration:");
		durationField = new TextField("10");
		durationP.add(durationLabel);
		durationP.add(durationField);
		menuPanel.add(durationP);
		//START END BUTTONS
		Panel startEndButtonP = new Panel(new GridLayout(1, 2));
		Button startButton = new Button("Start");
		startButton.setActionCommand("start");
		startButton.addActionListener(this);
		Button endButton = new Button("End");
		endButton.setActionCommand("end");
		endButton.addActionListener(this);
		startEndButtonP.add(startButton);
		startEndButtonP.add(endButton);
		menuPanel.add(startEndButtonP);
		//BLUR
		Button blurButton = new Button("Blur");
		blurButton.setActionCommand("blur");
		blurButton.addActionListener(this);
		menuPanel.add(blurButton);		
		//RESET
		Button resetButton = new Button("Reset");
		resetButton.setActionCommand("reset");
		resetButton.addActionListener(this);
		menuPanel.add(resetButton);
		//SAVE TEXTURE PANEL
		Panel saveTexP = new Panel(new GridLayout(1, 2));
		Button saveTexB = new Button("Save Texture: ");
		saveTexB.setActionCommand("savetex");
		saveTexB.addActionListener(this);
		saveTexFile = new TextField("texture.png");
		saveTexP.add(saveTexB);
		saveTexP.add(saveTexFile);
		menuPanel.add(saveTexP);
		//TIME INFO PANEL
		infoLabel = new Label("Time: 0");
		menuPanel.add(infoLabel);
		
		add(menuPanel, "East");
		
        canvas.addGLEventListener(this);
        canvas.addMouseMotionListener(this);

        FPSAnimator animator = new FPSAnimator(canvas, 30);
        animator.start();
    }

	
	/*===============================GLEventListener INTERFACE==============================*/
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glFrontFace(GL2.GL_CCW);
        gl.glCullFace(GL2.GL_BACK);
        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
        gl.glEnable(GL2.GL_NORMALIZE);
        gl.glEnable(GL2.GL_TEXTURE_2D);
		
        initLighting(gl);
		
		camera = new Camera(0, 0, 100);
        scene = new Scene(gl, camera);
    }

    public void initLighting(GL2 gl) {
        gl.glEnable(GL2.GL_LIGHTING);
        float pos[] = {0.0f, 10.0f, 0.0f, 0.0f};
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, pos, 0);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glEnable(GL2.GL_NORMALIZE);
    }

	/**
	* Reshape by Steve Maddock
	**/
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
						
		this.width=width;
		this.height=height;
		
        GL2 gl = drawable.getGL().getGL2();

        float fAspect = (float) width/height;
        float fovy = 60.0f;
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        float top = (float) Math.tan(Math.toRadians(
                                    fovy*0.5))*NEAR_CLIP;
                
        float bottom = -top;
        float left = fAspect*bottom;
        float right = fAspect*top;
        
        gl.glFrustum(left, right, bottom, top, NEAR_CLIP,
                        FAR_CLIP);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        scene.update(gl);
        scene.render(gl);
		
		//Update the info label every frame too.
        infoLabel.setText("Time: "+df.format(scene.getTime()));
    }

    public void dispose(GLAutoDrawable drawable) {
    }
	
	/*=============================MouseMotionListener INTERFACE==========================*/
	/**
	* mouseDragged by Steve Madock
	* The mouse is used to control the camera position.
	* @param e instance of MouseEvent, automatically supplied by the system when the user drags the mouse
	**/    
	public void mouseDragged(MouseEvent e) {
		Point ms = e.getPoint();
    
		float dx=(float) (ms.x-lastpoint.x)/width;
		float dy=(float) (ms.y-lastpoint.y)/height;
		
		//the mouse moves the camera
		if (e.getModifiers()==MouseEvent.BUTTON1_MASK) {
			camera.updateThetaPhi(-dx*2.0f, dy*2.0f);
		}
		else if (e.getModifiers()==MouseEvent.BUTTON3_MASK) {
			camera.updateRadius(-dy*10.0f);
		}
		lastpoint = ms;
	}
	
	/**
	* mouseMoved by Steve Maddock
	* The mouse is used to control the camera position.
	* @param e  instance of MouseEvent, automatically supplied by the system when the user moves the mouse
	**/  
	public void mouseMoved(MouseEvent e) {
		lastpoint = e.getPoint();
	}
	
	/*================================ActionListener INTERFACE=============================*/
	/**
	* actionPerformed
	* Callback for for the menu options.
	**/
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equalsIgnoreCase("quit")){
			System.exit(0);
		}
		//Inkjets no longer support being manually added and must be generated.
		/*else if(e.getActionCommand().equalsIgnoreCase("addinkjet")){
			System.out.println("Adding inkjet");
			//Inkjet is projects along the current looking direction. Into the center, for the moment.
			double theta = camera.getTheta();
			double phi = camera.getPhi();
			//Pull params from fields.
			double startTime = Double.valueOf(inkjetStart.getText());
			double endTime = Double.valueOf(inkjetEnd.getText());
			int brushWidth = Integer.valueOf(inkjetWidth.getText());
			
			Inkjet inkjet = new Inkjet(theta, phi, startTime, endTime, brushWidth);
			scene.addInkjet(inkjet);
		}
		*/
		else if(e.getActionCommand().equalsIgnoreCase("toggleinkjets")){
			System.out.println("Toggling inkjets");
			scene.toggleInkjets();
		}
		else if(e.getActionCommand().equalsIgnoreCase("removeinkjets")){
			System.out.println("Removing all inkjets");
			scene.removeInkjets();
		}
		else if(e.getActionCommand().equalsIgnoreCase("start")){
			double duration = Double.valueOf(durationField.getText());
			scene.setDuration(duration);
			scene.setSimStarted(true);
			System.out.println("Sim started");
		}
		else if(e.getActionCommand().equalsIgnoreCase("end")){
			scene.setSimStarted(false);
			System.out.println("Sim stopped");
		}
		else if(e.getActionCommand().equalsIgnoreCase("blur")){
			scene.blurImage();
		}
		else if(e.getActionCommand().equalsIgnoreCase("reset")){
			scene.resetScene();
		}
		else if(e.getActionCommand().equalsIgnoreCase("save")){
			scene.saveInkjets();
		}
		else if(e.getActionCommand().equalsIgnoreCase("load")){
			scene.loadInkjets(loadFile.getText());
		}
		else if(e.getActionCommand().equalsIgnoreCase("loadrot")){
			scene.loadRotations(loadRotFile.getText());
		}
		else if(e.getActionCommand().equalsIgnoreCase("savetex")){
			scene.saveTexture("../"+saveTexFile.getText());
		}
		else if(e.getActionCommand().equalsIgnoreCase("openinkjetgen")){
			inkjetGenGUI.setVisible(true);
		}
		else if(e.getActionCommand().equalsIgnoreCase("openmovementgen")){
			rotationGenGUI.setVisible(true);
		}
	}
}
