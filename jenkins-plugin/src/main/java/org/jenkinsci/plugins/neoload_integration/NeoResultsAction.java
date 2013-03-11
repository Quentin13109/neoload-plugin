package org.jenkinsci.plugins.neoload_integration;

import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.model.Run.Artifact;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;

import org.codehaus.plexus.util.FileUtils;

public final class NeoResultsAction implements Action {
	
	/** This tag is found in certain pages generated by NeoLoad. */
	private static final String TAG_HTML_GENERATED_BY_NEOLOAD = "<!--#Generated by NeoLoad-->";
	
	/** This is added to a file to mark whether the styles have been applied or not. */
	private static final String COMMENT_APPLIED_STYLE = "<!-- NeoLoad Jenkins plugin applied style -->";
	
	/** This is added to a file to mark whether the styles have been applied or not. */
	private static final String COMMENT_CSS_APPLIED_STYLE = "/* NeoLoad Jenkins plugin applied style */";
	
	/** The current build. */
    private final AbstractBuild<?, ?> build;

    public NeoResultsAction(
            final AbstractBuild<?, ?> target) {
        super();
        this.build = target;
    }
    
    private class FileAndContent {
    	public File file = null;
    	public String href = null;
    	public String content = null;
    	public FileAndContent(File file, String href, String content) {
			this.file = file;
			this.href = href;
			this.content = content;
		}
    }

    /**
     * @return
     * @throws IOException
     */
    @SuppressWarnings("rawtypes")
	private FileAndContent findHtmlReportArtifact() throws IOException {
		Artifact artifact = null;
		Iterator<?> it = build.getArtifacts().iterator();
		String content = null;
		FileAndContent ac = null;
		
		// remove files that don't match
		while (it.hasNext()) {
			artifact = (Artifact) it.next();
			
			// if it's an html file
			if ((artifact.getFileName().length() > 4) && 
					("html".equalsIgnoreCase(artifact.getFileName().substring(artifact.getFileName().length() - 4)))) {
				
				// verify file contents
				content = FileUtils.fileRead(artifact.getFile().getAbsolutePath());
				if ((content != null) && (content.contains(TAG_HTML_GENERATED_BY_NEOLOAD))) {
					ac = new FileAndContent(artifact.getFile(), artifact.getHref(), content);
					break;
				}
			}
		}
		
		return ac;
	}

	/**
     * @return
     */
    public AbstractBuild<?, ?> getBuild() {
		return build;
	}

    /**
     * @return
     * @throws IOException
     */
    public String getHtmlReportFilePath() throws IOException {
    	FileAndContent ac = findHtmlReportArtifact();
    	
    	if (ac != null) {
    		// append the style changes if it hasn't already been done
    		if (!ac.content.contains(COMMENT_APPLIED_STYLE)) {
    			applySpecialFormatting(ac);
    		}
    		
    		return ac.href;
    	}
    	
    	return null;
    }

	/**
	 * @param ac
	 * @throws IOException
	 */
	protected static void applySpecialFormatting(FileAndContent ac) {
		try {
			// adjust the content
			ac.content = ac.content.replaceAll(Matcher.quoteReplacement("id=\"menu\""), "id=\"menu\" style='overflow-x: hidden;' ");
			ac.content = ac.content.replaceAll(Matcher.quoteReplacement("id=\"content\""), "id=\"content\" style='overflow-x: hidden;' ");
			ac.content += COMMENT_APPLIED_STYLE;
	
			// write the content
			long modDate = ac.file.lastModified();
			if (ac.file.canWrite()) {
				ac.file.delete();
				FileUtils.fileWrite(ac.file, ac.content);
				ac.file.setLastModified(modDate); // keep the old modification date
			}
			
			// find the menu.html
			String temp = ac.content.substring(ac.content.indexOf("src=\"") + 5);
			temp = temp.substring(0, temp.indexOf("\""));
			String menuLink = ac.file.getParent() + File.separatorChar + temp;
			String menuContent = FileUtils.fileRead(menuLink);
			menuContent = menuContent.replace(Matcher.quoteReplacement("body {"), "body {\noverflow-x: hidden;");
			menuContent += COMMENT_APPLIED_STYLE;
			new File(menuLink).delete();
			FileUtils.fileWrite(menuLink, menuContent);
			
			// find the style.css
			temp = ac.content.substring(ac.content.indexOf("<link"), ac.content.indexOf(">", ac.content.indexOf("<link")));
			temp = temp.substring(temp.indexOf("href=") + 6, temp.length() - 1);
			String styleLink = ac.file.getParent() + File.separatorChar + temp;
			String styleContent = FileUtils.fileRead(styleLink);
			styleContent = styleContent.replace(Matcher.quoteReplacement("body {"), "body {\noverflow-x: hidden;");
			styleContent += COMMENT_CSS_APPLIED_STYLE;
			new File(styleLink).delete();
			FileUtils.fileWrite(styleLink, styleContent);
			
		} catch (Exception e) {
			// this operation is not important enough to throw an exception.
			System.out.println("Couldn't add custom style to report files.");
		}
	}
    
    @Override
	public String getDisplayName() {
        return "Performance Result";
    }

    @Override
	public String getIconFileName() {
        return "clipboard.png";
    }

    @Override
	public String getUrlName() {
        return "neoload-report";
    }
}