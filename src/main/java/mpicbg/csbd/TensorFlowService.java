/*-
 * 
 * 
 * This code is adapted from imagej-tensorflow and will be deleted as soon as 
 * we are able to use image-tensorflow
 * 
 * #%L
 * ImageJ/TensorFlow integration.
 * %%
 * Copyright (C) 2017 Board of Regents of the University of
 * Wisconsin-Madison and Google, Inc.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package mpicbg.csbd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import net.imagej.ImageJService;

import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.tensorflow.Graph;

/**
 *
 * @author Curtis Rueden
 * @author Deborah Schmidt
 */
@Plugin(type = Service.class)
public class TensorFlowService extends AbstractService implements ImageJService

{

	/** Graphs which are already cached in memory. */
	private final Map<String, Graph> graphs = new HashMap<>();

	// -- TensorFlowService methods --

	public Graph loadGraph(final File graph) throws IOException
	{
		final String key = graph.getName();

		// If the graph is already cached in memory, return it.
		if (graphs.containsKey(key)) return graphs.get(key);

		// Read the serialized graph.
		final byte[] graphDef = Files.readAllBytes(Paths.get(graph.getAbsolutePath()));

		// Convert to a TensorFlow Graph object.
		final Graph _graph = new Graph();
		_graph.importGraphDef(graphDef);

		// Cache the result for performance next time.
		graphs.put(key, _graph);

		return _graph;
	}

	// -- Disposable methods --

	@Override
	public void dispose() {

		// Dispose graphs.
		for (final Graph graph : graphs.values()) {
			graph.close();
		}
		graphs.clear();

	}

}
