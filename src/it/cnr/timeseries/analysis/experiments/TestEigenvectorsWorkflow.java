package it.cnr.timeseries.analysis.experiments;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import it.cnr.timeseries.analysis.datastructures.AggregationFunctions;
import it.cnr.timeseries.analysis.signals.MathFunctions;
import it.cnr.timeseries.analysis.workflows.TimeSeriesAnalysis;

public class TestEigenvectorsWorkflow {

	
	public static void main(String args[]) throws Exception{
		
		//File timeSeriesTable = new File("ctc_timeseries_haul10.csv");
		//File timeSeriesTable = new File("ctc_timeseries_haul10_2018.csv");
		File timeSeriesTable = new File("D:\\WorkFolder\\Experiments\\EcologicalModelling Solemon\\SSA\\Sepia_officinalis\\Serie stazioni mancanti fino al 2018\\s32.csv");
		
		
		String valuescolum="biomindex";
		String timecolumn="year";
		AggregationFunctions aggregationFunction = AggregationFunctions.SUM;
		TimeSeriesAnalysis.Sensitivity sensitivityP = TimeSeriesAnalysis.Sensitivity.LOW;
		int fftwindowsamples = 3; 
		 
		boolean forceUniformSampling=true; 
		boolean doFourierAnalysis = false;
		boolean doSSA = true;
		
		float SSAEigenvaluesThreshold = 0.8f;
		//float SSAEigenvaluesThreshold = 2.341459297264286E-15f;
		//float SSAEigenvaluesThreshold = 0;
		//float SSAEigenvaluesThreshold = 10f;
		double trueValue = 13.83733273;
		int SSAAnalysisWindowSamples = 8;
		
		
		int SSAPointsToForecast=1;
		TimeSeriesAnalysis tsa = new TimeSeriesAnalysis();
		tsa.process(timeSeriesTable, valuescolum, timecolumn, aggregationFunction, sensitivityP, fftwindowsamples, SSAAnalysisWindowSamples, SSAEigenvaluesThreshold, SSAPointsToForecast, forceUniformSampling, doFourierAnalysis, doSSA);
		//double trueValue = 86.63373566;
		//double trueValue = 0;
		
		
		List<String> allRows = Files.readAllLines(new File(tsa.forecastFile).toPath());
		String predicted = allRows.get(allRows.size()-1);
		predicted = predicted.substring(predicted.indexOf(",")+1);
		double predictedValue = Double.parseDouble(predicted);
		double absoluteError = (trueValue-predictedValue);
		double relativeError = MathFunctions.roundDecimal(Math.abs(absoluteError/trueValue)*100f,2);
		System.out.println("Predicted "+predictedValue+" vs "+trueValue);
		System.out.println("Absolute Error "+absoluteError+"; Relative Error "+relativeError+"%");
		
	}
	
}
