/*-
 * #%L
 * CSBDeep: CNNs for image restoration of fluorescence microscopy.
 * %%
 * Copyright (C) 2017 - 2020 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
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
package de.csbdresden.csbdeep.network.model;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import de.csbdresden.csbdeep.tiling.Tiling;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;

public class ImageTensorTest {

	@Test
	public void getFinalAxesArray() {
		ImageTensor node = new ImageTensor();
		node.initialize(new long[]{10,20,30}, new AxisType[]{
			Axes.X, Axes.Y, Axes.TIME});
		node.setNode(new long[] {-1,-1,-1,1}, new AxisType[]{Axes.TIME, Axes.Y, Axes.X, Axes.CHANNEL});
		node.setMappingDefaults();
		node.generateMapping();
		AxisType[] finalAxes = node.getFinalAxesArray();
		assertEquals(4, finalAxes.length);
		assertEquals(Axes.X, finalAxes[0]);
		assertEquals(Axes.Y, finalAxes[1]);
		assertEquals(Axes.TIME, finalAxes[2]);
		assertEquals(Axes.CHANNEL, finalAxes[3]);
	}

	@Test
	public void getAxesArray() {
		ImageTensor node = new ImageTensor();
		node.initialize(new long[]{10,20,30}, new AxisType[]{
				Axes.X, Axes.Y, Axes.Z});
		node.setNode(new long[] {-1,-1,-1,1}, new AxisType[]{Axes.TIME, Axes.Y, Axes.X, Axes.CHANNEL});
		node.setMappingDefaults();
		node.generateMapping();
		node.printMapping();
		assertEquals(4, node.getMapping().length);
//		assertNotEquals(Axes.TIME, node.getMapping()[0]); // this should be unknown
		assertEquals(Axes.Y, node.getMapping()[1]);
		assertEquals(Axes.X, node.getMapping()[2]);
		assertEquals(Axes.CHANNEL, node.getMapping()[3]);
		AxisType[] finalAxes = node.getAxesArray();
		System.out.println(Arrays.toString(finalAxes));
		assertEquals(4, finalAxes.length);
		assertEquals(Axes.X, finalAxes[0]);
		assertEquals(Axes.Y, finalAxes[1]);
		assertNotEquals(Axes.TIME, finalAxes[2]); // should be unknown
		assertEquals(Axes.CHANNEL, finalAxes[3]);
	}

	@Test
	public void getFinalAxesArray2() {
		ImageTensor node = new ImageTensor();
		node.initialize(new long[]{10,20,30, 1}, new AxisType[]{
				Axes.X, Axes.Y, Axes.Z, Axes.TIME});
		node.setNode(new long[] {-1,-1,-1,1}, new AxisType[]{Axes.TIME, Axes.Y, Axes.X, Axes.CHANNEL});
		node.setMappingDefaults();
		node.generateMapping();
		node.printMapping();
		assertEquals(4, node.getMapping().length);
		assertNotEquals(Axes.TIME, node.getMapping()[0]);
		assertEquals(Axes.Y, node.getMapping()[1]);
		assertEquals(Axes.X, node.getMapping()[2]);
		assertEquals(Axes.CHANNEL, node.getMapping()[3]);
		AxisType[] finalAxes = node.getFinalAxesArray();
		System.out.println(Arrays.toString(finalAxes));
		assertEquals(4, finalAxes.length);
		assertEquals(Axes.X, finalAxes[0]);
		assertEquals(Axes.Y, finalAxes[1]);
		assertEquals(Axes.Z, finalAxes[2]);
		assertEquals(Axes.CHANNEL, finalAxes[3]);
		Tiling.TilingAction[] tilingActions = node.getTilingActions();
		System.out.println(Arrays.toString(tilingActions));
		assertEquals(Tiling.TilingAction.TILE_WITH_PADDING, tilingActions[0]);
		assertEquals(Tiling.TilingAction.TILE_WITH_PADDING, tilingActions[1]);
		assertEquals(Tiling.TilingAction.TILE_WITHOUT_PADDING, tilingActions[2]);
		assertEquals(Tiling.TilingAction.NO_TILING, tilingActions[3]);
	}

	@Test
	public void getFinalAxesArray3() {
		ImageTensor node = new ImageTensor();
		node.initialize(new long[]{10,20}, new AxisType[]{
				Axes.X, Axes.Y});
		node.setNode(new long[] {-1,-1,-1,1}, new AxisType[]{Axes.TIME, Axes.Y, Axes.X, Axes.CHANNEL});
		node.setMappingDefaults();
		node.generateMapping();
		node.printMapping();
		System.out.println(Arrays.toString(node.getMapping()));
		assertEquals(4, node.getMapping().length);
		assertEquals(Axes.TIME, node.getMapping()[0]);
		assertEquals(Axes.Y, node.getMapping()[1]);
		assertEquals(Axes.X, node.getMapping()[2]);
		assertEquals(Axes.CHANNEL, node.getMapping()[3]);
		AxisType[] finalAxes = node.getFinalAxesArray();
		System.out.println(Arrays.toString(finalAxes));
		assertEquals(4, finalAxes.length);
		assertEquals(Axes.X, finalAxes[0]);
		assertEquals(Axes.Y, finalAxes[1]);
		assertEquals(Axes.TIME, finalAxes[2]);
		assertEquals(Axes.CHANNEL, finalAxes[3]);
		Tiling.TilingAction[] tilingActions = node.getTilingActions();
		System.out.println(Arrays.toString(tilingActions));
		assertEquals(Tiling.TilingAction.TILE_WITH_PADDING, tilingActions[0]);
		assertEquals(Tiling.TilingAction.TILE_WITH_PADDING, tilingActions[1]);
		assertEquals(Tiling.TilingAction.TILE_WITHOUT_PADDING, tilingActions[2]);
		assertEquals(Tiling.TilingAction.NO_TILING, tilingActions[3]);
	}

	@Test
	public void getTilingActions() {
		ImageTensor node = new ImageTensor();
		node.initialize(new long[]{5, 10, 2, 5}, new AxisType[]{
				Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z});
		node.setNode(new long[] {-1,-1,-1,1}, new AxisType[]{Axes.TIME, Axes.Y, Axes.X, Axes.CHANNEL});
		node.setMappingDefaults();
		node.generateMapping();
		node.printMapping();
		System.out.println(Arrays.toString(node.getMapping()));
		assertEquals(4, node.getMapping().length);
		assertEquals(Axes.Z, node.getMapping()[0]);
		assertEquals(Axes.Y, node.getMapping()[1]);
		assertEquals(Axes.X, node.getMapping()[2]);
		assertEquals(Axes.CHANNEL, node.getMapping()[3]);
		AxisType[] finalAxes = node.getFinalAxesArray();
		System.out.println(Arrays.toString(finalAxes));
		assertEquals(4, finalAxes.length);
		assertEquals(Axes.X, finalAxes[0]);
		assertEquals(Axes.Y, finalAxes[1]);
		assertEquals(Axes.CHANNEL, finalAxes[2]);
		assertEquals(Axes.Z, finalAxes[3]);
		Tiling.TilingAction[] tilingActions = node.getTilingActions();
		System.out.println(Arrays.toString(tilingActions));
		assertEquals(Tiling.TilingAction.TILE_WITH_PADDING, tilingActions[0]);
		assertEquals(Tiling.TilingAction.TILE_WITH_PADDING, tilingActions[1]);
		assertEquals(Tiling.TilingAction.NO_TILING, tilingActions[2]);
		assertEquals(Tiling.TilingAction.TILE_WITHOUT_PADDING, tilingActions[3]);

	}

}
