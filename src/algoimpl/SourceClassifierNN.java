package algoimpl;

import algo.SourceClassifier;
import dm.NeuralNetwork;
import dm.Source;
import dm.Source.Type;

/**
 * {@link SourceClassifier} implementation based on a {@link NeuralNetwork}.
 *
 * TODO: hardcode the parameters for the neural network.
 *
 * @author nrowell
 * @version $Id$
 */
public class SourceClassifierNN implements SourceClassifier {

	/**
	 * The {@link NeuralNetwork} used to perform the classification.
	 */
	NeuralNetwork neuralNetwork;
	
	/**
	 * The parameters of the {@link NeuralNetwork}.
	 */
	double[] params = new double[]{1.1342973367371332, -2.686399965949776, -1.183947600746605, -1.2909343864147822, 1.220267272296525, 1.2722888159185817, -0.5785770637784854, 0.9399417262035493, -0.29411436417652, -0.2686562238985049, 0.8225590961571908, 0.8163491807820592, -2.417196242984236, 1.1391005264312741, -0.8781652180198124, -1.0294356583396442, 4.161501566817192, -1.5665312874662265, -1.410515258327577, 4.2321684981157, 0.03012668407874716, 0.5343032081772355, 0.9452803696986759, 1.0513098301544503, -0.32731564850346034, 0.046251759073640236, -4.8955557598415975, 1.2579438659696651, 0.0974370996575036, 1.4582193810401405, 1.0825952532575551, 1.480833734939218, 0.9108362473383418, 0.15330348846047617, -4.470348331090813, 1.0313164017306462, -6.362317558798425, 0.11034741479027868, 2.568858682692154, 2.285788610562752, -0.3376578088344199, 2.707312816608264, 0.010482934203044419, -5.58497375933043, 2.1876543221562317, -0.855300570993043, 2.963581734985444, -0.5353876346111963, 2.3753511749058265, -5.198915679333386, -1.1555220768194827, -0.632136621634756, -0.9102443940795066, -1.9328088494609266, -1.461761040745588, -1.443327142703491, -0.6371866584321354, -1.9994449672909955, -1.8927941075681458, -0.8667008159336119, -1.0814995173326556, -2.0848783734590293, -0.3882575363829958, -1.7164112366648014, -0.5326624654124, -1.8537129002252917};
	
	/**
	 * Main constructor for the {@link SourceClassifierNN}.
	 */
	public SourceClassifierNN() {
		neuralNetwork = new NeuralNetwork(new int[]{4,4,6}, 3, params);
	}
	
	@Override
	public Type classifySource(Source source) {
		
		// Build the input array for the neural network
		double[] input = new double[3];
		input[0] = source.getFluxRatio();
		input[1] = source.getEigenvalues()[0];
		input[2] = source.getEigenvalues()[1];
		
		// Compute the output of the network
		double[] output = neuralNetwork.getNetworkOutput(input);
		
		// Get the source classification from the largest output value
		int maxIdx = 0;
		double max = output[0];
		for(int i=0; i<output.length; i++) {
			if(output[i] > max) {
				max = output[i];
				maxIdx = i;
			}
		}
		
		// The variable maxIdx contains the index of the largest value in the output array.
		// This tells us the classification for this source.
		return Type.values()[maxIdx];
	}
}