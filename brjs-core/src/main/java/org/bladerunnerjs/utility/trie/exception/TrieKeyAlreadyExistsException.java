package org.bladerunnerjs.utility.trie.exception;


public class TrieKeyAlreadyExistsException extends Exception
{
	private static final long serialVersionUID = 1L;

	public TrieKeyAlreadyExistsException(String key)
	{
		super("The key '"+key+"' already exists");
	}
}
