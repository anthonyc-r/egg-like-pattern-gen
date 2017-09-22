/**
* Author: Anthony Cohn-Richardby.
*
**/
package simulation;

import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
* This version of inkjets no longer uses spherical coordinates.
**/
public class Inkjet {
	//The radius of the sphere the jets are positioned upon
	public static final double RADIUS = 43;
	
	/*private double theta;
	private double phi;*/
	private float[] p1;
	private float[] p2;
	private double startTime;
	private double endTime;
	private int width;
	
	private float[] lastIntersect = null;
	
	/*public Inkjet(double theta, double phi, double startTime, double endTime, int width) {
		this.theta = theta;
		this.phi = phi;
		updatePoints();
		this.startTime = startTime;
		this.endTime = endTime;
		this.width = width;
		this.lastIntersect = null;
	}*/
	
	public Inkjet(float[] p1, double startTime, double endTime, int width) {
		this.p1 = p1.clone();
		this.p2 = new float[]{0, 0, 0};
		this.startTime = startTime;
		this.endTime = endTime;
		this.width = width;
	}
	
	public Inkjet(float[] p1, float[] p2, double startTime, double endTime, int width) {
		this.p1 = p1.clone();
		this.p2 = p2.clone();
		this.startTime = startTime;
		this.endTime = endTime;
		this.width = width;
	}
	
	public void draw(GL2 gl) {
		gl.glLineWidth(10);
		gl.glBegin(GL2.GL_LINES);
		  gl.glVertex3fv(p1, 0);
		  gl.glVertex3fv(p2, 0);
		gl.glEnd();
		gl.glLineWidth(1);
	}
	
	/*public void updatePoints() {
		double cy, cz, sy, sz;
		cy = Math.cos(theta);
		sy = Math.sin(theta);
		cz = Math.cos(phi);
		sz = Math.sin(phi);
    	
		p1 = new double[]{RADIUS*cy*cz, RADIUS*sz, -RADIUS*sy*cz};
		p2 = new double[]{0, 0, 0};
	}*/
	
	public float[] getP1() {
		return p1;
	}
	public float[] getP2() {
		return p2;
	}
	/*public double getTheta() {
		return theta;
	}
	public double getPhi() {
		return phi;
	}*/
	public double getStartTime() {
		return startTime;
	}
	public double getEndTime() {
		return endTime;
	}
	public int getWidth() {
		return width;
	}
	public float[] getLastIntersect() {
		return lastIntersect;
	}
    public void resetLastIntersect() {
        lastIntersect = null;
    }
	public void setLastIntersect(float[] lastIntersect) {
		this.lastIntersect = lastIntersect.clone();
	}
	/*public void incrementThetaPhi(double incTheta, double incPhi) {
		theta += incTheta;
		phi += incPhi;
		
		updatePoints();
	}*/
	
	/**
	* importInkjets
	**/
	public static Inkjet[] importInkjets(String filename) {
		try {
		File inkjetsXml = new File(filename);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(inkjetsXml);
		
		NodeList nList = doc.getElementsByTagName("inkjet");
		int numInkjets = nList.getLength();
		Inkjet[] inkjets = new Inkjet[numInkjets];
		
		for(int i=0; i<numInkjets; i++){
			Node inkjetNode = nList.item(i);
			Element elem = (Element) inkjetNode;
			//p1
			String pointXStr = elem.getElementsByTagName("pointX").item(0).getTextContent();
			String pointYStr = elem.getElementsByTagName("pointY").item(0).getTextContent();
			String pointZStr = elem.getElementsByTagName("pointZ").item(0).getTextContent();
			//p2
			String point2XStr = elem.getElementsByTagName("point2X").item(0).getTextContent();
			String point2YStr = elem.getElementsByTagName("point2Y").item(0).getTextContent();
			String point2ZStr = elem.getElementsByTagName("point2Z").item(0).getTextContent();
			
			String startStr = elem.getElementsByTagName("start").item(0).getTextContent();
			String endStr = elem.getElementsByTagName("end").item(0).getTextContent();
			String widthStr = elem.getElementsByTagName("width").item(0).getTextContent();

			float[] point = new float[]{Float.valueOf(pointXStr), Float.valueOf(pointYStr), Float.valueOf(pointZStr)};
			float[] point2 = new float[]{Float.valueOf(point2XStr), Float.valueOf(point2YStr), Float.valueOf(point2ZStr)};

			double start = Double.valueOf(startStr);
			double end = Double.valueOf(endStr);
			int width = Integer.valueOf(widthStr);
			
			inkjets[i] = new Inkjet(point, point2, start, end, width);
		}
		System.out.println("Inkjets loaded from inkjets.xml");
		return inkjets;	
		
		} catch(Exception e) {
		e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	* exportInkjets
	*
	**/
	public static void exportInkjets(Inkjet[] inkjets, String filename) {
		filename = "../res/"+filename;
		try {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// root elements
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("inkjets");
		doc.appendChild(rootElement);

		// inkjets
		for(Inkjet inkjet : inkjets){
			Element inkjetElem = doc.createElement("inkjet");
			rootElement.appendChild(inkjetElem);

			//p1
			//X
			Element xElem = doc.createElement("pointX");
			xElem.appendChild(doc.createTextNode(Float.toString(inkjet.getP1()[0])));
			inkjetElem.appendChild(xElem);
			//Y
			Element yElem = doc.createElement("pointY");
			yElem.appendChild(doc.createTextNode(Float.toString(inkjet.getP1()[1])));
			inkjetElem.appendChild(yElem);
			//Z
			Element zElem = doc.createElement("pointZ");
			zElem.appendChild(doc.createTextNode(Float.toString(inkjet.getP1()[2])));
			inkjetElem.appendChild(zElem);
			
			//p2
			//X
			Element xElem2 = doc.createElement("point2X");
			xElem2.appendChild(doc.createTextNode(Float.toString(inkjet.getP2()[0])));
			inkjetElem.appendChild(xElem2);
			//Y
			Element yElem2 = doc.createElement("point2Y");
			yElem2.appendChild(doc.createTextNode(Float.toString(inkjet.getP2()[1])));
			inkjetElem.appendChild(yElem2);
			//Z
			Element zElem2 = doc.createElement("point2Z");
			zElem2.appendChild(doc.createTextNode(Float.toString(inkjet.getP2()[2])));
			inkjetElem.appendChild(zElem2);
			
			//start
			Element startElem = doc.createElement("start");
			startElem.appendChild(doc.createTextNode(Double.toString(inkjet.getStartTime())));
			inkjetElem.appendChild(startElem);
			//end
			Element endElem = doc.createElement("end");
			endElem.appendChild(doc.createTextNode(Double.toString(inkjet.getEndTime())));
			inkjetElem.appendChild(endElem);
			//width
			Element widthElem = doc.createElement("width");
			widthElem.appendChild(doc.createTextNode(Integer.toString(inkjet.getWidth())));
			inkjetElem.appendChild(widthElem);
			
		}

		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(filename));

		transformer.transform(source, result);
		System.out.println("Inkjets saved to "+filename);
		
		} catch (ParserConfigurationException pce) {
		pce.printStackTrace();
		} catch (TransformerException tfe) {
		tfe.printStackTrace();
		}
	}
}
