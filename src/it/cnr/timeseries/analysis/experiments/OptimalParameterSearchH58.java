package it.cnr.timeseries.analysis.experiments;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import it.cnr.timeseries.analysis.datastructures.AggregationFunctions;
import it.cnr.timeseries.analysis.signals.MathFunctions;
import it.cnr.timeseries.analysis.workflows.TimeSeriesAnalysis;

public class OptimalParameterSearchH58 {

	
	public static void main(String args[]) throws Exception{
		
		//File timeSeriesTable = new File("ctc_timeseries_haul10.csv");
		File timeSeriesTable = new File("ctc_timeseries_haul10_2018.csv");
		//File timeSeriesTable = new File("D:\\WorkFolder\\Experiments\\EcologicalModelling Solemon\\SSA\\Sepia_officinalis\\Serie stazioni mancanti fino al 2018\\s58.csv");
		
		double trueValue = 72.96804047;
		
		String valuescolum="biomindex";
		String timecolumn="year";
		AggregationFunctions aggregationFunction = AggregationFunctions.SUM;
		TimeSeriesAnalysis.Sensitivity sensitivityP = TimeSeriesAnalysis.Sensitivity.LOW;
		int fftwindowsamples = 3; 
		 
		boolean forceUniformSampling=true; 
		boolean doFourierAnalysis = false;
		boolean doSSA = true;
		
		float SSAEigenvaluesThreshold = 0.1f;

		
		int bestwindow = 0;
		double bestrelativeerror = 200;
		int SSAPointsToForecast=1;
		
		for (int i = 1;i<100;i++)
		//int i = 8;
		{
			
		//int SSAAnalysisWindowSamples = 10;//12;
		int SSAAnalysisWindowSamples = i;
		System.out.println("####Testing "+i+" Window samples");
		TimeSeriesAnalysis tsa = new TimeSeriesAnalysis();
		try {
		
		tsa.process(timeSeriesTable, valuescolum, timecolumn, aggregationFunction, sensitivityP, fftwindowsamples, SSAAnalysisWindowSamples, SSAEigenvaluesThreshold, SSAPointsToForecast, forceUniformSampling, doFourierAnalysis, doSSA);
		}catch(Exception e) {
			break;
		}
		//double trueValue = 86.63373566;
		//double trueValue = 0;
		
		
		List<String> allRows = Files.readAllLines(new File(tsa.forecastFile).toPath());
		String predicted = allRows.get(allRows.size()-1);
		predicted = predicted.substring(predicted.indexOf(",")+1);
		double predictedValue = Double.parseDouble(predicted);
		double absoluteError = (trueValue-predictedValue);
		double relativeError = MathFunctions.roundDecimal(Math.abs(absoluteError/trueValue)*100f,2);
		
		System.out.println("SSA samples: "+i);
		System.out.println("Predicted "+predictedValue+" vs "+trueValue);
		System.out.println("Absolute Error "+absoluteError+"; Relative Error "+relativeError+"%");
		
		if (!Double.isNaN(predictedValue) && relativeError<bestrelativeerror)
		{
			bestrelativeerror = relativeError;
			bestwindow = SSAAnalysisWindowSamples;
			
		}
		
		}
		
		TimeSeriesAnalysis tsa = new TimeSeriesAnalysis();
		tsa.process(timeSeriesTable, valuescolum, timecolumn, aggregationFunction, sensitivityP, fftwindowsamples, bestwindow, SSAEigenvaluesThreshold, SSAPointsToForecast, forceUniformSampling, doFourierAnalysis, doSSA);
		
		System.out.println("#######################");
		System.out.println("BEST SSA window: "+bestwindow+ " Error "+bestrelativeerror+"%");
		System.out.println("#######################");
		System.out.println("");
		
		List<String> allEigen = Files.readAllLines(new File(tsa.eigenvalueFile).toPath());
		int besteigen = 0;
		double besteigenthreshold = 0;
		bestrelativeerror = 200;
		
		for (int i = 2;i<allEigen.size();i++)
			//int i = 8;
			{
			
			//int SSAAnalysisWindowSamples = 10;//12;
			int SSAAnalysisWindowSamples = bestwindow;
			SSAEigenvaluesThreshold = Float.parseFloat(allEigen.get(i));
			System.out.println("####Testing "+i+" Eigenvalues with threshold "+SSAEigenvaluesThreshold);
			TimeSeriesAnalysis tsae = new TimeSeriesAnalysis();
			try {
			
			tsae.process(timeSeriesTable, valuescolum, timecolumn, aggregationFunction, sensitivityP, fftwindowsamples, SSAAnalysisWindowSamples, SSAEigenvaluesThreshold, SSAPointsToForecast, forceUniformSampling, doFourierAnalysis, doSSA);
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			
			List<String> allRows = Files.readAllLines(new File(tsa.forecastFile).toPath());
			String predicted = allRows.get(allRows.size()-1);
			predicted = predicted.substring(predicted.indexOf(",")+1);
			double predictedValue = Double.parseDouble(predicted);
			double absoluteError = (trueValue-predictedValue);
			double relativeError = MathFunctions.roundDecimal(Math.abs(absoluteError/trueValue)*100f,2);
			
			System.out.println("SSA eigen N.: "+(i-1));
			System.out.println("SSA eigen T.: "+SSAEigenvaluesThreshold);
			System.out.println("Predicted "+predictedValue+" vs "+trueValue);
			System.out.println("Absolute Error "+absoluteError+"; Relative Error "+relativeError+"%");
			
			if (!Double.isNaN(predictedValue) && relativeError<bestrelativeerror) {
				besteigen = i;
				besteigenthreshold =SSAEigenvaluesThreshold; 
				bestrelativeerror = relativeError;
			}
			}
		System.out.println("#############");
		System.out.println("BEST SSA eigen N.: "+(besteigen-1));
		System.out.println("BEST SSA eigen T.: "+besteigenthreshold);
		System.out.println("BEST SSA eigen Error.: "+bestrelativeerror+"%");
		
	}
	
}
