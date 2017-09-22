/**
* @Author:  Anthony Cohn-Richardby
**/
package generation;

import simulation.Inkjet;
import simulation.Scene;

import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;

public class InkjetGen {
	//Inkjetgen stuff
	public static final double X_MAX = Scene.TEXTURE_WIDTH;
	public static final double Y_MAX = Scene.TEXTURE_HEIGHT;
	
	MultivariateNormalDistribution clusterDistribution;
	double[][] clusterLocations;
	MultivariateNormalDistribution[] dists;
	
	Random rng = null;
	

	public InkjetGen() {	
		rng = new Random(System.nanoTime());
	}
	
	/**
	* generateClusterDistribution
	* Generates a multivariate normal distribution to be used when generating
	* inkjet cluster centers.
	* @param nClusters The number of cluster centres to generateClusterCentres
	* @param means The mean vector of the multivariate normal
	* @param covariances The covariances of the multivariate normal
	**/
	public double[][] generateClusterCentres(int nClusters, double[] means, double[][] covariances) {
		double[][] clusterLocations = new double[nClusters][2];
		clusterDistribution = new MultivariateNormalDistribution(means, covariances);
		for(int i=0; i<nClusters; i++) {
			//Keep generating until we get one within bounds.
			double[] sample = new double[2];
			do{
				sample = clusterDistribution.sample();
			}while(!(sample[0]>0 && sample[0]<X_MAX && sample[1]>0 && sample[1]<Y_MAX));
			clusterLocations[i] = sample;
		}
		
		return clusterLocations;
	}
	
	/**
	* generateClusteredLocations
	* Generate clusters of 2D coordinates based on the provided cluster centres
	**/
	public double[][][] generateClusteredPoints(double[][] clusterCentres, int numPerCluster, double largeVar, double smallVar, double distrebution) {
		//storae
		int numClusters = clusterCentres.length;
		double[][][] inkjetLocations = new double[numClusters][numPerCluster][2];
		
		//Need a mvn per cluster
		MultivariateNormalDistribution[] dists = new MultivariateNormalDistribution[numClusters];
		//Each with it's own mean and covariances
        double var;
		double[] clusterMean = new double[2];
		double[][] clusterCovariance = new double[2][2];
		
		//Initialize all of the MVNs with a mean, center with specified noise
		for(int i=0; i<numClusters; i++) {
            if(Math.random() < distrebution){
                //large blob
                var = largeVar;
            }
            else{
                //small blob
                var = smallVar;
            }
			clusterMean[0] = clusterCentres[i][0];
			clusterMean[1] = clusterCentres[i][1];
			clusterCovariance[0][0] = var;
			clusterCovariance[0][1] = 0;
			clusterCovariance[1][0] = 0;
			clusterCovariance[1][1] = var;

			dists[i] = new MultivariateNormalDistribution(clusterMean, clusterCovariance);
		}
		
		//For each cluster, generate numPerCluster of inkjets using that cluster's mvn
		for(int i=0; i<numClusters; i++){
			for(int j=0; j<numPerCluster; j++){
				//Keep sampling until in bounds, as long as the clusters aren't out of bounds this will terminate
				double[] sample = new double[2];
				do{
					sample = dists[i].sample();
				}while(!(sample[0]>0 && sample[0]<X_MAX && sample[1]>0 && sample[1]<Y_MAX));
				inkjetLocations[i][j] = sample;
				//System.out.println(cluster);
				//System.out.println(i+": "+inkjetLocations[i][0]+", "+inkjetLocations[i][1]);
			}
		}
		
		return inkjetLocations;
	}

