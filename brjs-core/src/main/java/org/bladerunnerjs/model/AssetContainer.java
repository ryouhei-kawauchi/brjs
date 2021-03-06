package org.bladerunnerjs.model;

import java.util.List;
import java.util.Set;

import org.bladerunnerjs.api.App;
import org.bladerunnerjs.api.Asset;
import org.bladerunnerjs.api.BRJSNode;
import org.bladerunnerjs.api.model.exception.RequirePathException;

/**
 * Represents a location that can contain assets (src or resources) such as an Aspect, Blade or Workbench.
 *
 */
public interface AssetContainer extends BRJSNode {
	App app();
	String requirePrefix();
	boolean isNamespaceEnforced();
	Set<Asset> assets();
	Asset asset(String requirePath);
	
	String canonicaliseRequirePath(Asset asset, String requirePath) throws RequirePathException;
	
	/**
	 * Returns all AssetContainers whose assets can be referred to by assets in this AssetContainer
	 */
	List<AssetContainer> scopeAssetContainers();
}
