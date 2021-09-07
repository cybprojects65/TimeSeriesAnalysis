package it.cnr.timeseries.analysis.workflows;

import java.awt.Image;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import javax.imageio.ImageIO;

import it.cnr.timeseries.analysis.datastructures.AggregationFunctions;
import it.cnr.timeseries.analysis.datastructures.DateGuesser;
import it.cnr.timeseries.analysis.datastructures.ImageTools;
import it.cnr.timeseries.analysis.datastructures.Tuple;
import it.cnr.timeseries.analysis.signals.MathFunctions;
import it.cnr.timeseries.analysis.signals.PeriodicityDetector;
import it.cnr.timeseries.analysis.signals.SignalProcessing;
import it.cnr.timeseries.analysis.signals.TimeSeries;
import it.cnr.timeseries.analysis.ssa.SSADataset;
import it.cnr.timeseries.analysis.ssa.SSAWorkflow;

public class TimeSeriesAnalysis {

	private Image signalImg = null;
	private Image uniformSignalImg = null;
	private Image uniformSignalSamplesImg = null;
	private Image spectrogramImg = null;
	private Image forecastsignalImg = null;
	private Image eigenValuesImg = null;
	private File outputfilename = null;
	public static boolean display = false;
	private static int maxpoints = 10000;

	public static String uniformSignalImageFile = "signal_u.png";
	public static String sampleSignalImageFile = "signal_samples_u.png";
	public static String spectrogramImgFile = "spectrogram.png";
	public static String forecastsignalImgFile = "forecast.png";
	public static String eigenValuesImgFile = "eigenvalues.png";
	public static String signalFile = "signal_u.csv";
	public static String forecastFile = "forecast.csv";
	public static String eigenvalueFile = "eigenvalues.csv";

	public LinkedHashMap<String, String> outputParameters = new LinkedHashMap<>();

	public enum Sensitivity {
		LOW, NORMAL, HIGH
	}

