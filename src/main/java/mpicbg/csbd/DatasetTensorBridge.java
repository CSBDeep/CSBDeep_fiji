package mpicbg.csbd;

import java.util.Arrays;
import java.util.List;

import net.imagej.Dataset;
import net.imagej.axis.Axes;

import org.tensorflow.Shape;

public class DatasetTensorBridge {
	
	private Dataset dataset = null;
    private Shape tensorShape = null;

	private List<String> datasetDimNames = null;
    private List<Integer> datasetDimIndices = null;
    private List<Long> datasetDimLengths = null;
    private int[] dimMapping = {0,1,2,3,4};
    private boolean mappingInitialized = false;  
    
    public DatasetTensorBridge(Dataset image){
    	dataset = image;
    	datasetDimNames = Arrays.asList("X","Y","Z","Ch","T");
		datasetDimIndices = Arrays.asList(image.dimensionIndex(Axes.X),
				image.dimensionIndex(Axes.Y), 
				image.dimensionIndex(Axes.Z), 
				image.dimensionIndex(Axes.CHANNEL), 
				image.dimensionIndex(Axes.TIME));
		datasetDimLengths = Arrays.asList(image.getWidth(), 
				image.getHeight(), 
				image.getDepth(), 
				image.getChannels(), 
				image.getFrames());	
    }
    
    public int getDatasetDimLengthFromTFIndex(int fakeTFIndex){
		if(dimMapping[fakeTFIndex] < 0){
			return 1;
		}
		return datasetDimLengths.get(dimMapping[fakeTFIndex]).intValue();
	}
	
    public int getDatasetDimIndexByTFIndex(int fakeTFIndex){
		if(dimMapping[fakeTFIndex] < 0){
			return -1;
		}
		return datasetDimIndices.get(dimMapping[fakeTFIndex]);
	}

	public boolean isMappingInitialized() {
		return mappingInitialized;
	}

	public void setMappingInitialized(boolean mappingInitialized) {
		this.mappingInitialized = mappingInitialized;
	}

	public void setMappingDefaults() {
		System.out.println("setmappingdefaults");
		setMappingInitialized(true);
		if(tensorShape.numDimensions() == 5){
			dimMapping[0] = 4;
			dimMapping[1] = 2;
			dimMapping[2] = 1;
			dimMapping[3] = 0;
			dimMapping[4] = 3;
		}else{
			if(tensorShape.numDimensions() == 4){
				dimMapping[0] = -1;
				dimMapping[1] = 4;
				dimMapping[2] = 1;
				dimMapping[3] = 0;
				dimMapping[4] = 3;
			}else{
				return;
			}
		}
		
	}
	
    public void setTensorShape(Shape shape){
    	tensorShape = shape;
    }
	
    public Shape getTensorShape() {
		return tensorShape;
	}
    
    public int numDimensions(){
    	return datasetDimNames.size();
    }
    
    public long getDatasetDimLength(int index){
    	return datasetDimLengths.get(index);
    }
    
    public String getDatasetDimName(int index){
    	return datasetDimNames.get(index);
    }
    
    public int getMapping(int fakeTFIndex){
    	return dimMapping[fakeTFIndex];
    }
    
    public void setMapping(int fakeTFIndex, int mapping){
    	dimMapping[fakeTFIndex] = mapping;
    }
    
    public void setMappingRealTFIndex(int realTFIndex, int mapping){
    	dimMapping[realTFIndex+dimMapping.length-tensorShape.numDimensions()] = mapping;
    }
    
    public float[][][][][] createFakeTFArray(){
    	System.out.println("create fake tf array with dims: " + getDatasetDimLengthFromTFIndex(0) + " " + getDatasetDimLengthFromTFIndex(1) + " " + getDatasetDimLengthFromTFIndex(2) + " " + getDatasetDimLengthFromTFIndex(3) + " " + getDatasetDimLengthFromTFIndex(4));
    	return new float[getDatasetDimLengthFromTFIndex(0)]
    					[getDatasetDimLengthFromTFIndex(1)]
    					[getDatasetDimLengthFromTFIndex(2)]
						[getDatasetDimLengthFromTFIndex(3)]
						[getDatasetDimLengthFromTFIndex(4)];
    }

	public boolean complete() {
		return tensorShape != null && dataset != null;
	}
	
	public void printMapping(){
		System.out.print("mapping:");
		for(int i : dimMapping){
			System.out.print(" " + i);
		}
		System.out.println();
	}

}
