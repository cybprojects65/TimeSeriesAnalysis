package it.cnr.timeseries.analysis.test;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.cnr.timeseries.analysis.datastructures.AggregationFunctions;
import it.cnr.timeseries.analysis.signals.MathFunctions;
import it.cnr.timeseries.analysis.workflows.TimeSeriesAnalysis;
import it.cnr.timeseries.analysis.workflows.TimeSeriesAnalysisRO;

public class Main {
	
	
	public static void main(String args[]) throws Exception {
		
		//NOTE: the eigen corresponding to the threshold is excluded!
		
		//yyyy
		File timeSeriesTable = new File ("ctc_timeseries_haul10.csv");
		//MM/yyyy
		//File timeSeriesTable = new File ("ctc_timeseries_haul_10_month.csv");
		//MM/dd/yyyy
		//File timeSeriesTable = new File ("ctc_timeseries_haul_10_day.csv");
		//HH:mm:ss MM/dd/yyyy
		//File timeSeriesTable = new File ("ctc_timeseries_haul_10_seconds.csv");
		//HH:mm:ss:SSS
		//File timeSeriesTable = new File ("ctc_timeseries_haul_10_millisecs.csv");
		//yyyy + holes
		//File timeSeriesTable = new File ("ctc_timeseries_haul10_year_holes.csv");
		//File timeSeriesTable = new File ("ctc_timeseries_haul10_year_holes.csv");
		
		String timecolumn = "year";
		String valuecolumn = "biomindex";
		int SSAPointsToForecast = 1;
		float SSAEigenvaluesThreshold = 6f;
		int SSAAnalysisWindowSamples = 6;
		boolean adapt = false;
		
		if (args!=null && args.length>0) {
			timeSeriesTable = new File (args[0]);
			timecolumn = args[1];
			valuecolumn = args[2];
			SSAPointsToForecast = Integer.parseInt(args[3]);
			SSAEigenvaluesThreshold = Float.parseFloat(args[4]);
			SSAAnalysisWindowSamples = Integer.parseInt(args[5]);
			adapt = Boolean.parseBoolean(args[6]);
		}
		
		System.out.println("Parameters:");
		System.out.println("timeSeriesTable:"+timeSeriesTable.getName());
		System.out.println("timecolumn:"+timecolumn);
		System.out.println("valuecolumn:"+valuecolumn);
		System.out.println("SSAPointsToForecast:"+SSAPointsToForecast);
		System.out.println("SSAEigenvaluesThreshold:"+SSAEigenvaluesThreshold);
		System.out.println("SSAAnalysisWindowSamples:"+SSAAnalysisWindowSamples);
		System.out.println("adapt:"+adapt);
		
		
		//read input from file
		System.out.println("Reading input file");
		List<String> allLines = Files.readAllLines(timeSeriesTable.toPath());
		List<String> header = null;
		
		int timeIdx = -1;
		int valueIdx = -1;
		StringBuffer sb = new StringBuffer();
		sb.append("time,value\n");
		int timeseriesvaluecounter = 0;
		for (String s : allLines) {
			
			List<String> row = parseCSVString(s, ",");
			if (header == null) {
				header = row;
				int ch = 0;
				for (String h:header) {
					if (h.equals(timecolumn)) {
						timeIdx = ch;
						
					}
					if (h.equals(valuecolumn)) {
						valueIdx = ch;
					}
					
					ch++;	
				}	
			}else {
				
				String timeValue = row.get(timeIdx);
				try {
					double value = Double.parseDouble(row.get(valueIdx));
					sb.append(timeValue+","+value+"\n");
					timeseriesvaluecounter++;
				}catch(Exception e) {
				}
				
			}
		}

		System.out.println("Saving extraction of the time series");
		Path tempFile = Files.createTempFile(new File("./").toPath(), "temp.csv_", null);
		
		FileWriter fw = new FileWriter(tempFile.toFile());
		fw.write(sb.toString());
		fw.close();
		
		//SSA parameters
		timecolumn = "time";
		valuecolumn = "value";
		System.out.println("Executing SSA");
		if (adapt) {
			SSAEigenvaluesThreshold =0;
		}
		
		TimeSeriesAnalysisRO configuration = new TimeSeriesAnalysisRO(SSAAnalysisWindowSamples, SSAPointsToForecast, SSAEigenvaluesThreshold, 0, 0, 0, 0, tempFile.toFile(), valuecolumn, timecolumn, timeseriesvaluecounter, null);
		configuration = executeSSAAnalysis (configuration);
		
		double lowestError = Double.MAX_VALUE;
		TimeSeriesAnalysisRO optimalconfiguration = null;
		if (adapt) {
			
			System.out.println("Finding optimal window for the data at hand");
			for (int window = 1;window<(timeseriesvaluecounter);window++) {
				configuration.setSSAAnalysisWindowSamples(window);
				configuration = executeSSAAnalysis (configuration);
				System.out.print("Sample window "+window+" -> "+configuration.getAbsoluteError());
				if (!Double.isNaN(configuration.getAbsoluteError()) && configuration.getAbsoluteError()<lowestError) {
					System.out.println(" V");
					lowestError = configuration.getAbsoluteError();
					optimalconfiguration = configuration.clone();
				}
				System.out.println("");
			}
			
			System.out.println("#Optimal window estimated: "+optimalconfiguration.SSAAnalysisWindowSamples+"#");
			
			System.out.println("Finding optimal eigenvalues for the data at hand");
			//leave at least 2 eigenvectors
			float eigenvaluethresholdsPercentages [] = new float [optimalconfiguration.getEigenvalues().length-1]; ;
			eigenvaluethresholdsPercentages = Arrays.copyOfRange(optimalconfiguration.getEigenvalues(),2,optimalconfiguration.getEigenvalues().length);
			eigenvaluethresholdsPercentages [eigenvaluethresholdsPercentages.length-2] = 0;
			
			configuration = null; System.gc();
			configuration = optimalconfiguration.clone();
			
			for (double eigen : eigenvaluethresholdsPercentages) {
			
				configuration.setSSAEigenvaluesThreshold((float)eigen);
				configuration = executeSSAAnalysis (configuration);
				System.out.print("Perc eigen "+eigen+" (%) -> "+configuration.getAbsoluteError());
				if (configuration.getAbsoluteError()<lowestError) {
					System.out.println(" V");
					lowestError = configuration.getAbsoluteError();
					optimalconfiguration = configuration.clone();
				}else
					System.out.println("");
			}
			configuration = optimalconfiguration.clone();
			configuration = executeSSAAnalysis (configuration);
		}
		
		sb = new StringBuffer();
		sb.append("SSA samples = "+configuration.SSAAnalysisWindowSamples+"\n");
		sb.append("SSA forecast points = "+configuration.SSAPointsToForecast+"\n");
		sb.append("SSA eigenvalue threshold (%)= "+configuration.SSAEigenvaluesThreshold+"\n");
		sb.append("Number of eigenvectors used = "+configuration.getNumberOfEigenVectors()+"\n");
		sb.append("Total number of eigenvectors = "+configuration.eigenvalues.length+"\n");
		sb.append("Predicted value = "+configuration.predictedValue+"\n");
		sb.append("Last known value = "+configuration.lastknownvalue+"\n");
		sb.append("Absolute error = "+configuration.absoluteError+"\n");
		sb.append("Relative error (%) = "+configuration.relativeError+"\n");
		
		System.out.println("Summary:\n"+sb.toString());
		fw = new FileWriter("summary.txt");
		fw.write(sb.toString());
		fw.close();
		
		System.out.println("Output files:");
		System.out.println("Gap-filled time series image-> "+TimeSeriesAnalysis.uniformSignalImageFile);
		System.out.println("Gap-filled time series file -> "+TimeSeriesAnalysis.signalFile);
		System.out.println("Sample time series image-> "+TimeSeriesAnalysis.sampleSignalImageFile);
		System.out.println("Forecast time series image-> "+TimeSeriesAnalysis.forecastFile);
		if (configuration.getSSAPointsToForecast() == 0) {
			new File(TimeSeriesAnalysis.forecastFile).delete();
			new File(TimeSeriesAnalysis.forecastFile).createNewFile();
		}
		System.out.println("Forecast time series file -> "+TimeSeriesAnalysis.signalFile);
		System.out.println("Eigenvalues weights image-> "+TimeSeriesAnalysis.eigenValuesImgFile);
		System.out.println("Eigenvalues weights file-> "+TimeSeriesAnalysis.eigenvalueFile);
		
		System.out.println("Done.");
	}
	
