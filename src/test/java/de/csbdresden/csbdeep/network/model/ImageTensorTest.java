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
