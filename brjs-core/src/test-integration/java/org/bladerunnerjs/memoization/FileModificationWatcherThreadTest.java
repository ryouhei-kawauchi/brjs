package org.bladerunnerjs.memoization;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bladerunnerjs.model.BRJS;
import org.bladerunnerjs.utility.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FileModificationWatcherThreadTest
{
	private static final int MAX_UPDATE_CHECKS = 300; // 300 x 200ms (wait 1 minute before failing)
	private static final int THREAD_SLEEP_INTEVAL = 200;
	
	
	private BRJS mockBrjs;
	private FileModificationRegistry mockModificationRegistry;
	private MemoizedFile rootWatchDir;
	private FileModificationWatcherThread modificationWatcherThread;
	private List<File> fileChanges = Collections.synchronizedList( new ArrayList<>() );
	private File fileInRoot;
	private File dirInRoot;
	private File fileInChildDir;
	
	@Before
	public void setup() throws IOException, InterruptedException {
		
		mockModificationRegistry = mock(FileModificationRegistry.class);
		doAnswer(new Answer<Object>() {
	        public Object answer(InvocationOnMock invocation) {
	            Object[] args = invocation.getArguments();
	            fileChanges.add( (File) args[0] );
	            return null;
	        }
	    }).when(mockModificationRegistry).incrementFileVersion(any(File.class));
		
		mockBrjs = mock(BRJS.class);
		rootWatchDir = new MemoizedFile(mockBrjs, FileUtils.createTemporaryDirectory( this.getClass() ).getAbsolutePath() );
		when(mockBrjs.dir()).thenReturn(rootWatchDir);
		when(mockBrjs.getFileModificationRegistry()).thenReturn(mockModificationRegistry);
		
		modificationWatcherThread = new FileModificationWatcherThread(mockBrjs);
		modificationWatcherThread.start();
		
		Thread.sleep(1000); // give the thread time to initialise
		
		fileInRoot = new File(rootWatchDir, "some-file.txt");
		dirInRoot = new File(rootWatchDir, "some-dir");
		fileInChildDir = new File(dirInRoot, "nested-file.txt");
	}
	
	@After
	public void tearDown() {
		modificationWatcherThread.interrupt();
		FileUtils.deleteQuietly(rootWatchDir);
	}
	
	@Test @Ignore
	public void newFilesInTheRootDirAreDetected() throws Exception
	{
		fileInRoot.createNewFile();
		
		for (int i = 0; i < MAX_UPDATE_CHECKS; i++) {
			if (fileChanges.size() > 0 && fileChanges.get(0).equals(fileInRoot)) {
				fileChanges.remove(0);
				return;
			}
			Thread.sleep(THREAD_SLEEP_INTEVAL);
		}
		fail("Changes were not detected");		
	}
	
	@Test @Ignore
	public void changesToFilesInTheRootDirAreDetected() throws Exception
	{
		newFilesInTheRootDirAreDetected();
		org.apache.commons.io.FileUtils.write(fileInRoot, "some test data");
		
		for (int i = 0; i < MAX_UPDATE_CHECKS; i++) {
			if (fileChanges.size() > 0 && fileChanges.get(0).equals(fileInRoot)) {
				fileChanges.remove(0);
				return;
			}
			Thread.sleep(THREAD_SLEEP_INTEVAL);
		}
		fail("Changes were not detected");		
	}
	
	@Test @Ignore
	public void deletedFilesInTheRootDirAreDetected() throws Exception
	{
		newFilesInTheRootDirAreDetected();
		fileInRoot.delete();
		
		for (int i = 0; i < MAX_UPDATE_CHECKS; i++) {
			if (fileChanges.size() > 0 && fileChanges.get(0).equals(fileInRoot)) {
				fileChanges.remove(0);
				return;
			}
			Thread.sleep(THREAD_SLEEP_INTEVAL);
		}
		fail("Changes were not detected");		
	}
	
	@Test @Ignore
	public void newDirectoriesInTheRootDirAreDetected() throws Exception
	{
		dirInRoot.mkdir();
		
		for (int i = 0; i < MAX_UPDATE_CHECKS; i++) {
			if (fileChanges.size() > 0 && fileChanges.get(0).equals(dirInRoot)) {
				fileChanges.remove(0);
				return;
			}
			Thread.sleep(THREAD_SLEEP_INTEVAL);
		}
		fail("Changes were not detected");		
	}
	
	@Test @Ignore
	public void deletedDirectoriesInTheRootDirAreDetected() throws Exception
	{
		newDirectoriesInTheRootDirAreDetected();
		dirInRoot.delete();
		
		for (int i = 0; i < MAX_UPDATE_CHECKS; i++) {
			if (fileChanges.size() > 0 && fileChanges.get(0).equals(dirInRoot)) {
				fileChanges.remove(0);
				return;
			}
			Thread.sleep(THREAD_SLEEP_INTEVAL);
		}
		fail("Changes were not detected");		
	}
	
	@Test @Ignore
	public void newFilesInChildDirsAreDetected() throws Exception
	{
		newDirectoriesInTheRootDirAreDetected();
		fileInChildDir.createNewFile();
		
		for (int i = 0; i < MAX_UPDATE_CHECKS*4; i++) {
			if (fileChanges.size() > 0 && fileChanges.get(0).equals(dirInRoot)) {
				fileChanges.remove(0);
			}
			if (fileChanges.size() > 0 && fileChanges.get(0).equals(fileInChildDir)) {
				fileChanges.remove(0);
				return;
			}
			Thread.sleep(THREAD_SLEEP_INTEVAL);
		}
		fail("Changes were not detected");		
	}
	
}