	public static TimeSeriesAnalysisRO executeSSAAnalysis(TimeSeriesAnalysisRO object) throws Exception{
		
		System.out.println(">------------------------------------------------------------------>");
		new File(TimeSeriesAnalysis.forecastFile).delete();
		new File(TimeSeriesAnalysis.eigenvalueFile).delete();
		
		TimeSeriesAnalysis tsa = new TimeSeriesAnalysis();
		tsa.process(object.getInputFile(), object.getValuecolumn(), 
				object.getTimecolumn(), AggregationFunctions.SUM,  TimeSeriesAnalysis.Sensitivity.LOW,
					0, object.SSAAnalysisWindowSamples, object.SSAEigenvaluesThreshold, object.SSAPointsToForecast,
					true, false, true);
			
		List<String> allRows = Files.readAllLines(new File(TimeSeriesAnalysis.forecastFile).toPath());
		if (object.getSSAPointsToForecast()==0)
			allRows = Files.readAllLines(new File(TimeSeriesAnalysis.signalFile).toPath());
		
		List<String> allSignal = Files.readAllLines(new File(TimeSeriesAnalysis.signalFile).toPath());
		object.timeseriesLength = allSignal.size()-1;
		int timeseriesLengthIncludingHeader = object.timeseriesLength+1;
		
		List<String> allEigenvalues = Files.readAllLines(new File(TimeSeriesAnalysis.eigenvalueFile).toPath());
		float eigenvalues [] = new float[allEigenvalues.size()-1];
		int i = 0;
		for (String ei:allEigenvalues) {
			if (i>0)
				eigenvalues [i-1] = Float.parseFloat(ei);
			i++;
		}
		
		String lasttsrow = allRows.get(timeseriesLengthIncludingHeader-1);
		double lastknownvalue = Double.parseDouble(lasttsrow.split(",")[1]);
		double predictedValue = lastknownvalue;
		double absoluteError = 0;
		double relativeError = 0;
		
		if (allRows.size() > timeseriesLengthIncludingHeader) {
			String predicted = allRows.get(timeseriesLengthIncludingHeader);	
			predictedValue = Double.parseDouble(predicted.split(",")[1]);
			absoluteError = Math.abs(lastknownvalue - predictedValue);
			relativeError = MathFunctions.roundDecimal(Math.abs(absoluteError / lastknownvalue) * 100f, 2);
		}
		
		object.setLastknownvalue(lastknownvalue);
		object.setPredictedValue(predictedValue);
		object.setAbsoluteError(absoluteError);
		object.setRelativeError(relativeError);
		object.setEigenvalues(eigenvalues);
		System.out.println("<------------------------------------------------------------------<");
		return object;
	}

