package algo;

import dm.Source;
import dm.Source.Type;

/**
 * The interface that source classification algorithms must adhere to.
 * 
 * @author nrowell
 */
public interface SourceClassifier {

	/**
	 * Classify the given {@link Source}.
	 * @param source
	 * 	The {@link Source} to classify.
	 * @return
	 * 	The {@link Source.Type} of the {@link Source}.
	 */
	public Type classifySource(Source source);
}