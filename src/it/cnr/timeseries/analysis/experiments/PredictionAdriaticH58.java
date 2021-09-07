package it.cnr.timeseries.analysis.experiments;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import it.cnr.timeseries.analysis.datastructures.AggregationFunctions;
import it.cnr.timeseries.analysis.signals.MathFunctions;
import it.cnr.timeseries.analysis.workflows.TimeSeriesAnalysis;

public class PredictionAdriaticH58 {

	
	public static void main(String args[]) throws Exception{
		
		File timeSeriesTable = new File("D:\\WorkFolder\\Experiments\\EcologicalModelling Solemon\\SSA\\Sepia_officinalis\\Serie stazioni mancanti fino al 2019\\s58.csv");
		TimeSeriesAnalysis tsa = new TimeSeriesAnalysis();
		String valuescolum="biomindex";
		String timecolumn="year";
		AggregationFunctions aggregationFunction = AggregationFunctions.SUM;
		TimeSeriesAnalysis.Sensitivity sensitivityP = TimeSeriesAnalysis.Sensitivity.LOW;
		int fftwindowsamples = 3; 
		
		boolean forceUniformSampling=true; 
		boolean doFourierAnalysis = false;
		boolean doSSA = true;
		
		float SSAEigenvaluesThreshold = 5.431453022831493E-16f;
		
				
		int SSAAnalysisWindowSamples = 12;
		double trueValue = 72.96804047;
		
		int SSAPointsToForecast=1;
		
		tsa.process(timeSeriesTable, valuescolum, timecolumn, aggregationFunction, sensitivityP, fftwindowsamples, SSAAnalysisWindowSamples, SSAEigenvaluesThreshold, SSAPointsToForecast, forceUniformSampling, doFourierAnalysis, doSSA);
		
		
		List<String> allRows = Files.readAllLines(new File(tsa.forecastFile).toPath());
		String predicted = allRows.get(allRows.size()-1);
		predicted = predicted.substring(predicted.indexOf(",")+1);
		double predictedValue = Double.parseDouble(predicted);
		double absoluteError = (trueValue-predictedValue);
		double relativeError = MathFunctions.roundDecimal(Math.abs(absoluteError/trueValue)*100f,2);
		
		System.out.println("Predicted "+predictedValue+" vs previous value "+trueValue);
		System.out.println("Absolute Error "+absoluteError+"; Relative Error "+relativeError+"%");
		
	}
	
}
