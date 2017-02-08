package com.ibm.g11n.pipeline.ant;

/**
 * This class is used within BundleSet as nested element for ant. 
 * This specifies the modifications to be made to the folder/file names 
 * when downloading translation files from the globalization pipeline instance
 * @author jugudanniesundar
 *
 */
public class LanguageMap {
	
	/**
	 * The language name which needs to be modified when downloading files in that language
	 */
	private String from;
	
	/**
	 * The language name which should be used when downloading files in "from" languange
	 */
	private String to;
	
	/**
	 * 
	 * @return from
	 */
	public String getFrom() {
		return from;
	}
	
	/**
	 * 
	 * @param from
	 */
	public void setFrom(String from) {
		this.from = from;
	}
	
	/**
	 * 
	 * @return to
	 */
	public String getTo() {
		return to;
	}
	
	/**
	 * 
	 * @param to
	 */
	public void setTo(String to) {
		this.to = to;
	}
	

}
