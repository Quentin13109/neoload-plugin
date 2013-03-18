package org.jenkinsci.plugins.neoload_integration;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import org.jenkinsci.plugins.neoload_integration.supporting.NeoLoadPluginOptions;
import org.jenkinsci.plugins.neoload_integration.supporting.PluginUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * This class adds the link to the html report to a build after the build has
 * completed.
 */
@SuppressWarnings("unchecked")
public class NeoPostBuildAction extends Notifier implements NeoLoadPluginOptions {
	
	/** User option presented in the GUI. Show the average response time. */
	private final boolean showTrendAverageResponse;

	/** User option presented in the GUI. Show the average response time. */
	private final boolean showTrendErrorRate;
	
	@DataBoundConstructor
	public NeoPostBuildAction(boolean showTrendAverageResponse, boolean showTrendErrorRate) {
		// this method and the annotation @DataBoundConstructor are required for jenkins 1.393 even if no params are passed in.
		this.showTrendAverageResponse = showTrendAverageResponse;
		this.showTrendErrorRate = showTrendErrorRate;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
		PluginUtils.addActionIfNotExists(build);

		// look at all builds to see if we can add a report link
		for (Object o : build.getProject().getBuilds()) {
			if (o instanceof AbstractBuild) {
				PluginUtils.addActionIfNotExists((AbstractBuild<?, ?>) o);
			}
		}

		return true;
	}

	@Extension(optional = true)
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		public DescriptorImpl() {
			super(NeoPostBuildAction.class);
		}

		@Override
		public String getDisplayName() {
			return "Incorporate NeoLoad Results";
		}

		@Override
		public boolean isApplicable(
				@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
			return true;
		}
	}
	

	/** @return the showTrendAverageResponse */
	@Override
	public boolean isShowTrendAverageResponse() {
		return showTrendAverageResponse;
	}

	/** @return the showTrendErrorRate */
	@Override
	public boolean isShowTrendErrorRate() {
		return showTrendErrorRate;
	}	
}
