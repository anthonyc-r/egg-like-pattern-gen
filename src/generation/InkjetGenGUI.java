/**
* @Author: Anthony Cohn-Richardby
**/
package generation;

import simulation.Inkjet;
import simulation.Scene;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class InkjetGenGUI extends Frame implements ActionListener {
	public static final int WIDTH = 800;
	public static final int HEIGHT = 600;
	public static final int PREVIEW_WIDTH = Scene.TEXTURE_WIDTH;
	public static final int PREVIEW_HEIGHT = Scene.TEXTURE_HEIGHT;
	InkjetGen gen = null;
	BufferedImage inkjetPreview = null;
	Graphics2D graphics = null;
	
	//Clusters
	Panel clusterMeanInputP = null;
	Panel clusterCovarInputP = null;
	TextField clusterNumInput = null; 
	double[][] clusterCentres = null;
	
	//Inkjets
    TextField inkjetLargeVarInput = null;
    TextField inkjetSmallVarInput = null;
    TextField inkjetDistrebutionInput = null;
	TextField inkjetNumInput = null;
	TextField inkjetWidthInput = null;
	double[][][] clusteredPoints = null;
	
	//Preview
	Panel previewP = null;
	
	//Export
	TextField exportFile = null;
	
	public InkjetGenGUI() {
		setSize(WIDTH, HEIGHT);
		guiSetup();
		gen = new InkjetGen();
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equalsIgnoreCase("quit")){
			System.exit(0);
		}
		else if(e.getActionCommand().equalsIgnoreCase("gencentres")){
			genCentres();
		}
		else if(e.getActionCommand().equalsIgnoreCase("geninkjets")){
			genInkjets();
		}
		else if(e.getActionCommand().equalsIgnoreCase("save")){
			if(clusteredPoints == null){
				//err
			}
			else{
				int inkjetWidth = Integer.valueOf(inkjetWidthInput.getText());
				Inkjet.exportInkjets(gen.toInkjets(clusteredPoints, inkjetWidth), exportFile.getText());
			}
		}
	}
	
	private void genInkjets() {
        double largeVar = Double.valueOf(inkjetLargeVarInput.getText());
        double smallVar = Double.valueOf(inkjetSmallVarInput.getText());
        double distrebution = Double.valueOf(inkjetDistrebutionInput.getText());
		int numInkjets = Integer.valueOf(inkjetNumInput.getText());
		
		clusteredPoints = gen.generateClusteredPoints(clusterCentres, numInkjets, largeVar, smallVar, distrebution);
		updatePreviewImage();
		previewP.repaint();
	}
	
	private void genCentres() {
		clusteredPoints = null;
		//Get mean
		double[] mean = getMean(clusterMeanInputP);
		//Get covar
		double[][] covar = getCovariance(clusterCovarInputP);
		//get num
		int numClusters = Integer.valueOf(clusterNumInput.getText());
		
		clusterCentres = gen.generateClusterCentres(numClusters, mean, covar);
		updatePreviewImage();
		previewP.repaint();
	}
	
	private double[][] getCovariance(Panel p) {
		double[][] covar = new double[2][2];
		covar[0][0] = Double.valueOf(((TextField) p.getComponent(0)).getText());
		covar[0][1] = Double.valueOf(((TextField) p.getComponent(1)).getText());
		covar[1][0] = Double.valueOf(((TextField) p.getComponent(2)).getText());
		covar[1][1] = Double.valueOf(((TextField) p.getComponent(3)).getText());
		
		return covar;
	}
	
	private double[] getMean(Panel p) {
		double[] mean = new double[2];
		mean[0] = Double.valueOf(((TextField) p.getComponent(0)).getText());
		mean[1] = Double.valueOf(((TextField) p.getComponent(1)).getText());
		
		return mean;
	}
	
	private void guiSetup() {
		addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });
		
		//Three panels, tleft tright and bottom
		Panel clusterControlP = new Panel();
		//previewP
		Panel inkjetControlP = new Panel();
		
		/*=======================CLUSTER CONTROL PANEL=========================*/
		//A mean input
		Panel clusterMeanP = new Panel();
		Label meanLabel = new Label("Mean: ");
		clusterMeanInputP = new Panel();
		clusterMeanInputP.setLayout(new GridLayout(2, 1));
		clusterMeanInputP.add(new TextField("400"));
		clusterMeanInputP.add(new TextField("300"));
		clusterMeanP.add(meanLabel, "Left");
		clusterMeanP.add(clusterMeanInputP, "Right");
		//A covariance input
		Panel clusterCovarP = new Panel();
		Label covarLabel = new Label("Covariance Matrix: ");
		clusterCovarInputP = new Panel();
		clusterCovarInputP.setLayout(new GridLayout(2, 2));
		clusterCovarInputP.add(new TextField("100000")); //Component 0
		clusterCovarInputP.add(new TextField("0")); //Component 1
		clusterCovarInputP.add(new TextField("0")); //Component 2
		clusterCovarInputP.add(new TextField("100000")); //Component 3
		clusterCovarP.add(covarLabel, "Left");
		clusterCovarP.add(clusterCovarInputP, "Right");
		//A number of clusters
		Panel clusterNumP = new Panel();
		Label clusterNumLabel = new Label("Number of Clusters: ");
		clusterNumInput = new TextField("1000");
		clusterNumP.add(clusterNumLabel, "Left");
		clusterNumP.add(clusterNumInput, "Right");
		//A generate cluster centres button
		Button genCentresB = new Button("Generate Cluster Centres...");
		genCentresB.setActionCommand("gencentres");
		genCentresB.addActionListener(this);
		
		//Add them to the main panel...
		clusterControlP.setLayout(new GridLayout(4, 1));
		clusterControlP.add(clusterMeanP);
		clusterControlP.add(clusterCovarP);
		clusterControlP.add(clusterNumP);
		clusterControlP.add(genCentresB);
		
		/*============================INKJET CONTROL PANEL==================================*/
        //large/small var + distrebution
        Panel inkjetP = new Panel(new GridLayout(3, 2));
        Label largeVarL = new Label("Large Cluster Variance: ");
        inkjetLargeVarInput = new TextField("50");
        Label smallVarL = new Label("Small Cluster Variance: ");
        inkjetSmallVarInput = new TextField("0.1");
        Label distrebutionL = new Label("Distrebution of Size (0: most small, 1: most large): ");
        inkjetDistrebutionInput = new TextField("0.5");
        inkjetP.add(largeVarL);
        inkjetP.add(inkjetLargeVarInput);
        inkjetP.add(smallVarL);
        inkjetP.add(inkjetSmallVarInput);
        inkjetP.add(distrebutionL);
        inkjetP.add(inkjetDistrebutionInput);
		//num input
		Panel inkjetNumP = new Panel();
		Label inkjetNumL = new Label("Inkjets Per Cluster: ");
		inkjetNumInput = new TextField("10");
		inkjetNumP.add(inkjetNumL);
		inkjetNumP.add(inkjetNumInput);
		//Width
		Panel inkjetWidthP = new Panel();
		Label inkjetWidthL = new Label("Inkjer Width:");
		inkjetWidthInput = new TextField("3");
		inkjetWidthP.add(inkjetWidthL);
		inkjetWidthP.add(inkjetWidthInput);
		//Gen button
		Button genInkjetB = new Button("Generate Inkjets...");
		genInkjetB.setActionCommand("geninkjets");
		genInkjetB.addActionListener(this);
		//Add to main
		inkjetControlP.setLayout(new GridLayout(3, 1));
		inkjetControlP.add(inkjetP);
		inkjetControlP.add(inkjetNumP);
		inkjetControlP.add(inkjetWidthP);
		inkjetControlP.add(genInkjetB);
		/*============================PREVIEW PANEL=========================================*/
		inkjetPreview = new BufferedImage(PREVIEW_WIDTH, PREVIEW_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		graphics = (Graphics2D) inkjetPreview.getGraphics();
		
		previewP = new Panel() {
			public void paint(Graphics g) {
				super.paint(g);
				g.drawImage(inkjetPreview, 0, 0, this.getWidth(), this.getHeight(), this);
			}
		};
		/**=============================EXPORT PANEL=======================================*/
		Panel exportP = new Panel();
		Button exportB = new Button("Save As");
		exportFile = new TextField("inkjets.xml");
		exportP.add(exportB);
		exportP.add(exportFile);
		exportB.setActionCommand("save");
		exportB.addActionListener(this);
		
		
		setLayout(new GridLayout(2, 2));
		add(clusterControlP);
		add(previewP);
		add(inkjetControlP);
		add(exportP);
	}
	
	private void updatePreviewImage() {
		//Clear
		Color backgroundColor = new Color(250, 250, 250);
		graphics.setPaint(backgroundColor);
		graphics.fill(new Rectangle2D.Double(0, 0, Scene.TEXTURE_WIDTH, Scene.TEXTURE_HEIGHT));
		
		//Fill centres and inkjets
		Color centreColor = new Color(255, 0, 0);
		Color inkjetColor = new Color(0, 255, 0);
		double centreCircleSize = 5.0;
		double inkjetCircleSize = 5.0;
		//Centres
		if(clusterCentres != null) {
			graphics.setPaint(centreColor);
			for(double[] centre : clusterCentres) {
				graphics.fill(new Ellipse2D.Double(centre[0]-(centreCircleSize/2.0),
													centre[1]-(centreCircleSize/2),
													centreCircleSize, centreCircleSize));
			}
		}
		//inkjetPoints
		if(clusteredPoints != null) {
			graphics.setPaint(inkjetColor);
			for(double[][] points : clusteredPoints){
				for(double[] point : points) {
					graphics.fill(new Ellipse2D.Double(point[0]-(inkjetCircleSize/2.0),
														point[1]-(inkjetCircleSize/2),
														inkjetCircleSize, inkjetCircleSize));
				}
			}
		}
	}
	
	/**
	* Test harness
	**/
	public static void main(String[] args) {
		InkjetGenGUI gui = new InkjetGenGUI();
		gui.setVisible(true);
	}
}
