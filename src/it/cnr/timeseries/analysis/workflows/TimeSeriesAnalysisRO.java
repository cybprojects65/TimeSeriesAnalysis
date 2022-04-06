package it.cnr.timeseries.analysis.workflows;

import java.io.File;

public class TimeSeriesAnalysisRO {
	public int SSAAnalysisWindowSamples;
	public File inputFile;
	public String valuecolumn;
	public int timeseriesLength;
	public float eigenvalues [];
			
	
	public int getNumberOfEigenVectors() {
		int counter = 0;
		//NOTE: the eigen corresponding to the threshold is excluded!
		for (double eigenvalue:eigenvalues) {
			if ((int)(eigenvalue*100000)<=(int)(SSAEigenvaluesThreshold*100000))
				break;
			
			counter++;
		}
		return counter;
	}
	public float[] getEigenvalues() {
		return eigenvalues;
	}
	public void setEigenvalues(float[] eigenvalues) {
		this.eigenvalues = eigenvalues;
	}
	public String getValuecolumn() {
		return valuecolumn;
	}
	public void setValuecolumn(String valuecolumn) {
		this.valuecolumn = valuecolumn;
	}
	public String getTimecolumn() {
		return timecolumn;
	}
	public void setTimecolumn(String timecolumn) {
		this.timecolumn = timecolumn;
	}
	public String timecolumn;
	
	public File getInputFile() {
		return inputFile;
	}
	public void setInputFile(File inputFile) {
		this.inputFile = inputFile;
	}
	
	public TimeSeriesAnalysisRO clone() {
		return new TimeSeriesAnalysisRO( SSAAnalysisWindowSamples,  SSAPointsToForecast,
				 SSAEigenvaluesThreshold,  predictedValue,  lastknownvalue,  absoluteError,
				 relativeError,  inputFile,  valuecolumn,  timecolumn,  timeseriesLength,eigenvalues);
	}
	public TimeSeriesAnalysisRO(int sSAAnalysisWindowSamples, int sSAPointsToForecast,
			float sSAEigenvaluesThreshold, double predictedValue, double lastknownvalue, double absoluteError,
			double relativeError, File inputFile, String valuecolumn, String timecolumn, int timeseriesLength, float eigenvalues []) {
		super();
		SSAAnalysisWindowSamples = sSAAnalysisWindowSamples;
		SSAPointsToForecast = sSAPointsToForecast;
		SSAEigenvaluesThreshold = sSAEigenvaluesThreshold;
		this.predictedValue = predictedValue;
		this.lastknownvalue = lastknownvalue;
		this.absoluteError = absoluteError;
		this.relativeError = relativeError;
		this.inputFile = inputFile;
		this.valuecolumn = valuecolumn;
		this.timecolumn = timecolumn;
		this.timeseriesLength = timeseriesLength;
		this.eigenvalues = eigenvalues;
	}
	public int getSSAAnalysisWindowSamples() {
		return SSAAnalysisWindowSamples;
	}
	public void setSSAAnalysisWindowSamples(int sSAAnalysisWindowSamples) {
		SSAAnalysisWindowSamples = sSAAnalysisWindowSamples;
	}
	public int getSSAPointsToForecast() {
		return SSAPointsToForecast;
	}
	public void setSSAPointsToForecast(int sSAPointsToForecast) {
		SSAPointsToForecast = sSAPointsToForecast;
	}
	public float getSSAEigenvaluesThreshold() {
		return SSAEigenvaluesThreshold;
	}
	public void setSSAEigenvaluesThreshold(float sSAEigenvaluesThreshold) {
		SSAEigenvaluesThreshold = sSAEigenvaluesThreshold;
	}
	public double getPredictedValue() {
		return predictedValue;
	}
	public void setPredictedValue(double predictedValue) {
		this.predictedValue = predictedValue;
	}
	public double getLastknownvalue() {
		return lastknownvalue;
	}
	public void setLastknownvalue(double lastknownvalue) {
		this.lastknownvalue = lastknownvalue;
	}
	public double getAbsoluteError() {
		return absoluteError;
	}
	public void setAbsoluteError(double absoluteError) {
		this.absoluteError = absoluteError;
	}
	public double getRelativeError() {
		return relativeError;
	}
	public void setRelativeError(double relativeError) {
		this.relativeError = relativeError;
	}
	public int SSAPointsToForecast;
	public float SSAEigenvaluesThreshold;
	public double predictedValue;
	public double lastknownvalue;
	public double absoluteError;
	public double relativeError;
	
	
	
}
