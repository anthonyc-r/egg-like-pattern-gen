/**
* @Author: Anthony Cohn-Richardby
**/
package generation;

import simulation.RotationHandler;

import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Random;

import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.VectorUtil;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.distribution.MixtureMultivariateNormalDistribution;
import org.apache.commons.math3.util.Pair;

public class RotationGen {
	public static double STARTING_THETA = 0;
	public static double STARTING_PHI = Math.PI/2;	
	
	private double theta;
	private double phi;
	
	private Random rng;
		
	public RotationGen() {
		theta = STARTING_THETA;
		phi = STARTING_PHI;
		rng = new Random(System.nanoTime());
	}
	
	/**
	* genRotations
	* Generates random rotations distrebuted according to the mixture model provided.
	**/
	public TreeMap<Double, Quaternion> genRotations(double maxTheta, double maxThetaChange, double maxPhi, double maxPhiChange, int numRotations,
														MixtureMultivariateNormalDistribution mixtureDistr) {
															
		TreeMap<Double, Quaternion> generated = new TreeMap<Double, Quaternion>();
		
		//initial state
		Quaternion initialQ = genRandomQuaternion(maxTheta, maxThetaChange, maxPhi, maxPhiChange);
		generated.put(0.0, initialQ);
		
		//Sample the mixture n times
		double time;
        double timeStep = 1.0/numRotations;
		for(int i=1; i<numRotations-1; i++){
			//Get a valid time for each rotation
			/*do{
				time = mixtureDistr.sample()[0];
			}while(time<=0 || time>=1.0);*/
            time = i*timeStep;
			//Add quaternion
			Quaternion rotation = genRandomQuaternion(maxTheta, maxThetaChange, maxPhi, maxPhiChange);
			generated.put(time, rotation);
		}
		//Final state.
		Quaternion finalQ = genRandomQuaternion(maxTheta, maxThetaChange, maxPhi, maxPhiChange);
		generated.put(1.0, finalQ);
		
		//Reset theta and phi since we're done
		theta = STARTING_THETA;
		phi = STARTING_PHI;
		
		return generated;
	}
	
	/**
	* genRandomQuaternion
	* Returns a random Quaternion within the maximum rotation angle
	* Quaternion is generated from local axes
	* The y axis is derived from a point on a circle circumference
	* The z acis is the vector at a tangent to this point
	* The x axis is derived from the cross product
	**/
	private Quaternion genRandomQuaternion(double maxTheta, double maxThetaChange, double maxPhi, double maxPhiChange) {
		Quaternion q = new Quaternion();

		//Add some random value between -maxAngleChange and max angle to the spherical coord.
		theta += (2*rng.nextDouble()*maxThetaChange)-maxThetaChange;
		phi += (2*rng.nextDouble()*maxPhiChange)-maxPhiChange;
		//Limit it 
		if(theta > STARTING_THETA+maxTheta){
			theta = STARTING_THETA+maxTheta;
		}
		else if(theta < STARTING_THETA-maxTheta){
			theta = STARTING_THETA-maxTheta;
		}
		if(phi > STARTING_PHI+maxPhi){
			phi = STARTING_PHI+maxPhi;
		}
		else if(phi < STARTING_PHI-maxPhi){
			phi = STARTING_PHI-maxPhi;
		}
		
		//Y axis is just derived from a point on a sphere
		float[] yAxis = {(float) (Math.cos(theta)*Math.cos(phi)),
							(float) Math.sin(phi),
							(float) (Math.sin(theta)*Math.cos(phi))};
		//z is 90 degrees out of phase on both axes
		float[] zAxis = {(float) (Math.cos(theta-(Math.PI/2))*Math.cos(phi+(Math.PI/2))),
							(float) Math.sin(phi-(Math.PI/2)),
							(float) (Math.sin(theta-(Math.PI/2))*Math.cos(phi+(Math.PI/2)))};
		//Just set x to be an axis perpendicular to both
		float[] xAxis = new float[3];
		VectorUtil.crossVec3(xAxis, yAxis, zAxis);
		q = q.setFromAxes(xAxis, yAxis, zAxis);
		
		return q;	
	}

	
	/**
	* test harness
	**/
	public static void main(String[] args) {
	}
}
