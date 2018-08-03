
package mpicbg.csbd.util;

import java.util.ArrayList;
import java.util.List;

public class ArrayHelper {

	public static void replaceNegativeIndicesWithUnusedIndices(
		final List<Integer> arr)
	{
		final List<Integer> indices = new ArrayList<>();
		for (int i = 0; i < arr.size(); i++) {
			indices.add(arr.get(i));
		}
		for (int i = 0; i < arr.size(); i++) {
			if (!indices.contains(i)) {
				for (int j = 0; j < arr.size(); j++) {
					if (arr.get(j) == -1) {
						arr.set(j, i);
						break;
					}
				}
			}
		}
	}

	public static int[] toIntArray(final List<Integer> list) {
		final int[] ret = new int[list.size()];
		for (int i = 0; i < ret.length; i++)
			ret[i] = list.get(i);
		return ret;
	}

}
