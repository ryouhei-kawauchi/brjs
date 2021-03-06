package org.bladerunnerjs.plugin.seedlocator;

import java.util.List;

import org.bladerunnerjs.api.Asset;
import org.bladerunnerjs.api.BRJS;
import org.bladerunnerjs.api.LinkedAsset;
import org.bladerunnerjs.api.memoization.MemoizedFile;
import org.bladerunnerjs.api.plugin.AssetRegistry;
import org.bladerunnerjs.api.plugin.base.AbstractAssetPlugin;
import org.bladerunnerjs.model.AssetContainer;
import org.bladerunnerjs.api.BrowsableNode;
import org.bladerunnerjs.model.IndexPageAsset;


public class BrowsableNodeSeedLocator extends AbstractAssetPlugin
{
	
	@Override
	public void setBRJS(BRJS brjs)
	{	
	}
	
	@Override
	public void discoverAssets(AssetContainer assetContainer, MemoizedFile dir, String requirePrefix, List<Asset> implicitDependencies, AssetRegistry assetDiscoveryInitiator)
	{
		if (assetContainer instanceof BrowsableNode && assetContainer.dir() == dir) {
			MemoizedFile indexFile = null;
			
			if(assetContainer.file("index.html").isFile()) {
				indexFile = assetContainer.file("index.html");
			}
			else if(assetContainer.file("index.jsp").exists()) {
				indexFile = assetContainer.file("index.jsp");
			}
			
			if ( indexFile != null && !assetDiscoveryInitiator.hasRegisteredAsset(IndexPageAsset.calculateRequirePath(requirePrefix, indexFile)) ) {
				LinkedAsset indexAsset = new IndexPageAsset(indexFile, assetContainer, requirePrefix, implicitDependencies);
				assetDiscoveryInitiator.registerSeedAsset(indexAsset);
			}
		}
	}

}
