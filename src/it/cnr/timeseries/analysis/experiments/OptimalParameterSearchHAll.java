package it.cnr.timeseries.analysis.experiments;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.List;

import it.cnr.timeseries.analysis.datastructures.AggregationFunctions;
import it.cnr.timeseries.analysis.datastructures.Utils;
import it.cnr.timeseries.analysis.signals.MathFunctions;
import it.cnr.timeseries.analysis.workflows.TimeSeriesAnalysis;

public class OptimalParameterSearchHAll {

	public static void main(String args[]) throws Exception {
		int year = 2019;

		String header = "station" + "," + "truevalue" + "," + "bestpredictedvalue" + "," + "uncertainty" + ","
				+ "bestwindow" + "," + "bestrelativeerrorW" + "," + "firsteigenpredictedvalue" + ","
				+ "bestrelativeerror" + "," + "bestnumeigen" + "," + "besteigenthreshold";

		FileWriter fw = new FileWriter(new File("predictionsummary.csv"));
		fw.append(header + "\n");

		// CTC = Sepia officinalis
		// File originalTimeSeriesTable = new
		// File("C:\\Users\\Utente\\Ricerca\\Experiments\\EcologicalModelling
		// Solemon\\Data_EcologicalModelling_Solemon\\solemon\\ctc_index.csv");
		// File timeSeriesTables [] = new
		// File("C:\\Users\\Utente\\Ricerca\\Experiments\\EcologicalModelling
		// Solemon\\Validazione\\SSA\\Serie 2016-2018\\").listFiles();

		// SOL = Solea solea
		File originalTimeSeriesTable = new File(
				"C:\\Users\\Utente\\Ricerca\\Experiments\\EcologicalModelling Solemon\\Data_EcologicalModelling_Solemon\\solemon\\sol_index.csv");
		File timeSeriesTables[] = new File(
				"C:\\Users\\Utente\\Ricerca\\Experiments\\EcologicalModelling Solemon\\Validazione\\Solea solea\\2006-2018")
						.listFiles();

		for (int k = 0; k < timeSeriesTables.length; k++) { // cycle through all time series

			File timeSeriesTable = timeSeriesTables[k];

			String station = timeSeriesTable.getName().replace(".csv", "").replace("s", "");
			
			if (!station.equals("48")) continue;
			
			List<String> allLines = Files.readAllLines(originalTimeSeriesTable.toPath());
			double trueValue = 0;
			for (String s : allLines) {

				String elements[] = s.split(",");
				String stat = elements[0].replace("\"", "");
				String yeare = elements[3].replace("\"", "");
				String value = elements[5].replace("\"", "");

				if (yeare.equals("" + year) && stat.equals(station)) {

					trueValue = Double.parseDouble(value);
					System.out.println("Found true value " + trueValue);
					break;
				}
			}

			// double trueValue = 7.38500785827637;

			String valuescolum = "biomindex";
			String timecolumn = "year";
			AggregationFunctions aggregationFunction = AggregationFunctions.SUM;
			TimeSeriesAnalysis.Sensitivity sensitivityP = TimeSeriesAnalysis.Sensitivity.LOW;
			int fftwindowsamples = 3;

			boolean forceUniformSampling = true;
			boolean doFourierAnalysis = false;
			boolean doSSA = true;

			float SSAEigenvaluesThreshold = 0.1f;

			int bestwindow = 0;
			double bestrelativeerror = 200;
			double bestpredictedvalue = 0;

			int SSAPointsToForecast = 1;

			for (int i = 1; i < 100; i++)
			// int i = 8;
			{

				// int SSAAnalysisWindowSamples = 10;//12;
				int SSAAnalysisWindowSamples = i;
				System.out.println("####Testing " + i + " Window samples");
				TimeSeriesAnalysis tsa = new TimeSeriesAnalysis();
				try {

					tsa.process(timeSeriesTable, valuescolum, timecolumn, aggregationFunction, sensitivityP,
							fftwindowsamples, SSAAnalysisWindowSamples, SSAEigenvaluesThreshold, SSAPointsToForecast,
							forceUniformSampling, doFourierAnalysis, doSSA);
				} catch (Exception e) {
					break;
				}
				// double trueValue = 86.63373566;
				// double trueValue = 0;

				List<String> allRows = Files.readAllLines(new File(tsa.forecastFile).toPath());
				String predicted = allRows.get(allRows.size() - 1);
				predicted = predicted.substring(predicted.indexOf(",") + 1);
				double predictedValue = Double.parseDouble(predicted);
				double absoluteError = (trueValue - predictedValue);
				double relativeError = MathFunctions.roundDecimal(Math.abs(absoluteError / trueValue) * 100f, 2);

				System.out.println("SSA samples: " + i);
				System.out.println("Predicted " + predictedValue + " vs " + trueValue);
				System.out.println("Absolute Error " + absoluteError + "; Relative Error " + relativeError + "%");

				if (!Double.isNaN(predictedValue) && relativeError < bestrelativeerror) {
					bestrelativeerror = relativeError;
					bestwindow = SSAAnalysisWindowSamples;

				}

			}
			double bestrelativeerrorW = bestrelativeerror;

			
			System.out.println("####Optimal window: " + bestwindow+ " error: "+bestrelativeerrorW+"%" );
			
			System.out.println("Searching for best eigen threshold");
			
			TimeSeriesAnalysis tsa = new TimeSeriesAnalysis();
			tsa.process(timeSeriesTable, valuescolum, timecolumn, aggregationFunction, sensitivityP, fftwindowsamples,
					bestwindow, SSAEigenvaluesThreshold, SSAPointsToForecast, forceUniformSampling, doFourierAnalysis,
					doSSA);

			List<String> allEigen = Files.readAllLines(new File(tsa.eigenvalueFile).toPath());
			int besteigen = 0;
			double besteigenthreshold = 0;
			bestrelativeerror = 200;
			double firsteigenpredictedvalue = 0;

			for (int i = 1; i < allEigen.size(); i++)
			// int i = 8;
			{

				// int SSAAnalysisWindowSamples = 10;//12;
				int SSAAnalysisWindowSamples = bestwindow;
				SSAEigenvaluesThreshold = Float.parseFloat(allEigen.get(i));
				System.out.println("####Testing " + i + " Eigenvalues with threshold " + SSAEigenvaluesThreshold);
				TimeSeriesAnalysis tsae = new TimeSeriesAnalysis();
				try {

					tsae.process(timeSeriesTable, valuescolum, timecolumn, aggregationFunction, sensitivityP,
							fftwindowsamples, SSAAnalysisWindowSamples, SSAEigenvaluesThreshold, SSAPointsToForecast,
							forceUniformSampling, doFourierAnalysis, doSSA);
				} catch (Exception e) {
					System.out.println("Error with this eigen threshold: " +e.getLocalizedMessage());
					e.printStackTrace();
					continue;
				}

				System.out.println("Getting forecast...");
				
				List<String> allRows = Files.readAllLines(new File(tsa.forecastFile).toPath());
				String predicted = allRows.get(allRows.size() - 1);
				predicted = predicted.substring(predicted.indexOf(",") + 1);
				double predictedValue = Double.parseDouble(predicted);
				double absoluteError = (trueValue - predictedValue);
				double relativeError = MathFunctions.roundDecimal(Math.abs(absoluteError / trueValue) * 100f, 2);

				System.out.println("SSA eigen N.: " + (i - 1));
				System.out.println("SSA eigen T.: " + SSAEigenvaluesThreshold);
				System.out.println("Predicted " + predictedValue + " vs " + trueValue);
				System.out.println("Absolute Error " + absoluteError + "; Relative Error " + relativeError + "%");

				if (i > 1 && !Double.isNaN(predictedValue) && relativeError < bestrelativeerror) {
					besteigen = i;
					besteigenthreshold = SSAEigenvaluesThreshold;
					bestrelativeerror = relativeError;
					bestpredictedvalue = predictedValue;
				} else if (i == 1) {

					firsteigenpredictedvalue = predictedValue;

				}

			}

			System.out.println("Running the forecast with the optimal selected parameters");
			
			TimeSeriesAnalysis tsae = new TimeSeriesAnalysis();
			try {
				tsae.process(timeSeriesTable, valuescolum, timecolumn, aggregationFunction, sensitivityP,
						fftwindowsamples, bestwindow, (float) besteigenthreshold, SSAPointsToForecast,
						forceUniformSampling, doFourierAnalysis, doSSA);

				// save charts in a separate folder
				File stationFolder = new File("./s" + station);
				if (stationFolder.exists())
					Utils.deleteDirectoryWalkTree(stationFolder.toPath());
				stationFolder.mkdir();

				Files.copy(new File(TimeSeriesAnalysis.uniformSignalImageFile).toPath(),
						new File(stationFolder, TimeSeriesAnalysis.uniformSignalImageFile).toPath());
				Files.copy(new File(TimeSeriesAnalysis.sampleSignalImageFile).toPath(),
						new File(stationFolder, TimeSeriesAnalysis.sampleSignalImageFile).toPath());
				Files.copy(new File(TimeSeriesAnalysis.forecastsignalImgFile).toPath(),
						new File(stationFolder, TimeSeriesAnalysis.forecastsignalImgFile).toPath());
				Files.copy(new File(TimeSeriesAnalysis.eigenValuesImgFile).toPath(),
						new File(stationFolder, TimeSeriesAnalysis.eigenValuesImgFile).toPath());
				Files.copy(new File(TimeSeriesAnalysis.signalFile).toPath(),
						new File(stationFolder, TimeSeriesAnalysis.signalFile).toPath());
				Files.copy(new File(TimeSeriesAnalysis.forecastFile).toPath(),
						new File(stationFolder, TimeSeriesAnalysis.forecastFile).toPath());
				Files.copy(new File(TimeSeriesAnalysis.eigenvalueFile).toPath(),
						new File(stationFolder, TimeSeriesAnalysis.eigenvalueFile).toPath());

			} catch (Exception e) {
				e.printStackTrace();
			}

			double uncertainty = Math.abs(bestpredictedvalue - firsteigenpredictedvalue);

			System.out.println("");
			System.out.println("#######################");
			System.out.println("BEST SSA window: " + bestwindow + " Error " + bestrelativeerrorW + "%");
			System.out.println("#######################");
			System.out.println("");
			System.out.println("#############");
			System.out.println("BEST SSA eigen N.: " + (besteigen - 1));
			System.out.println("BEST SSA eigen T.: " + besteigenthreshold);
			System.out.println("BEST SSA eigen Error.: " + bestrelativeerror + "%");
			System.out.println("BEST Prediction.: " + bestpredictedvalue);
			System.out.println("1st eigen Prediction.: " + firsteigenpredictedvalue);

			System.out.println("Uncertainty.: " + uncertainty);

			String output = station + "," + trueValue + "," + bestpredictedvalue + "," + uncertainty + "," + bestwindow
					+ "," + bestrelativeerrorW + "," + firsteigenpredictedvalue + "," + bestrelativeerror + ","
					+ (besteigen - 1) + "," + besteigenthreshold;
			fw.append(output + "\n");

		}

		fw.close();
	}

}
