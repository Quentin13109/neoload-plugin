package org.jenkinsci.plugins.neoload_integration;

import hudson.model.ProminentProjectAction;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run.Artifact;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;
import hudson.util.DataSetBuilder;
import hudson.util.Graph;

import java.awt.Color;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.jenkinsci.plugins.neoload_integration.supporting.NeoLoadGraph;
import org.jenkinsci.plugins.neoload_integration.supporting.PluginUtils;
import org.xml.sax.SAXException;

import com.neotys.nl.controller.report.transform.NeoLoadReportDoc;

public class ProjectSpecificAction implements ProminentProjectAction {

	/** A link to the Jenkins job. */
    private AbstractProject<?, ?> project;
    
    /** Key is the build. Value is the NeoLoad xml report file. */
	private Map<AbstractBuild<?,?>, NeoLoadReportDoc> buildsAndDocs = new LinkedHashMap<>();
	
	/** Log various messages. */
	private static Logger logger = Logger.getLogger(ProjectSpecificAction.class.getName());
    
    public ProjectSpecificAction(AbstractProject<?, ?> project) {
        this.project = project;
    }
    
    @Override
    public String getUrlName() {
        return "neoload";
    }
    
    public boolean showAvgGraph() {
    	if (!PluginUtils.getPluginOptions(project).isShowTrendAverageResponse()) {
    		return false;
    	}
    	
    	return graphDataExists();
    }
    
    public boolean showErrGraph() {
    	if (!PluginUtils.getPluginOptions(project).isShowTrendErrorRate()) {
    		return false;
    	}
    	
    	return graphDataExists();
    }
    

    /** 
     * @return true if enough data exists to create a graph
     * @throws XPathExpressionException 
     * @throws IOException 
     * @throws SAXException 
     * @throws ParserConfigurationException 
     */
    public boolean graphDataExists() {
    	try {
			findNeoLoadXMLResults(project);
		} catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
			logger.log(Level.SEVERE, "Error finding NeoLoad xml results. " + e.getMessage());
			e.printStackTrace();
		}
    	
    	// there must be at least two results to create the graph
    	return buildsAndDocs.size() > 1;
    }
    
    /**
     * @return
     * @throws XPathExpressionException 
     */
    public Graph getErrGraph() throws XPathExpressionException {
        DataSetBuilder<String, NumberOnlyBuildLabel> dsb = new DataSetBuilder<>();
        Float errorRate;
        
        for (AbstractBuild<?, ?> build : buildsAndDocs.keySet()) {
        	NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(build);
        	errorRate = buildsAndDocs.get(build).getErrorRatePercentage();
			logger.log(Level.FINE, "Error rate found for build " + build.number + ": " + errorRate);
        	
        	if (errorRate != null) {
        		dsb.add(errorRate, "Time", label);
        	}
        }

        // color from ColorTable.java
		return new NeoLoadGraph(dsb.build(), "Error Rate %", new Color(200, 0, 0));
    }
    
    /**
     * Generates a graph 
     * @throws XPathExpressionException 
     * 
     */
    public Graph getAvgGraph() throws XPathExpressionException {
        DataSetBuilder<String, NumberOnlyBuildLabel> dsb = new DataSetBuilder<>();
        Float avgResponseTime;
        
        for (AbstractBuild<?, ?> build : buildsAndDocs.keySet()) {
        	NumberOnlyBuildLabel label = new NumberOnlyBuildLabel(build);
        	avgResponseTime = buildsAndDocs.get(build).getAverageResponseTime();
			logger.log(Level.FINE, "Average response time found for build " + build.number + ": " + avgResponseTime);
        	
        	if (avgResponseTime != null) {
        		dsb.add(avgResponseTime, "Time", label);
        	}
        }

        // color from ColorTable.java
		return new NeoLoadGraph(dsb.build(), "Avg Resp Time (secs)", new Color(237, 184, 0));
    }
    
	/**
	 * @param aProject
	 * @throws XPathExpressionException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	private void findNeoLoadXMLResults(final AbstractProject<?, ?> aProject) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
		NeoLoadReportDoc doc = null;
		Map<AbstractBuild<?,?>, NeoLoadReportDoc> newBuildsAndDocs = new LinkedHashMap<>();
		
		System.out.println("--------");
		for (AbstractBuild build : project.getBuilds()) {
			doc = findXMLResultsFile(build);
			
			// if the correct file was found
			if (doc != null) {
				// only include successful builds
				if (build.getResult().isBetterThan(Result.FAILURE)) {
					System.out.println("");
					newBuildsAndDocs.put(build, doc);
				}
			}
		}
		
		// switch out the data for the new view
		Map<AbstractBuild<?,?>, NeoLoadReportDoc> oldBuildsAndDocs = buildsAndDocs;
		buildsAndDocs = newBuildsAndDocs;
		oldBuildsAndDocs.clear();
	}

	/**
	 * @param build
	 * @return
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws XPathExpressionException 
	 */
	private static NeoLoadReportDoc findXMLResultsFile(final AbstractBuild build) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		Artifact artifact = null;
		Iterator<Artifact> it = build.getArtifacts().iterator();
		NeoLoadReportDoc nlrd = null;
		NeoLoadReportDoc correctDoc = null;
		
		// remove files that don't match
		while (it.hasNext()) {
			artifact = it.next();
			
			nlrd = new NeoLoadReportDoc(artifact.getFile().getAbsolutePath());

			// if the file is valid and was created during this build
			if (!nlrd.isValidReportDoc()) {
				it.remove();
			} else {
				correctDoc = nlrd;
				break;
			}
		}
		
		return correctDoc;
	}
        
    @Override
	public String getIconFileName() {
        return null;
    }

    @Override
	public String getDisplayName() {
        return "!" + this.getClass().getSimpleName() + "!";
    }
}