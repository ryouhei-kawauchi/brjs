package org.bladerunnerjs.utility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bladerunnerjs.aliasing.AliasDefinition;
import org.bladerunnerjs.aliasing.AliasException;
import org.bladerunnerjs.logging.Logger;
import org.bladerunnerjs.logging.LoggerType;
import org.bladerunnerjs.model.BundlableNode;
import org.bladerunnerjs.model.BundleSet;
import org.bladerunnerjs.model.BundleSetCreator;
import org.bladerunnerjs.model.LinkedAsset;
import org.bladerunnerjs.model.AssetLocation;
import org.bladerunnerjs.model.SourceModule;
import org.bladerunnerjs.model.BundleSetCreator.Messages;
import org.bladerunnerjs.model.exception.CircularDependencyException;
import org.bladerunnerjs.model.exception.ModelOperationException;
import org.bladerunnerjs.model.exception.RequirePathException;
import org.bladerunnerjs.model.exception.request.ContentFileProcessingException;

import com.google.common.base.Joiner;


public class BundleSetBuilder {
	private static final String BOOTSTRAP_LIB_NAME = "br-bootstrap";
	private final Set<SourceModule> sourceModules = new LinkedHashSet<>();
	private final Set<SourceModule> testSourceModules = new LinkedHashSet<>();
	private final Set<AliasDefinition> activeAliases = new LinkedHashSet<>();
	private final Set<LinkedAsset> linkedAssets = new HashSet<LinkedAsset>();
	private final Set<AssetLocation> assetLocations = new LinkedHashSet<>();
	private final BundlableNode bundlableNode;
	private final Logger logger;
	
	public BundleSetBuilder(BundlableNode bundlableNode) {
		this.bundlableNode = bundlableNode;
		logger = bundlableNode.root().logger(LoggerType.BUNDLER, BundleSetCreator.class);
	}
	
	public BundleSet createBundleSet() throws ModelOperationException {
		List<AliasDefinition> activeAliasList = new ArrayList<>();
		List<AssetLocation> resourceLocationList = new ArrayList<>();
		
		try {
			activeAliasList.addAll(activeAliases);
			resourceLocationList.addAll(assetLocations);
			
			for(AliasDefinition aliasDefinition : new ArrayList<>(activeAliases)) {
				addSourceModule(bundlableNode.getSourceModule(aliasDefinition.getRequirePath()));
			}
		}
		catch(RequirePathException e) {
			throw new ModelOperationException(e);
		}
		
		return new BundleSet(bundlableNode, orderSourceModules(sourceModules), new ArrayList<SourceModule>(testSourceModules), activeAliasList, resourceLocationList);
	}
	
	public void addSeedFiles(List<LinkedAsset> seedFiles) throws ModelOperationException {
		for(LinkedAsset seedFile : seedFiles) {
			addLinkedAsset(seedFile);
		}
	}
	
	public void addTestSourceModules(List<? extends SourceModule> testSourceModules) throws ModelOperationException {
		for(SourceModule sourceModule : testSourceModules) {
			addTestSourceModule(sourceModule);
		}
	}
	
	private void addTestSourceModule(SourceModule sourceModule) throws ModelOperationException {
		if(testSourceModules.add(sourceModule)) {
			activeAliases.addAll(getAliases(sourceModule.getAliasNames()));
			addLinkedAsset(sourceModule);
		}
	}
	
	private void addSourceModule(SourceModule sourceModule) throws ModelOperationException {
		if(sourceModules.add(sourceModule)) {
			activeAliases.addAll(getAliases(sourceModule.getAliasNames()));
			addLinkedAsset(sourceModule);
		}
	}
	
	private void addLinkedAsset(LinkedAsset linkedAsset) throws ModelOperationException {
		
		if(linkedAssets.add(linkedAsset)) {
			List<SourceModule> moduleDependencies = getDependentSourceModules(linkedAsset, bundlableNode);
			
			activeAliases.addAll(getAliases(linkedAsset.getAliasNames()));
			
			if(moduleDependencies.isEmpty()) {
				logger.debug(Messages.FILE_HAS_NO_DEPENDENCIES_MSG, linkedAsset.getAssetPath());
			}
			else {
				logger.debug(Messages.FILE_DEPENDENCIES_MSG, linkedAsset.getAssetPath(), sourceFilePaths(moduleDependencies));
			}
			
			for(SourceModule sourceModule : moduleDependencies) {
				addSourceModule(sourceModule);
			}
			
			addAssetLocation(linkedAsset.getAssetLocation());
		}
		
	}
	
