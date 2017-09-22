/**
* @Author: Anthony Cohn-Richardby
**/
package simulation;

import com.jogamp.opengl.math.Quaternion;

import java.util.TreeMap;
import java.util.Map;

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

public class RotationHandler {
	public static final String RESOURCE_LOCATION = "../res/";
	
	private TreeMap<Double, Quaternion> rotations = null;
	private float[] currentRotationMat = null;
	private float[] currentInverseMat = null;
	
	
	public RotationHandler(String filename) {
		currentRotationMat = new float[16];
		currentInverseMat = new float[16];
		importRotations(filename);
	}
	
	/**
	* update takes time as a percentage of completion 0.0-1.0
	**/
	public void update(double time) {
		
		Quaternion rotation = new Quaternion();
		Quaternion invRotation = new Quaternion();
		
		//Get they key/value >= current time
		Map.Entry<Double, Quaternion> prevEntry = rotations.floorEntry(time);
		//Get the entry after current time
		Map.Entry<Double, Quaternion> nextEntry = rotations.higherEntry(time);
		
		
		if(prevEntry == null || nextEntry == null) {
			System.out.println("Error: last time or next time not set");
			return;
		}
		else{
			//System.out.println("prevTime: "+lastTime+", nextTime: "+nextTime);
			//Find out what pc through the current rotation interval we are...
			double timeDiff = time - prevEntry.getKey();
			//jogl uses floats...
			float pcDiff = (float) (timeDiff/(nextEntry.getKey()-prevEntry.getKey()));
			//System.out.println("pc Diff: "+pcDiff);
			
			//Set it to the interpolated point between rotations
			rotation.setSlerp(prevEntry.getValue(), nextEntry.getValue(), pcDiff);
			//init the inverse to be the rotation and invert it (for use in drawing upon egg)
			invRotation.set(rotation);
			invRotation.invert();
			//Convert to matrix for glMult
			rotation.toMatrix(currentRotationMat, 0);
			invRotation.toMatrix(currentInverseMat, 0);
		}
	}
	
	public float[] getRotation() {
		return currentRotationMat;
	}
	
	public float[] getInverseRotation() {
		return currentInverseMat;
	}
	
	/**
	* exportRotations
	*
	**/
	public static void exportRotations(TreeMap<Double, Quaternion> rotations, String filename) {
		filename = RESOURCE_LOCATION+filename;
		try {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// root elements
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("rotations");
		doc.appendChild(rootElement);

		// rotations <time, quat>
		for(Map.Entry<Double, Quaternion> entry : rotations.entrySet()){
			//get set values
			double time = entry.getKey();
			Quaternion q = entry.getValue();
			//add time as an attribute to each rot
			Element rotationElem = doc.createElement("rotation");
			rotationElem.setAttribute("time", String.valueOf(time));
			rootElement.appendChild(rotationElem);

			//quaternion is added as an element
			Element quaternionElem = doc.createElement("quaternion");
			//x, y, z, w as attributes
			quaternionElem.setAttribute("w", String.valueOf(q.getW()));
			quaternionElem.setAttribute("x", String.valueOf(q.getX()));
			quaternionElem.setAttribute("y", String.valueOf(q.getY()));
			quaternionElem.setAttribute("z", String.valueOf(q.getZ()));
			rotationElem.appendChild(quaternionElem);
		}

		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(filename));

		transformer.transform(source, result);
		System.out.println("Rotations saved to "+filename);
		
		} catch (ParserConfigurationException pce) {
		pce.printStackTrace();
		} catch (TransformerException tfe) {
		tfe.printStackTrace();
		}
	}
	
	/**
	* importRotations
	* import a set of rotations from the xml file
	**/
	public void importRotations(String filename) {
		try {
		filename = RESOURCE_LOCATION+filename;
		File inkjetsXml = new File(filename);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(inkjetsXml);
		
		NodeList nList = doc.getElementsByTagName("rotation");
		int numRots = nList.getLength();
		rotations = new TreeMap<Double, Quaternion>();
		
		Node rotNode = null;
		Element rotElem = null;
		Element quatElem = null;
		double time;
		Quaternion q = null;
		
		
		for(int i=0; i<numRots; i++){
			q = new Quaternion();
			rotNode = nList.item(i);
			rotElem = (Element) rotNode;
			quatElem = (Element) rotElem.getElementsByTagName("quaternion").item(0);
			time = Double.valueOf(rotElem.getAttribute("time"));
			q.setW(Float.valueOf(quatElem.getAttribute("w")));
			q.setX(Float.valueOf(quatElem.getAttribute("x")));
			q.setY(Float.valueOf(quatElem.getAttribute("y")));
			q.setZ(Float.valueOf(quatElem.getAttribute("z")));
			
			rotations.put(time, q);
		}
		System.out.println("Rotations loaded from "+filename);	
		} catch(Exception e) {
		e.printStackTrace();
		}
		
		update(0);
	}

	/**
	* test harness
	**/
	public static void main(String[] args) {
		RotationHandler roth = new RotationHandler("rotations.xml");
	}
}