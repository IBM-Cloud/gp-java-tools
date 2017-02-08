package com.ibm.g11n.pipeline.ant;

/**
 * This class is used within BundleSet as nested element.
 * This class is also used in GPUpload/GPDownload as nested element
 * 
 * When used within BundleSet, then the languages specified using TargetLanguages
 * are used to upload/download files specifically in that language in that bundle
 * 
 * If the targetLanguages are not defined within the BundleSet, then the targetLanguages
 * defined in GPUpload/GPDownload is used for uploading/downloading files in those language(s)
 * @author jugudanniesundar
 *
 */
public class TargetLanguage {
	
	/**
	 * a target language
	 */
	String lang;
	
	public TargetLanguage() {
    }
    
	/**
	 * 
	 * @param targetLang
	 */
    public void setLang(String targetLang) { 
    	this.lang = targetLang; 
    }
    
    /**
     * 
     * @return targetLang
     */
    public String getLang() { 
    	return lang; 
    }
}