	private void addAssetLocation(AssetLocation assetLocation) throws ModelOperationException {
		
		if(assetLocations.add(assetLocation)) {
			for(LinkedAsset resourceSeedFile : assetLocation.seedResources()) {
				addLinkedAsset(resourceSeedFile);
			}
			
			for(AssetLocation dependentAssetLocation : assetLocation.getDependentAssetLocations()) {
				addAssetLocation(dependentAssetLocation);
			}
		}
	}

	private List<AliasDefinition> getAliases(List<String> aliasNames) throws ModelOperationException {
		List<AliasDefinition> aliases = new ArrayList<>();
		
		try {
			for(String aliasName : aliasNames) {
				AliasDefinition alias = bundlableNode.getAlias(aliasName);
				if (alias != null)
				{
					aliases.add(alias);
				}
			}
		}
		catch(AliasException | ContentFileProcessingException e) {
			throw new ModelOperationException(e);
		}
		
		return aliases;
	}

	private List<SourceModule> orderSourceModules(Set<SourceModule> sourceModules) throws ModelOperationException {
		List<SourceModule> orderedSourceModules = new ArrayList<>();
		Set<LinkedAsset> metDependencies = new HashSet<>();		
		
		try {
			SourceModule bootstrapSourceModule = bundlableNode.getSourceModule(BOOTSTRAP_LIB_NAME);
			if (!sourceModules.isEmpty())
			{
				orderedSourceModules.add(bootstrapSourceModule);
				metDependencies.add(bootstrapSourceModule);
			}
		}
		catch(RequirePathException e) {
			// do nothing: 'bootstrap' is only an implicit dependency if it exists 
		}
		
		while (!sourceModules.isEmpty()) {
			Set<SourceModule> unprocessedSourceModules = new LinkedHashSet<>();
			boolean progressMade = false;
			
			for(SourceModule sourceModule : sourceModules) {
				if (dependenciesHaveBeenMet(sourceModule, metDependencies)) {
					progressMade = true;
					if (!orderedSourceModules.contains(sourceModule))
					{
						orderedSourceModules.add(sourceModule);
					}
					metDependencies.add(sourceModule);
				}
				else {
					unprocessedSourceModules.add(sourceModule);
				}
			}
			
			if (!progressMade)
			{
				throw new CircularDependencyException(unprocessedSourceModules);
			}
			
			sourceModules = unprocessedSourceModules;
		}
		
		return orderedSourceModules;
	}
	
	private boolean dependenciesHaveBeenMet(SourceModule sourceModule, Set<LinkedAsset> metDependencies) throws ModelOperationException {
		for(LinkedAsset dependentSourceModule : getOrderDependentSourceModules(sourceModule, bundlableNode)) {
			if(!metDependencies.contains(dependentSourceModule)) {
				return false;
			}
		}
		
		return true;
	}
	
	private String sourceFilePaths(List<SourceModule> sourceModules) {
		List<String> sourceFilePaths = new ArrayList<>();
		
		for(SourceModule sourceModule : sourceModules) {
			sourceFilePaths.add(sourceModule.getAssetPath());
		}
		
		return "'" + Joiner.on("', '").join(sourceFilePaths) + "'";
	}
	
	
	private List<SourceModule> getOrderDependentSourceModules(SourceModule sourceModule, BundlableNode bundlableNode) throws ModelOperationException
	{
		List<SourceModule> orderDependentSourceModules = sourceModule.getOrderDependentSourceModules(bundlableNode);
//		addBootstrapToDependencies(orderDependentSourceModules);		
		return orderDependentSourceModules;
	}
	
	private List<SourceModule> getDependentSourceModules(LinkedAsset linkedAsset, BundlableNode bundlableNode) throws ModelOperationException
	{
		List<SourceModule> dependentSourceModules = linkedAsset.getDependentSourceModules(bundlableNode);
		addBootstrapToDependencies(dependentSourceModules);
		return dependentSourceModules;
	}
	
	private void addBootstrapToDependencies(List<SourceModule> dependencies)
	{
		try {
			if (!dependencies.isEmpty())
			{
				dependencies.add( bundlableNode.getSourceModule(BOOTSTRAP_LIB_NAME) );
			}
		}
		catch(RequirePathException e) {
			// do nothing: 'bootstrap' is only an implicit dependency if it exists 
		}
		
	}
	
	
	
	
}
