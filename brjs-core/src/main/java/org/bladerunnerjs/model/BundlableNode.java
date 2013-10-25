package org.bladerunnerjs.model;

import java.util.List;

import org.bladerunnerjs.model.engine.Node;
import org.bladerunnerjs.model.exception.AmbiguousRequirePathException;
import org.bladerunnerjs.model.exception.ModelOperationException;
import org.bladerunnerjs.model.file.AliasesFile;


public interface BundlableNode extends Node, SourceLocation {
	AliasesFile aliases();
	SourceFile getSourceFile(String requirePath) throws AmbiguousRequirePathException;
	List<LinkedAssetFile> getSeedFiles();
	List<SourceLocation> getSourceLocations();
	BundleSet getBundleSet() throws ModelOperationException;
}