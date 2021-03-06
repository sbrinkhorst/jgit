/*
 * Copyright (C) 2011, Christian Halstrick <christian.halstrick@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.Sets;
import org.junit.Test;

public class StatusCommandTest extends RepositoryTestCase {

	@Test
	public void testEmptyStatus() throws NoWorkTreeException,
			GitAPIException {
		try (Git git = new Git(db)) {
			Status stat = git.status().call();
			assertEquals(0, stat.getAdded().size());
			assertEquals(0, stat.getChanged().size());
			assertEquals(0, stat.getMissing().size());
			assertEquals(0, stat.getModified().size());
			assertEquals(0, stat.getRemoved().size());
			assertEquals(0, stat.getUntracked().size());
		}
	}

	@Test
	public void testDifferentStates() throws IOException,
			NoFilepatternException, GitAPIException {
		try (Git git = new Git(db)) {
			writeTrashFile("a", "content of a");
			writeTrashFile("b", "content of b");
			writeTrashFile("c", "content of c");
			git.add().addFilepattern("a").addFilepattern("b").call();
			Status stat = git.status().call();
			assertEquals(Sets.of("a", "b"), stat.getAdded());
			assertEquals(0, stat.getChanged().size());
			assertEquals(0, stat.getMissing().size());
			assertEquals(0, stat.getModified().size());
			assertEquals(0, stat.getRemoved().size());
			assertEquals(Sets.of("c"), stat.getUntracked());
			git.commit().setMessage("initial").call();

			writeTrashFile("a", "modified content of a");
			writeTrashFile("b", "modified content of b");
			writeTrashFile("d", "content of d");
			git.add().addFilepattern("a").addFilepattern("d").call();
			writeTrashFile("a", "again modified content of a");
			stat = git.status().call();
			assertEquals(Sets.of("d"), stat.getAdded());
			assertEquals(Sets.of("a"), stat.getChanged());
			assertEquals(0, stat.getMissing().size());
			assertEquals(Sets.of("b", "a"), stat.getModified());
			assertEquals(0, stat.getRemoved().size());
			assertEquals(Sets.of("c"), stat.getUntracked());
			git.add().addFilepattern(".").call();
			git.commit().setMessage("second").call();

			stat = git.status().call();
			assertEquals(0, stat.getAdded().size());
			assertEquals(0, stat.getChanged().size());
			assertEquals(0, stat.getMissing().size());
			assertEquals(0, stat.getModified().size());
			assertEquals(0, stat.getRemoved().size());
			assertEquals(0, stat.getUntracked().size());

			deleteTrashFile("a");
			assertFalse(new File(git.getRepository().getWorkTree(), "a").exists());
			git.add().addFilepattern("a").setUpdate(true).call();
			writeTrashFile("a", "recreated content of a");
			stat = git.status().call();
			assertEquals(0, stat.getAdded().size());
			assertEquals(0, stat.getChanged().size());
			assertEquals(0, stat.getMissing().size());
			assertEquals(0, stat.getModified().size());
			assertEquals(Sets.of("a"), stat.getRemoved());
			assertEquals(Sets.of("a"), stat.getUntracked());
			git.commit().setMessage("t").call();

			writeTrashFile("sub/a", "sub-file");
			stat = git.status().call();
			assertEquals(1, stat.getUntrackedFolders().size());
			assertTrue(stat.getUntrackedFolders().contains("sub"));
		}
	}

	@Test
	public void testDifferentStatesWithPaths() throws IOException,
			NoFilepatternException, GitAPIException {
		try (Git git = new Git(db)) {
			writeTrashFile("a", "content of a");
			writeTrashFile("D/b", "content of b");
			writeTrashFile("D/c", "content of c");
			writeTrashFile("D/D/d", "content of d");
			git.add().addFilepattern(".").call();

			writeTrashFile("a", "new content of a");
			writeTrashFile("D/b", "new content of b");
			writeTrashFile("D/D/d", "new content of d");


			// filter on an not existing path
			Status stat = git.status().addPath("x").call();
			assertEquals(0, stat.getModified().size());

			// filter on an existing file
			stat = git.status().addPath("a").call();
			assertEquals(Sets.of("a"), stat.getModified());

			// filter on an existing folder
			stat = git.status().addPath("D").call();
			assertEquals(Sets.of("D/b", "D/D/d"), stat.getModified());

			// filter on an existing folder and file
			stat = git.status().addPath("D/D").addPath("a").call();
			assertEquals(Sets.of("a", "D/D/d"), stat.getModified());

			// do not filter at all
			stat = git.status().call();
			assertEquals(Sets.of("a", "D/b", "D/D/d"), stat.getModified());
		}
	}
}