	public void process(File timeSeriesTable, String valuescolum, String timecolumn, AggregationFunctions aggregationFunction, Sensitivity sensitivityP, int fftwindowsamples, int SSAAnalysisWindowSamples, float SSAEigenvaluesThreshold, int SSAPointsToForecast, boolean forceUniformSampling, boolean doFourierAnalysis, boolean doSSA) throws Exception {

		String tablename = timeSeriesTable.getName();
		String aggregationFunc = aggregationFunction.name();
		String fftwindowsamplesS = "" + fftwindowsamples;
		int windowLength = SSAAnalysisWindowSamples;
		float eigenvaluespercthr = SSAEigenvaluesThreshold;
		int pointsToReconstruct = SSAPointsToForecast;

		float sensitivity = 9;
		switch (sensitivityP) {
		case LOW:
			sensitivity = 9;
			break;
		case NORMAL:
			sensitivity = 5;
			break;
		case HIGH:
			sensitivity = 1;
			break;
		}

		int fftWindowSamplesDouble = 1;
		if (timecolumn == null)
			timecolumn = "time";
		if (aggregationFunc == null)
			aggregationFunc = "SUM";
		if (fftwindowsamplesS != null) {
			try {
				fftWindowSamplesDouble = Integer.parseInt(fftwindowsamplesS);
			} catch (Exception e) {
			}
		}

		System.out.println("TimeSeriesAnalysis->Table Name: " + tablename);
		System.out.println("TimeSeriesAnalysis->Time Column: " + timecolumn);
		System.out.println("TimeSeriesAnalysis->Values Column: " + valuescolum);
		System.out.println("TimeSeriesAnalysis->Aggregation: " + aggregationFunc);
		System.out.println("TimeSeriesAnalysis->FFT Window Samples: " + fftWindowSamplesDouble);
		System.out.println("TimeSeriesAnalysis->SSA Window Samples: " + windowLength);
		System.out.println("TimeSeriesAnalysis->SSA Eigenvalues threshold: " + eigenvaluespercthr);
		System.out.println("TimeSeriesAnalysis->SSA Points to Reconstruct: " + pointsToReconstruct);

		System.out.println("TimeSeriesAnalysis->Extracting Points...");

		List<String> allRows = Files.readAllLines(timeSeriesTable.toPath());
		String[] firstRow = allRows.get(0).split(",");
		int timeIdx = -1;
		int valueIdx = -1;
		int counter = 0;
		for (String f : firstRow) {

			if (f.equals(timecolumn))
				timeIdx = counter;
			if (f.equals(valuescolum))
				valueIdx = counter;

			counter++;
		}
		if (timeIdx == -1 || valueIdx == -1)
			throw new Exception("Could not find input time or value column, check the CSV file");

		// build signal
		System.out.println("TimeSeriesAnalysis->Building signal");
		List<Tuple<String>> signal = new ArrayList<Tuple<String>>();
		int sizesignal = 0;

		for (String row : allRows) {
			if (sizesignal > 0) {
				Object[] srow = row.split(",");
				String value = "" + srow[valueIdx];
				String time = "" + srow[timeIdx];
				signal.add(new Tuple<String>(time, value));
			}
			sizesignal++;
		}

		System.out.println("TimeSeriesAnalysis->Signal built with success. Size: " + sizesignal);
		System.out.println("TimeSeriesAnalysis->Building Time Series");

		TimeSeries ts = TimeSeries.buildFromSignal(signal);
		String timepattern = ts.getTimepattern();
		System.out.println("TimeSeriesAnalysis->Detected time pattern " + timepattern);
		String chartpattern = "MM-dd-yy";
		if (timepattern.equals("s") || (DateGuesser.isJavaDateOrigin(ts.getTime()[0]) && DateGuesser.isJavaDateOrigin(ts.getTime()[ts.getTime().length - 1]))) {
			System.out.println("TimeSeriesAnalysis->Changing chart pattern to seconds");
			chartpattern = "HH:mm:ss:SS";
		} else
			System.out.println("TimeSeriesAnalysis->Chart pattern remains " + chartpattern);

		System.out.println("TimeSeriesAnalysis->Uniformly sampling the signal");
		if (display)
			SignalProcessing.displaySignalWithTime(ts.getValues(), ts.getTime(), "Time Series", chartpattern);

		signalImg = SignalProcessing.renderSignalWithTime(ts.getValues(), ts.getTime(), "Original Time Series", chartpattern);

		int originalSignalLength = ts.getValues().length;
		if (forceUniformSampling) {
			System.out.println("TimeSeriesAnalysis->Uniform sampling");
			ts.convertToUniformSignal(0);
			System.out.println("TimeSeriesAnalysis->Uniform sampling finished");

			BufferedWriter bw = new BufferedWriter(new FileWriter(signalFile));
			bw.write("time,value\n");
			int maxLen = ts.getValues().length;
			for (int i = 0; i < maxLen; i++) {
				bw.write(ts.getTime()[i] + "," + ts.getValues()[i] + "\n");
			}

			bw.close();
			if (display) {
				SignalProcessing.displaySignalWithTime(ts.getValues(), ts.getTime(), "Uniformly Sampled Time Series", chartpattern);
				SignalProcessing.displaySignalWithGenericTime(ts.getValues(), 0, 1, "Uniformly Sampled Time Series in Samples");
			}
		}

		PeriodicityDetector pd = new PeriodicityDetector();

		if (doFourierAnalysis) {
			// spectrum and signal processing
			System.out.println("TimeSeriesAnalysis->Detecting periodicities");
			pd = new PeriodicityDetector();
			LinkedHashMap<String, String> frequencies = pd.detectAllFrequencies(ts.getValues(), 1, 0.01f, 0.5f, -1, fftWindowSamplesDouble, sensitivity, display);
			outputParameters.put("Original Time Series Length", "" + originalSignalLength);
			outputParameters.put("Uniformly Samples Time Series Length", "" + ts.getValues().length);
			outputParameters.put("Spectral Analysis Window Length", "" + pd.currentWindowAnalysisSamples);
			outputParameters.put("Spectral Analysis Window Shift", "" + pd.currentWindowShiftSamples);
			outputParameters.put("Spectral Analysis Sampling Rate", "" + MathFunctions.roundDecimal(pd.currentSamplingRate, 2));
			outputParameters.put("Spectrogram Sections", "" + pd.currentspectrum.length);
			outputParameters.put("Range of frequencies (in samples^-1) represented in the Spectrogram:", "[" + MathFunctions.roundDecimal(pd.minFrequency, 2) + " ; " + MathFunctions.roundDecimal(pd.maxFrequency, 2) + "]");
			outputParameters.put("Unit of Measure of Frequency", "samples^-1");
			outputParameters.put("Unit of Measure of Time", "samples");

			for (String freqPar : frequencies.keySet()) {
				outputParameters.put(freqPar, frequencies.get(freqPar));
			}

			/*
			 * outputParameters.put("Detected Frequency (samples^-1)",
			 * ""+MathFunctions.roundDecimal(F,2));
			 * outputParameters.put("Indecision on Frequency",
			 * "["+MathFunctions.roundDecimal(pd.lowermeanF,2)+" , "
			 * +MathFunctions.roundDecimal(pd.uppermeanF,2) + "]");
			 * outputParameters.put("Average detected Period (samples)",
			 * ""+MathFunctions.roundDecimal(pd.meanPeriod,2));
			 * outputParameters.put("Indecision on Average Period",
			 * "["+MathFunctions.roundDecimal(pd.lowermeanPeriod,2)+" , "
			 * +MathFunctions.roundDecimal(pd.uppermeanPeriod,2) + "]");
			 * outputParameters.
			 * put("Samples range in which periodicity was detected",
			 * "from "+pd.startPeriodSampleIndex+"  to "+pd.endPeriodSampleIndex
			 * ); outputParameters.put("Period Strength with interpretation",
			 * ""+MathFunctions.roundDecimal(pd.periodicityStrength,2)+" ("+pd.
			 * getPeriodicityStregthInterpretation()+")");
			 */
			System.out.println("TimeSeriesAnalysis->Periodicity Analysis Done!");
		}
		System.gc();
		SSADataset ssa = null;
		Date[] newtimes = null;
		if (doSSA) {

			System.out.println("TimeSeriesAnalysis->Executing SSA analysis");
			List<Double> values = new ArrayList<Double>();
			for (double v : ts.getValues()) {
				values.add(v);
			}
			newtimes = ts.extendTime(pointsToReconstruct);

			if (windowLength < ts.getValues().length)
				ssa = SSAWorkflow.applyCompleteWorkflow(values, windowLength, eigenvaluespercthr, pointsToReconstruct, false);
			else {
				System.out.println("TimeSeriesAnalysis->SSA analysis impossible to complete");
				System.out.println("SSA Note: The window length is higher than the signal length. Please reduce the value to less than the signal length.");
				throw new Exception("Window length is higher than the signal length");
			}

			System.out.println("TimeSeriesAnalysis->SSA analysis completed");

		}

		System.out.println("TimeSeriesAnalysis->Rendering Images");
		uniformSignalImg = SignalProcessing.renderSignalWithTime(ts.getValues(), ts.getTime(), "Uniformly Sampled Time Series", chartpattern);
		if (uniformSignalImg == null)
			outputParameters.put("Note:", "The charts for uniformly sampled and forecasted signals contain too many points and will not be displayed. The values will be only reported in the output file.");
		else {
			ImageIO.write(ImageTools.toBufferedImage(uniformSignalImg), "PNG", new File(uniformSignalImageFile));
			outputParameters.put("Note:", "Details about the values are reported in the output file.");
		}

		uniformSignalSamplesImg = SignalProcessing.renderSignalWithGenericTime(ts.getValues(), 0, 1, "Uniformly Sampled Time Series in Samples");
		ImageIO.write(ImageTools.toBufferedImage(uniformSignalSamplesImg), "PNG", new File(sampleSignalImageFile));

		if (doFourierAnalysis) {
			spectrogramImg = SignalProcessing.renderSignalSpectrogram2(pd.currentspectrum);
			ImageIO.write(ImageTools.toBufferedImage(spectrogramImg), "PNG", new File(spectrogramImgFile));
		}
		if (doSSA) {
			int timeseriesV = ts.getValues().length;
			double[] forecastedpiece = Arrays.copyOfRange(ssa.getForecastSignal(), timeseriesV, timeseriesV + pointsToReconstruct);
			List<String> tsnames = new ArrayList<String>();
			tsnames.add("Original Time Series");
			tsnames.add("Forecasted Time Series");
			List<double[]> signals = new ArrayList<double[]>();
			signals.add(ts.getValues());
			signals.add(forecastedpiece);
			forecastsignalImg = SignalProcessing.renderSignalsWithTime(signals, newtimes, tsnames, chartpattern);
			ImageIO.write(ImageTools.toBufferedImage(forecastsignalImg), "PNG", new File(forecastsignalImgFile));
			if (display) {
				SignalProcessing.displaySignalsWithTime(signals, newtimes, tsnames, chartpattern);
			}

			double[] eigenValues = new double[ssa.getPercentList().size()];
			for (int i = 0; i < eigenValues.length; i++) {
				eigenValues[i] = ssa.getPercentList().get(i);
			}
			eigenValuesImg = SignalProcessing.renderSignalWithGenericTime(eigenValues, 0f, 1, "SSA Eigenvalues");
			ImageIO.write(ImageTools.toBufferedImage(eigenValuesImg), "PNG", new File(eigenValuesImgFile));
			System.out.println("TimeSeriesAnalysis->Images Rendered");

			System.out.println("TimeSeriesAnalysis->Producing SSA Files");

			BufferedWriter bw = new BufferedWriter(new FileWriter(forecastFile));
			bw.write("time,value\n");
			int maxLen = ssa.getForecastSignal().length;
			for (int i = 0; i < maxLen; i++) {
				bw.write(newtimes[i] + "," + ssa.getForecastSignal()[i] + "\n");
			}

			bw.close();

			bw = new BufferedWriter(new FileWriter(eigenvalueFile));
			bw.write("ssa_eigenvalues\n");
			maxLen = eigenValues.length;
			for (int i = 0; i < maxLen; i++) {
				bw.write(eigenValues[i] + "\n");
			}

			bw.close();

			if (display) {
				SignalProcessing.displaySignalWithTime(ssa.getForecastSignal(), newtimes, "Forecasted Time Series", chartpattern);
				SignalProcessing.displaySignalWithGenericTime(eigenValues, 0f, 1, "SSA Eigenvalues");
			}
		}

		System.out.println("TimeSeriesAnalysis->Files Produced");

		System.out.println("TimeSeriesAnalysis->" + outputParameters);
		System.out.println("TimeSeriesAnalysis->Computation has finished");
	}

}