	public static List<String> parseCSVString(String row, String delimiter) throws Exception {

		List<String> elements = new ArrayList<String>();
		String phrase = row;
		int idxdelim = -1;
		boolean quot = false;
		phrase = phrase.trim();
		while ((idxdelim = phrase.indexOf(delimiter)) >= 0) {
			quot = phrase.startsWith("\"");
			if (quot) {
				phrase = phrase.substring(1);
				String quoted = "";
				if (phrase.startsWith("\""))
					phrase = phrase.substring(1);
				else{
					Pattern pattern = Pattern.compile("[^\\\\]\"", Pattern.CASE_INSENSITIVE);
					Matcher regexp = pattern.matcher(phrase);
			    
					boolean matching = regexp.find();

					if (matching) {
						int i0 = regexp.start(0);
						quoted = phrase.substring(0, i0 + 1).trim();
						phrase = phrase.substring(i0 + 2).trim();
					}
				}

				if (phrase.startsWith(delimiter))
					phrase = phrase.substring(1);

				elements.add(quoted);

			} else {
				elements.add(phrase.substring(0, idxdelim));
				phrase = phrase.substring(idxdelim + 1).trim();
			}
		}
		if (phrase.startsWith("\""))
			phrase = phrase.substring(1);

		if (phrase.endsWith("\""))
			phrase = phrase.substring(0, phrase.length() - 1);

		elements.add(phrase);

		return elements;
	}
	
	
}
