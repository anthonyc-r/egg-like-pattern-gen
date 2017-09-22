/**
* @Author: Anthony Cohn-Richardby
**/
package generation;

import simulation.RotationHandler;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import javax.swing.*;

import com.jogamp.opengl.math.Quaternion;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.distribution.MixtureMultivariateNormalDistribution;
import org.apache.commons.math3.util.Pair;

public class RotationGenGUI extends Frame implements ActionListener {
	public static final int WIDTH = 800;
	public static final int HEIGHT = 600;
	public static final int PREVIEW_WIDTH = 800;
	public static final int PREVIEW_HEIGHT = 600;

	RotationGen gen = null;
	BufferedImage distrPreview = null;
	Graphics2D graphics = null;

	MixtureMultivariateNormalDistribution mixtureDistr = null;
	ArrayList<Pair<Double, MultivariateNormalDistribution>> distrList = null;
	ArrayList<Panel> distrParams = null;
	
	TreeMap<Double, Quaternion> rotations = null;
	
	TextField numRotsInput = null;
	TextField maxThetaInput = null;
	TextField maxThetaChInput = null;
    TextField maxPhiInput = null;
    TextField maxPhiChInput = null;
	Panel distrP = null;
	Panel previewP = null;
	Panel graphicsP = null;
	TextField exportFile = null;
	

	public RotationGenGUI() {
		setSize(WIDTH, HEIGHT);
		guiSetup();
		gen = new RotationGen();
		distrParams = new ArrayList<Panel>();
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equalsIgnoreCase("quit")){
			System.exit(0);
		}
		else if(e.getActionCommand().equalsIgnoreCase("del")){
			distrP.remove(((Component) e.getSource()).getParent());
			distrP.revalidate();
		}
		else if(e.getActionCommand().equalsIgnoreCase("gen")){
			setRotations();
			updatePreviewImage();
			graphicsP.repaint();
		}
		else if(e.getActionCommand().equalsIgnoreCase("save")){
			String filename = exportFile.getText();
			RotationHandler.exportRotations(rotations, filename);
		}
	}
	
	private void setRotations() {
		double maxTheta = Double.valueOf(maxThetaInput.getText());
		double maxThetaChange = Double.valueOf(maxThetaChInput.getText());
     	double maxPhi = Double.valueOf(maxPhiInput.getText());
		double maxPhiChange = Double.valueOf(maxPhiChInput.getText());
           
		int numRotations = Integer.valueOf(numRotsInput.getText());
		rotations = gen.genRotations(maxTheta, maxThetaChange, maxPhi, maxPhiChange, numRotations, mixtureDistr);
	}

	
	private void guiSetup() {
		addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });
		
		//2 panels, generation and preview
		Panel generationP = new Panel();
		generationP.setLayout(new GridLayout(6, 1));
		previewP = new Panel();
		previewP.setLayout(new GridLayout(2, 1));
		
		/*=======================GENERATION CTRL PANEL=========================*/
		//num of rotations input
		Panel numRotsP = new Panel();
		numRotsP.setLayout(new GridLayout(1, 2));
		Label numRotsL = new Label("Number of Rotations:");
		numRotsInput = new TextField("10");
		numRotsP.add(numRotsL);
		numRotsP.add(numRotsInput);
		//THETA 
        //maximum rotation angle input
		Panel maxThetaP = new Panel();
		maxThetaP.setLayout(new GridLayout(1, 2));
		Label maxThetaL = new Label("Maximum Theta Rotation Angle:");
		maxThetaInput = new TextField("3");
		maxThetaP.add(maxThetaL);
		maxThetaP.add(maxThetaInput);
		//maximum change in egg angle
		Panel maxThetaChP = new Panel();
		maxThetaChP.setLayout(new GridLayout(1, 2));
		Label maxThetaChL = new Label("Maximum Change in Theta:");
		maxThetaChInput = new TextField("0.1");
		maxThetaChP.add(maxThetaChL);
		maxThetaChP.add(maxThetaChInput);
        
        //PHI
        //maximum rotation angle input
		Panel maxPhiP = new Panel();
		maxPhiP.setLayout(new GridLayout(1, 2));
		Label maxPhiL = new Label("Maximum Phi Rotation Angle:");
		maxPhiInput = new TextField("6");
		maxPhiP.add(maxPhiL);
		maxPhiP.add(maxPhiInput);
		//maximum change in egg angle
		Panel maxPhiChP = new Panel();
		maxPhiChP.setLayout(new GridLayout(1, 2));
		Label maxPhiChL = new Label("Maximum Change in Phi:");
		maxPhiChInput = new TextField("0.2");
		maxPhiChP.add(maxPhiChL);
		maxPhiChP.add(maxPhiChInput);
		//Then generate button
		Button genB = new Button("Generate");
		genB.setActionCommand("gen");
		genB.addActionListener(this);
        generationP.add(numRotsP);
        generationP.add(maxThetaP);
        generationP.add(maxThetaChP);
        generationP.add(maxPhiP);
        generationP.add(maxPhiChP);
        generationP.add(genB);
		
		
		/*============================PREVIEW PANEL=========================================*/
		distrPreview = new BufferedImage(PREVIEW_WIDTH, PREVIEW_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		graphics = (Graphics2D) distrPreview.getGraphics();
		
		//Set paint function to draw the bufferedImage generated by updatePreviewImage()
		graphicsP = new Panel() {
			public void paint(Graphics g) {
				super.paint(g);
				g.drawImage(distrPreview, 0, 0, this.getWidth(), this.getHeight(), this);
			}
		};
		/**=============================EXPORT PANEL=======================================*/
		Panel exportP = new Panel();
		Button exportB = new Button("Save As");
		exportFile = new TextField("rotations.xml");
		exportP.add(exportB);
		exportP.add(exportFile);
		exportB.setActionCommand("save");
		exportB.addActionListener(this);
		
		//Preview and export share a panel
		previewP.add(graphicsP);
		previewP.add(exportP);
		
		
		setLayout(new GridLayout(1, 2));
		add(generationP);
		add(previewP);
	}
	
	private void updatePreviewImage() {
		double samplesY = (PREVIEW_HEIGHT*3)/4;
		double samplesH = 5;
		double scaleY = PREVIEW_HEIGHT-10;
		double scaleX = 10;
		double scaleW = PREVIEW_WIDTH-20;
		
		int lineWidth = 5;
		Color mixtureDensityColor = new Color(255, 0, 0);
		Color samplesColor = new Color(0, 255, 0);
		Color scaleColor = new Color(0, 0, 0);
		BasicStroke line = new BasicStroke(lineWidth);
		graphics.setStroke(line);
		
		//Clear
		Color backgroundColor = new Color(250, 250, 250);
		graphics.setPaint(backgroundColor);
		graphics.fill(new Rectangle2D.Double(0, 0, 800, 600));
		
		
		//Draw samples
		for(Map.Entry<Double, Quaternion> entry : rotations.entrySet()){
			double t = entry.getKey();
            System.out.println(t);
			graphics.fill(new Ellipse2D.Double((t/1.0)*PREVIEW_WIDTH, samplesY, samplesH, samplesH));
		}
		//Draw scale
		graphics.setPaint(scaleColor);
		graphics.draw(new Line2D.Double(scaleX, scaleY, scaleX+scaleW, scaleY));
		
	}
	
	/**
	* Test harness
	**/
	public static void main(String[] args) {
		RotationGenGUI gui = new RotationGenGUI();
		gui.setVisible(true);
	}
}