	/**
	* toInkjets
	* Convert a list of 2D points to inkjets going from this point to the origin.
	**/
	public Inkjet[] toInkjets(double[][][] clusteredPoints, int inkjetWidth) {
        double disjoint = 0.01;
		//TODO: make this variable 
		double maxDuration = 0.01;
		double startNoise = 0.05;
		double durationNoise = 0.005;
		int numClusters = clusteredPoints.length;
		ArrayList<Inkjet> inkjets = new ArrayList<Inkjet>();
		Inkjet[] inkjetsArray;
		
		//Iterate per cluster
		int numPoints;
		float[] p1;
		float[] p2;
		//Averaege cluster timeframe
		double minClusterDuration = 1.0/numClusters;
        double maxClusterDuration = 1.0;
        double minClusterCenter;
        double maxClusterCenter = 0.5;
        double clusterCenter;
        double clusterDuration;
        double clusterStart;

		for(int i=0; i<numClusters; i++){
			numPoints = clusteredPoints[i].length;
            //Calculate cluster time period:
            minClusterCenter = ((i*minClusterDuration)+((i+1)*minClusterDuration))/2;
            clusterCenter = minClusterCenter + disjoint*(maxClusterCenter-minClusterCenter);
            clusterDuration = minClusterDuration + disjoint*(maxClusterDuration-minClusterDuration);
            clusterStart = clusterCenter - 0.5*clusterDuration;
            
			for(int j=0; j<numPoints; j++){
				p1 = to3DSphere(clusteredPoints[i][j]);
				p2 = new float[]{0, 0, 0};
				inkjets.add(new Inkjet(p1, p2, clusterStart, clusterStart+clusterDuration, inkjetWidth));
			}
		}
		
		inkjetsArray = inkjets.toArray(new Inkjet[inkjets.size()]); //??? just casting inkjets.toArray throws an exception
		return inkjetsArray;
	}
	
	/**
	* to3D
	* Convert the 2D inkjet locations to 3D points on the surface of a sphere.
	* @param inkjetLocations the 2D locations of all inkjets
	* @return An array of 3D points on the surface of a sphere.
	**/
	private double[] to3DCylinder(double[] inkjetLocation) {
		double radius = 50;
		double height = 80;
		
		double[] uv = toUV(inkjetLocation);
		double[] point = new double[3];
		

		double x = radius*Math.cos(2*Math.PI*uv[0]);
		double y = height*(uv[1]-1)+38;
		double z = radius*Math.sin(2*Math.PI*uv[0]);
		
		point = new double[]{x, y, z};
		
		return point;
	}
	
	/**
	* to3D
	* Convert the 2D inkjet locations to 3D points on the surface of a sphere.
	* @param inkjetLocations the 2D locations of all inkjets
	* @return An array of 3D points on the surface of a sphere.
	**/
	private float[] to3DSphere(double[] inkjetLocation) {
		double radius = 50;
		
		double[] uv = toUV(inkjetLocation);
		float[] point = new float[3];
		
		double theta = 2 * Math.PI * uv[0];
		double phi = Math.PI * uv[1];
		
		//See comments in toUV
		phi += Math.PI;
		

		float x = (float) (Math.cos(theta) * Math.sin(phi) * radius);
		float z = (float) (Math.sin(theta) * Math.sin(phi) * radius);
		float y = (float) (Math.cos(phi) * radius);
		
		point = new float[]{x, y, z};
		
		return point;
	}
	
	/**
	* toUV
	* Converts generated inkjets to UV coordinates between 0 and 1.
	* @param inkjetLocations inkjet locations from (0, 0) to (X_MAX, Y_MAX)
	* @return uvs from (0, 0) to (1, 1) ready to be used in placing inkjets on the surface of a sphere.
	**/
	private double[] toUV(double[] inkjetLocation) {
		double[] uv = new double[2];
		
		uv[0] = (inkjetLocation[0]/X_MAX);
		//flip around X=0.50 - so they appear where the user would expect them to.
		uv[0] -= 0.5;
		uv[0] = -uv[0];
		uv[0] += 0.5;
		//same around y=0.50
		uv[1] = inkjetLocation[1]/Y_MAX;
		uv[1] -= 0.5;
		uv[1] = -uv[1];
		uv[1] += 0.5;
		
		return uv;
	}
	
	/**
	* Test harness
	**/
	public static void main(String[] args) {
		InkjetGen gen = new InkjetGen();
	}
}
