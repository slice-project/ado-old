package org.etri.ado.app;

import org.nd4j.linalg.api.ndarray.INDArray;

public interface Agent {	
	INDArray getObservations();
	void setActions(INDArray actions);	
}
